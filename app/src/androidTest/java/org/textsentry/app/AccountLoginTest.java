package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.ResponseTypeValues;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccountLoginTest {
    private static final String CONFIG =
            "{\"client_id\":\"testpublicclient123\","
            + "\"authorization_server\":\"https://textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com\","
            + "\"redirect_uri\":\"textsentry://auth/callback\"}";

    @Test public void googleLoginUsesAuthorizationCodeAndPkceInsteadOfImplicitFlow() {
        AuthorizationRequest request = CognitoLogin.createRequest(CognitoConfig.parse(CONFIG));
        assertEquals(ResponseTypeValues.CODE, request.responseType);
        assertEquals("textsentry://auth/callback", request.redirectUri.toString());
        assertEquals("S256", request.codeVerifierChallengeMethod);
        assertNotNull(request.codeVerifier);
        assertNotNull(request.state);
        assertEquals("Google", request.additionalParameters.get("identity_provider"));
        assertTrue(request.scope.contains("openid"));
        assertTrue(request.scope.contains("email"));
    }

    @Test public void redirectReceiverIsExportedForExactCognitoCallback() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ActivityInfo receiver = context.getPackageManager().getActivityInfo(
                new ComponentName(context, net.openid.appauth.RedirectUriReceiverActivity.class),
                0);
        assertTrue(receiver.exported);
        Intent redirect = new Intent(Intent.ACTION_VIEW,
                Uri.parse("textsentry://auth/callback?code=synthetic&state=synthetic"));
        redirect.addCategory(Intent.CATEGORY_BROWSABLE);
        redirect.addCategory(Intent.CATEGORY_DEFAULT);
        redirect.setPackage(context.getPackageName());
        assertNotNull(context.getPackageManager().resolveActivity(redirect, 0));
    }

    @Test public void billingReturnIsSideloadOnlyAndNeverAProofOfSubscription() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("textsentry://billing/return?app_user_id=untrusted"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        assertTrue(AccountActivity.isBillingReturn(intent));
        assertFalse(AccountActivity.isBillingReturn(new Intent(Intent.ACTION_VIEW,
                Uri.parse("textsentry://auth/callback?code=untrusted"))));
        assertFalse(AccountActivity.isBillingReturn(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://example.com/billing/return"))));
        if ("sideload".equals(BuildConfig.FLAVOR)) {
            ActivityInfo activity = context.getPackageManager().getActivityInfo(
                    new ComponentName(context, AccountActivity.class), 0);
            assertTrue(activity.exported);
            intent.setPackage(context.getPackageName());
            assertNotNull(context.getPackageManager().resolveActivity(intent, 0));
        }
    }

    @Test public void managedKeyIsProcessMemoryOnlyAndCanBeCleared() {
        ManagedKeyMemory.clear();
        assertFalse(ManagedKeyMemory.hasActiveKey());
        ManagedKeyMemory.install("synthetic-ram-key", System.currentTimeMillis() + 60000);
        assertTrue(ManagedKeyMemory.hasActiveKey());
        assertEquals("synthetic-ram-key", ManagedKeyMemory.currentKey());
        ManagedKeyMemory.clear();
        assertNull(ManagedKeyMemory.currentKey());
    }

    @Test public void signOutInvalidatesInflightManagedKeyFetch() {
        ManagedKeyMemory.clear();
        long requestGeneration = ManagedKeyMemory.generation();
        ManagedKeyMemory.clear(); // Signout while remote request was running.
        assertFalse(ManagedKeyMemory.installIfUnchanged(
                "synthetic-key", System.currentTimeMillis() + 60000, requestGeneration));
        assertNull(ManagedKeyMemory.currentKey());
    }

    @Test public void accountStoreRejectsIncompleteTokensAndCanClear() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthSessionStore store = new AuthSessionStore(context);
        store.clear();
        assertNull(store.load());
        try {
            store.save(new AuthState());
            fail("Incomplete OAuth session must never be persisted");
        } catch (IllegalArgumentException expected) {}
        assertFalse(store.hasSession());
    }
}
