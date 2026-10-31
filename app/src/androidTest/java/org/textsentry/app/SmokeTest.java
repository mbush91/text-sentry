package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SmokeTest {
    @Test public void appStartsWithMonitoringDisabled() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Settings settings = new Settings(context);
        settings.save(Settings.DEFAULT_ENDPOINT, Settings.DEFAULT_MODEL,
                Settings.DEFAULT_PROMPT, false, false);
        assertFalse(settings.monitoring());
        assertFalse(settings.autoSend());
    }

    @Test public void encryptedKeyRoundTripsAndCanBeRemoved() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        KeyVault vault = new KeyVault(context);
        vault.save("example-not-a-real-key");
        assertEquals("example-not-a-real-key", vault.read());
        vault.clear();
        assertFalse(vault.hasKey());
    }
}
