package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OnboardingTest {
    @Test public void welcomeIsOnlyShownBeforeAccountOrOwnKeySetup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        new KeyVault(context).clear();
        new AuthSessionStore(context).clear();
        context.getSharedPreferences(Onboarding.PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        assertTrue(Onboarding.shouldShow(context));
        Onboarding.complete(context);
        assertFalse(Onboarding.shouldShow(context));
    }

    @Test public void byokKeySkipsAccountFirstWelcome() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        new AuthSessionStore(context).clear();
        context.getSharedPreferences(Onboarding.PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        KeyVault vault = new KeyVault(context);
        vault.save("synthetic-byok-key");
        assertFalse(Onboarding.shouldShow(context));
        vault.clear();
        Onboarding.complete(context);
    }
}
