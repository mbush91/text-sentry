package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Installed launchers must be able to load the adaptive TextSentry mark in both flavors. */
@RunWith(AndroidJUnit4.class)
public class LauncherIconTest {
    @Test public void brandedAdaptiveIconIsInstalledAndDrawable() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo info = context.getApplicationInfo();
        assertEquals(R.mipmap.ic_launcher, info.icon);
        Drawable icon = context.getPackageManager().getApplicationIcon(info);
        assertNotNull(icon);
        assertTrue(icon.getIntrinsicWidth() > 0);
        assertTrue(icon.getIntrinsicHeight() > 0);
        assertNotNull(context.getDrawable(R.drawable.ic_textsentry_foreground));
        assertNotNull(context.getDrawable(R.drawable.ic_textsentry_monochrome));
        assertNotNull(context.getDrawable(R.drawable.ic_textsentry_background));
    }
}
