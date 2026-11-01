package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsSafetyTest {
    @Test public void settingsAreLockedToJevLatestAndThresholdPersists() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteSharedPreferences("textsentry_settings");
        Settings settings = new Settings(context);
        assertEquals("jev-latest", settings.model());
        assertTrue(settings.systemOne());
        assertEquals(95, settings.confidence());
        settings.savePolicy("Only clear promotional opt-outs", 82, "STOP");
        Settings again = new Settings(context);
        assertEquals(82, again.confidence());
        assertEquals("jev-latest", again.model());
        assertEquals("Only clear promotional opt-outs", again.prompt());
        settings.addHistory("Preview only");
        assertTrue(settings.history().contains("Preview only"));
        settings.clearHistory();
        assertFalse(settings.history().contains("Preview only"));
        for (int bad : new int[]{49, 101}) {
            try {
                settings.savePolicy("Rules", bad, "STOP");
                fail("Threshold must be validated");
            } catch (IllegalArgumentException expected) {}
        }
    }

    @Test public void legacyChatProviderKeyIsClearedBeforeTypeSafeUse() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("textsentry_settings", Context.MODE_PRIVATE).edit()
                .putBoolean("system_one", false)
                .putBoolean("monitor", true).commit();
        KeyVault key = new KeyVault(context);
        key.save("legacy-chat-token");
        Settings updated = new Settings(context);
        assertFalse(key.hasKey());
        assertFalse(updated.monitoring());
        assertEquals("jev-latest", updated.model());
    }

    @Test public void savingAndPausingInvalidateAlreadyQueuedMessages() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteSharedPreferences("textsentry_settings");
        Settings settings = new Settings(context);
        String old = settings.generation();
        settings.save(Settings.DEFAULT_ENDPOINT, Settings.DEFAULT_MODEL,
                Settings.DEFAULT_PROMPT, true, false);
        String running = settings.generation();
        assertNotEquals(old, running);
        settings.pause();
        assertFalse(settings.monitoring());
        assertFalse(settings.autoSend());
        assertNotEquals(running, settings.generation());
    }
}
