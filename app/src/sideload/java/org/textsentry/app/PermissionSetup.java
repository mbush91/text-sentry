package org.textsentry.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

/** User-controlled links and honest diagnostics for sideload-only SMS permissions. */
public final class PermissionSetup {
    private PermissionSetup() {}

    static Intent appDetailsIntent(Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
    }

    static Intent batterySettingsIntent() {
        return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    }

    static boolean batteryOptimizationsExempt(Context context) {
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    static String smsHelpText() {
        return "1. Open TextSentry app info, then Permissions. If SMS is available, tap it and choose Allow. " +
                "Return to TextSentry to check the updated status.\n\n" +
                "2. If Android won't grant SMS or the option is missing, RECEIVE_SMS and SEND_SMS are " +
                "hard-restricted by Android. Your installer must allowlist them. The app itself cannot " +
                "override this, and the Android 13+ 'Allow restricted settings' switch does not " +
                "allowlist SMS permissions. You may need an installation method that supports " +
                "these permissions.\n\n" +
                "3. For delayed background processing, open app info > Battery (or App battery usage) " +
                "> Unrestricted, if your phone provides that choice. This is optional and won't grant SMS. " +
                "Android battery settings vary by phone.";
    }

    static void openAppDetails(Activity activity) {
        try {
            activity.startActivity(appDetailsIntent(activity));
        } catch (ActivityNotFoundException | SecurityException notAvailable) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException | SecurityException ignored) {
                Toast.makeText(activity, "Open Android Settings > Apps > TextSentry.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    static void openBatterySettings(Activity activity) {
        try {
            activity.startActivity(batterySettingsIntent());
        } catch (ActivityNotFoundException | SecurityException notAvailable) {
            openAppDetails(activity);
        }
    }
}
