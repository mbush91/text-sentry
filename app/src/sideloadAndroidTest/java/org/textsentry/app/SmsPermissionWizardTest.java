package org.textsentry.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Regression: exercise actual permission wizard rendering for Google-session and BYOK paths. */
@RunWith(AndroidJUnit4.class)
public class SmsPermissionWizardTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void googleAccountPermissionsScreenRendersWithoutCrash() throws Exception {
        // Setup only checks whether a session is present. This is not a real login
        // token and is removed immediately; no network or actual SMS is involved.
        context.getSharedPreferences("textsentry_auth_session", Context.MODE_PRIVATE)
                .edit().putString("state", "screen-only-test-session").commit();
        new KeyVault(context).clear();
        SetupFlow.reset(context);
        SetupFlow.completeSubscriptionPrompt(context);
        assertEquals(SetupFlow.PERMISSIONS, SetupFlow.stage(context));
        Activity activity = null;
        try {
            activity = InstrumentationRegistry.getInstrumentation().startActivitySync(
                    new Intent(context, SetupActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            View root = activity.getWindow().getDecorView();
            assertTrue(contains(root, "Allow message access."));
            assertTrue(contains(root, "Android permission settings"));
            assertTrue(contains(root, "Continue to battery settings"));
        } finally {
            if (activity != null) activity.runOnUiThread(activity::finish);
            context.getSharedPreferences("textsentry_auth_session", Context.MODE_PRIVATE)
                    .edit().remove("state").commit();
            SetupFlow.reset(context);
        }
    }

    @Test public void byokPermissionsScreenRendersWithoutCrash() throws Exception {
        SetupFlow.reset(context);
        new KeyVault(context).save("synthetic-screen-test");
        Activity activity = null;
        try {
            activity = InstrumentationRegistry.getInstrumentation().startActivitySync(
                    new Intent(context, SetupActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            View root = activity.getWindow().getDecorView();
            assertTrue(contains(root, "Allow message access."));
            assertTrue(contains(root, "Allow incoming SMS"));
        } finally {
            if (activity != null) activity.runOnUiThread(activity::finish);
            new KeyVault(context).clear();
            SetupFlow.reset(context);
        }
    }

    private static boolean contains(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText()))
            return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (contains(group.getChildAt(i), value)) return true;
            }
        }
        return false;
    }
}
