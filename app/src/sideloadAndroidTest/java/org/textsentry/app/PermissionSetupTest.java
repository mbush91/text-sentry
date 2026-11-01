package org.textsentry.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verify the sideload permission help opens real Android settings, not fake grants. */
@RunWith(AndroidJUnit4.class)
public class PermissionSetupTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void appDetailsLinkTargetsThisInstalledPackage() {
        Intent intent = PermissionSetup.appDetailsIntent(context);
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.getAction());
        assertEquals(Uri.parse("package:" + context.getPackageName()), intent.getData());
    }

    @Test public void batterySettingsUsesPublicSettingsAction() {
        Intent intent = PermissionSetup.batterySettingsIntent();
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.getAction());
    }

    @Test public void explainsRestrictedSmsGrantsSeparatelyFromBatteryOptimization() {
        String help = PermissionSetup.smsHelpText();
        assertTrue(help.contains("installer"));
        assertTrue(help.contains("RECEIVE_SMS"));
        assertTrue(help.contains("SEND_SMS"));
        assertTrue(help.contains("won't grant SMS"));
        PermissionSetup.batteryOptimizationsExempt(context); // Must be safe to query on this device.
    }

    @Test public void sideloadHomeIncludesRecoveryAndBatterySetup() {
        Intent launch = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(launch);
        try {
            View root = activity.getWindow().getDecorView();
            assertTrue(containsText(root, "SMS permission troubleshooting"));
            assertTrue(containsText(root, "Open TextSentry app permissions"));
            assertTrue(containsText(root, "Open battery optimization settings"));
        } finally {
            activity.runOnUiThread(activity::finish);
        }
    }

    private boolean containsText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), expected)) return true;
            }
        }
        return false;
    }
}
