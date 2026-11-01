package org.textsentry.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Guard against white-on-white system bars and drift from the website's brand colors. */
@RunWith(AndroidJUnit4.class)
public class BrandThemeTest {
    @Test public void paletteMatchesWebsite() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals(Color.rgb(5, 14, 32), UiKit.NIGHT);    // --night
        assertEquals(Color.rgb(11, 23, 48), UiKit.INK);     // --ink
        assertEquals(Color.rgb(93, 111, 136), UiKit.MUTED); // --muted
        assertEquals(Color.rgb(38, 215, 238), UiKit.ACCENT);// --brand-cyan
        assertEquals(Color.rgb(243, 247, 255), UiKit.BG);   // --paper
        assertEquals(context.getColor(R.color.textsentry_night), UiKit.NIGHT);
        assertEquals(context.getColor(R.color.textsentry_paper), UiKit.BG);
        assertEquals(context.getColor(R.color.textsentry_cyan), UiKit.ACCENT);
    }

    @Test public void launchWindowAndScrolledScreensUseReadableSystemBars() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            TypedArray bars = activity.obtainStyledAttributes(new int[]{
                    android.R.attr.statusBarColor, android.R.attr.navigationBarColor,
                    android.R.attr.windowLightStatusBar, android.R.attr.windowLightNavigationBar
            });
            try {
                assertEquals(UiKit.NIGHT, bars.getColor(0, Color.TRANSPARENT));
                assertEquals(UiKit.BG, bars.getColor(1, Color.TRANSPARENT));
                assertFalse(bars.getBoolean(2, true)); // white icons on navy
                assertTrue(bars.getBoolean(3, false)); // dark icons on pale navigation
            } finally {
                bars.recycle();
            }

            FrameLayout content = activity.findViewById(android.R.id.content);
            FrameLayout screen = (FrameLayout) content.getChildAt(0);
            View topScrim = screen.getChildAt(1); // permanent overlay above scrolling content
            assertEquals(UiKit.NIGHT,
                    ((ColorDrawable) topScrim.getBackground()).getColor());

            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsets insets = screen.getRootWindowInsets();
                assertNotNull(insets);
                assertEquals(insets.getInsets(WindowInsets.Type.statusBars()).top,
                        topScrim.getLayoutParams().height);
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                assertNotNull(controller);
                assertEquals(0, controller.getSystemBarsAppearance()
                        & WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                assertTrue((controller.getSystemBarsAppearance()
                        & WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0);
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
        }
    }
}
