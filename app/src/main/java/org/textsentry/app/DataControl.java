package org.textsentry.app;

import android.content.Context;
import androidx.work.WorkManager;

/** Erase all local app-owned preferences, queued SMS work and device BYOK secret. */
public final class DataControl {
    private DataControl() {}

    public static void deleteLocalData(Context context) {
        // Block queued sends before attempting async WorkManager cancellation.
        new Settings(context).pause();
        WorkManager.getInstance(context).cancelAllWork();
        new KeyVault(context).clear();
        for (String name : new String[] {"textsentry_settings", "textsentry_reply_ledger",
                "textsentry_secrets"}) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit();
        }
    }
}
