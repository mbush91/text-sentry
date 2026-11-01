package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Required sign-in/BYOK setup, independent of whether a welcome was shown before. */
@RunWith(AndroidJUnit4.class)
public class RequiredSetupTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test public void cannotAccessAppWithoutCredentialsEvenAfterOldWelcomeFlag() throws Exception {
        Context ctx = context();
        new AuthSessionStore(ctx).clear();
        new KeyVault(ctx).clear();
        Onboarding.complete(ctx);
        assertFalse(SetupGate.hasAccess(ctx));
        assertTrue(SetupGate.needsSetup(ctx));
    }

    @Test public void aSavedOwnKeyUnlocksAppWithoutGoogle() throws Exception {
        Context ctx = context();
        new AuthSessionStore(ctx).clear();
        KeyVault vault = new KeyVault(ctx);
        vault.save("fake-own-key");
        assertTrue(SetupGate.hasAccess(ctx));
        assertFalse(SetupGate.needsSetup(ctx));
        vault.clear();
    }

    @Test public void launcherOpensRequiredSetupScreen() {
        Context ctx = context();
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        assertNotNull(intent);
        assertEquals(SetupActivity.class.getName(), intent.getComponent().getClassName());
    }
}
