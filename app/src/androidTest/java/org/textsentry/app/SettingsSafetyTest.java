package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsSafetyTest {
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
