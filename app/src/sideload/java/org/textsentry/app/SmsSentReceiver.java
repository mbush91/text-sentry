package org.textsentry.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Android's actual sent callback; a successful SmsManager call is NOT proof of sending. */
public final class SmsSentReceiver extends BroadcastReceiver {
    public static final String ACTION = "org.textsentry.app.SMS_SENT";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || intent.getData() == null) return;
        String id = intent.getData().getLastPathSegment();
        try {
            new HistoryStore(context).markSendResult(id, getResultCode() == Activity.RESULT_OK);
        } catch (Exception ignored) {
            // Never persist message content or errors in system logs.
        }
    }
}
