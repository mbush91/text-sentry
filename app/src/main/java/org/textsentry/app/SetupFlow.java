package org.textsentry.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Guided onboarding state. Never substitutes for live permission checks. */
final class SetupFlow {
    static final int LOGIN = 0;
    static final int BATTERY = 1;
    static final int PERMISSIONS = 2;
    static final int ACTIVATE = 3;
    private static final String PREFS = "textsentry_setup_flow";
    private static final String STEP = "step";
    private static final String DONE = "completed";

    private SetupFlow() {}

    static boolean isComplete(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(DONE, false);
    }

    static int validatedStage(int saved, boolean authenticated) {
        if (!authenticated) return LOGIN;
        return Math.max(BATTERY, Math.min(ACTIVATE, saved));
    }

    static int stage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return validatedStage(prefs.getInt(STEP, LOGIN), SetupGate.hasAccess(context));
    }

    static void advance(Context context, int step) {
        if (!SetupGate.hasAccess(context)) return;
        int target = validatedStage(step, true);
        int current = stage(context);
        if (target > current + 1) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(STEP, target).apply();
    }

    static void complete(Context context) {
        if (!SetupGate.hasAccess(context)) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(STEP, ACTIVATE).putBoolean(DONE, true).commit();
    }

    static void reset(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }
}
