package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Shared account and sign-up entry point for Play and sideload editions.
 * Opens Google's existing-account or first-time sign-up flow in the system browser.
 * Never collects Google passwords or SMS messages.
 */
public final class AccountActivity extends Activity {
    private static final int AUTH_REQUEST_CODE = 405;
    private AuthorizationService service;
    private AuthSessionStore sessions;
    private TextView status;
    private int authGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        service = new AuthorizationService(this);
        sessions = new AuthSessionStore(this);
        render();
        if (sessions.hasSession()) fetchManagedKey();
    }

    private void render() {
        LinearLayout root = UiKit.screen(this, "Your TextSentry account.", true);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, card, "Google account");
        UiKit.description(this, card,
                "Create an account or sign in with Google. Authentication uses " +
                "Cognito's secure browser sign-in; TextSentry never sees your Google password.");
        status = UiKit.description(this, card, "");
        refreshStatus();
        UiKit.button(this, card, "Continue with Google", true, this::startLogin);
        if (sessions.hasSession()) {
            UiKit.button(this, card, "Sign out on this device", false, () ->
                    new AlertDialog.Builder(this)
                            .setTitle("Sign out of TextSentry?")
                            .setMessage("This removes your saved account session on this device. " +
                                    "Your local message history and BYOK settings are unchanged.")
                            .setPositiveButton("Sign out", (dialog, which) -> {
                                try {
                                    authGeneration++;
                                    ManagedKeyMemory.clear();
                                    sessions.clear();
                                    render();
                                } catch (Exception e) {
                                    status.setText("Could not remove the account session.");
                                }
                            })
                            .setNegativeButton("Cancel", null).show());
        }

        LinearLayout info = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, info, "Private by design");
        UiKit.description(this, info,
                "Google sign-in creates your TextSentry account. No SMS content is sent to " +
                "TextSentry's AWS service. Current SMS classification still uses your " +
                "TypeSafe BYOK key; managed OpenRouter access is currently separate.");
        UiKit.description(this, info,
                "Account tokens are encrypted using Android Keystore and cleared on sign-out " +
                "or Delete all app data. Your managed OpenRouter key is fetched only into " +
                "RAM and disappears on process restart or sign-out.");
        UiKit.button(this, info, "Refresh managed key", false, this::fetchManagedKey);
    }

    private void refreshStatus() {
        if (!sessions.hasSession()) {
            status.setText("○ Not signed in");
            return;
        }
        try {
            AuthState state = sessions.load();
            status.setText(state != null && state.isAuthorized()
                    ? "✓ Signed in with Google on this device"
                    : "○ Session needs renewal. Sign in again.");
        } catch (Exception e) {
            status.setText("○ Could not restore saved session. Sign in again.");
        }
    }

    private void startLogin() {
        authGeneration++;
        ManagedKeyMemory.clear();
        status.setText("Connecting securely to TextSentry…");
        new Thread(() -> {
            try {
                CognitoConfig config = retrieveConfiguration();
                runOnUiThread(() -> openBrowser(config));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText(
                        "Sign-in configuration is unavailable. Check the connection and try again."));
            }
        }, "textsentry-auth-discovery").start();
    }

    private static CognitoConfig retrieveConfiguration() throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL(CognitoConfig.DISCOVERY_URL).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) {
                throw new IllegalStateException("Authentication configuration unavailable");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] chunk = new byte[1024];
                int count;
                while ((count = stream.read(chunk)) >= 0) {
                    if (buffer.size() + count > 8192) {
                        throw new IllegalArgumentException("Unexpected configuration size");
                    }
                    buffer.write(chunk, 0, count);
                }
            }
            return CognitoConfig.parse(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private void openBrowser(CognitoConfig config) {
        if (isFinishing() || isDestroyed()) return;
        try {
            AuthorizationRequest request = CognitoLogin.createRequest(config);
            startActivityForResult(service.getAuthorizationRequestIntent(request),
                    AUTH_REQUEST_CODE);
            status.setText("Finish signing in through Google…");
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            status.setText("No supported browser is available to sign in.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != AUTH_REQUEST_CODE) return;
        if (resultCode != RESULT_OK || data == null) {
            status.setText("Google sign-in was canceled.");
            return;
        }
        AuthorizationResponse authorization = AuthorizationResponse.fromIntent(data);
        AuthorizationException error = AuthorizationException.fromIntent(data);
        if (authorization == null || error != null) {
            status.setText("Google sign-in failed. Please try again.");
            return;
        }
        status.setText("Completing secure sign-in…");
        AuthState state = new AuthState();
        state.update(authorization, null);
        service.performTokenRequest(authorization.createTokenExchangeRequest(),
                (tokens, failure) -> {
                    if (tokens == null || failure != null) {
                        runOnUiThread(() -> status.setText(
                                "Cognito could not complete sign-in. Try again."));
                        return;
                    }
                    state.update(tokens, null);
                    try {
                        sessions.save(state);
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                authGeneration++;
                                ManagedKeyMemory.clear();
                                render();
                                fetchManagedKey();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> status.setText(
                                "The account could not be saved securely. Sign-in was not completed."));
                    }
                });
    }

    /** Refresh Cognito access tokens when necessary, then fetch into RAM only. */
    private void fetchManagedKey() {
        if (!sessions.hasSession()) {
            ManagedKeyMemory.clear();
            status.setText("Sign in with Google to get your managed key.");
            return;
        }
        final int generation = authGeneration;
        status.setText("Checking your managed key…");
        try {
            AuthState state = sessions.load();
            if (state == null || !state.isAuthorized()) {
                ManagedKeyMemory.clear();
                status.setText("Sign in again to refresh your account.");
                return;
            }
            state.performActionWithFreshTokens(service, (accessToken, idToken, failure) -> {
                if (generation != authGeneration || isFinishing() || isDestroyed()) return;
                if (failure != null || accessToken == null) {
                    ManagedKeyMemory.clear();
                    status.setText("Account session expired. Sign in again.");
                    return;
                }
                try {
                    // Persist only OAuth session tokens, never the OpenRouter key.
                    sessions.save(state);
                } catch (Exception e) {
                    ManagedKeyMemory.clear();
                    status.setText("Unable to secure refreshed account tokens.");
                    return;
                }
                new Thread(() -> {
                    ManagedKeyClient.KeyResponse key = null;
                    String error = null;
                    try {
                        key = ManagedKeyClient.fetch(accessToken);
                    } catch (Exception e) {
                        // ManagedKeyClient exceptions do not contain credentials.
                        error = e.getMessage();
                    }
                    final ManagedKeyClient.KeyResponse fetched = key;
                    final String problem = error;
                    runOnUiThread(() -> {
                        if (generation != authGeneration || isFinishing() || isDestroyed()
                                || !sessions.hasSession()) return;
                        if (fetched == null) {
                            ManagedKeyMemory.clear();
                            status.setText(problem == null
                                    ? "Could not retrieve managed key."
                                    : problem);
                        } else {
                            ManagedKeyMemory.install(fetched.key, fetched.expiresAtMillis);
                            status.setText(fetched.status());
                        }
                    });
                }, "textsentry-managed-key").start();
            });
        } catch (Exception e) {
            ManagedKeyMemory.clear();
            status.setText("Unable to read account session. Sign in again.");
        }
    }

    @Override protected void onDestroy() {
        if (service != null) service.dispose();
        super.onDestroy();
    }
}
