package org.textsentry.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Display the account-first welcome exactly once; BYOK remains opt-in. */
final class Onboarding {
    static final String PREFS = "textsentry_onboarding";
    static final String INTRO_SEEN = "intro_seen";
    private Onboarding() {}

    static boolean shouldShow(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return !prefs.getBoolean(INTRO_SEEN, false)
                && !new AuthSessionStore(context).hasSession()
                && !new KeyVault(context).hasKey();
    }

    static void complete(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(INTRO_SEEN, true).commit();
    }

    static void offer(Activity activity) {
        if (!shouldShow(activity)) return;
        complete(activity);
        Intent welcome = new Intent(activity, AccountActivity.class);
        welcome.putExtra("welcome", true);
        activity.startActivity(welcome);
    }
}
