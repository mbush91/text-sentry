package org.textsentry.app;

import android.content.Context;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Refresh Cognito tokens as necessary and fetch a managed key directly into RAM.
 * This method is blocking and must only be called from a background thread.
 */
final class ManagedKeyAccess {
    private ManagedKeyAccess() {}

    static synchronized String obtain(Context context) throws Exception {
        String cached = ManagedKeyMemory.currentKey();
        if (cached != null) return cached;

        AuthSessionStore sessions = new AuthSessionStore(context);
        AuthState state = sessions.load();
        if (state == null || !state.isAuthorized() || state.getRefreshToken() == null) {
            throw new IOException("Sign in with Google to analyze messages");
        }
        String originalRefresh = state.getRefreshToken();
        AuthorizationService service = new AuthorizationService(context);
        String token;
        try {
            CountDownLatch ready = new CountDownLatch(1);
            AtomicReference<String> accessToken = new AtomicReference<>();
            state.performActionWithFreshTokens(service, (access, id, error) -> {
                if (error == null) accessToken.set(access);
                ready.countDown();
            });
            if (!ready.await(20, TimeUnit.SECONDS) || accessToken.get() == null) {
                throw new IOException("Google session expired; sign in again");
            }
            token = accessToken.get();
            // Do not restore a signed-out user's credentials after a network race.
            AuthState current = sessions.load();
            if (current == null || !originalRefresh.equals(current.getRefreshToken())) {
                throw new IOException("Account changed; sign in again");
            }
            sessions.save(state);
        } finally {
            service.dispose();
        }

        ManagedKeyClient.KeyResponse fetched = ManagedKeyClient.fetch(token);
        AuthState current = sessions.load();
        if (current == null || !state.getRefreshToken().equals(current.getRefreshToken())) {
            throw new IOException("Account changed; sign in again");
        }
        ManagedKeyMemory.install(fetched.key, fetched.expiresAtMillis);
        return fetched.key;
    }
}
