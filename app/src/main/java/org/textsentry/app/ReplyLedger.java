package org.textsentry.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** Only hashed sender/message identifiers and timestamps are retained. */
public final class ReplyLedger {
    public static final long COOLDOWN_MS = 24L * 60 * 60 * 1000;
    private static final Object LOCK = new Object();
    private final SharedPreferences prefs;

    public ReplyLedger(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("textsentry_reply_ledger", Context.MODE_PRIVATE);
    }

    public boolean reserveMessage(String sender, String body, long nowMs) {
        if (sender == null || body == null || nowMs <= 0) return false;
        return reserve("message:" + hash(sender + "\u0000" + body), nowMs);
    }

    public boolean reserveSend(String sender, long nowMs) {
        if (sender == null || nowMs <= 0) return false;
        return reserve("send:" + hash(sender), nowMs);
    }

    private boolean reserve(String key, long nowMs) {
        synchronized (LOCK) {
            long previous = prefs.getLong(key, 0);
            if (previous > 0 && nowMs >= previous && nowMs - previous < COOLDOWN_MS) {
                return false;
            }
            SharedPreferences.Editor edit = prefs.edit();
            // Bound local storage by pruning entries older than two days.
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getValue() instanceof Long) {
                    long timestamp = (Long) entry.getValue();
                    if (timestamp <= 0 || nowMs - timestamp > 2 * COOLDOWN_MS) {
                        edit.remove(entry.getKey());
                    }
                }
            }
            return edit.putLong(key, nowMs).commit();
        }
    }

    private static String hash(String raw) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
