package org.textsentry.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/** Every protected screen requires either an encrypted Google session or a BYOK key. */
final class SetupGate {
    private SetupGate() {}

    static boolean hasAccess(Context context) {
        return new AuthSessionStore(context).hasSession() || new KeyVault(context).hasKey();
    }

    static boolean needsSetup(Context context) { return !hasAccess(context); }

    /** Return false after routing away, before any protected UI is displayed. */
    static boolean enforce(Activity activity) {
        if (hasAccess(activity)) return true;
        // Stops stale monitoring/replies if the last credential was removed.
        new Settings(activity).pause();
        Intent setup = new Intent(activity, SetupActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent incoming = activity.getIntent();
        if (Intent.ACTION_SEND.equals(incoming.getAction())
                && "text/plain".equals(incoming.getType())) {
            CharSequence shared = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                setup.putExtra(Intent.EXTRA_TEXT,
                        shared.subSequence(0, Math.min(1600, shared.length())).toString());
            }
        }
        activity.startActivity(setup);
        activity.finish();
        return false;
    }
}
