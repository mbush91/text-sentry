package org.textsentry.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Non-secret on-device preferences; raw message bodies are never persisted. */
public final class Settings {
    public static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    public static final String DEFAULT_MODEL = "gpt-4.1-mini";
    public static final String DEFAULT_PROMPT = "Only reply STOP to unmistakably opt-in marketing messages that explicitly describe a STOP unsubscribe option. Ignore personal messages, scams, transactional notices, and uncertainty.";
    private final SharedPreferences prefs;
    private static final Object RESERVATION_LOCK = new Object();

    public Settings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("textsentry_settings", Context.MODE_PRIVATE);
    }

    public String endpoint() { return prefs.getString("endpoint", DEFAULT_ENDPOINT); }
    public String model() { return prefs.getString("model", DEFAULT_MODEL); }
    public String prompt() { return prefs.getString("prompt", DEFAULT_PROMPT); }
    public boolean systemOne() { return prefs.getBoolean("system_one", false); }
    public String cannedReply() { return prefs.getString("canned_reply", "STOP"); }
    public boolean monitoring() { return prefs.getBoolean("monitor", false); }
    public boolean autoSend() { return prefs.getBoolean("auto_send", false); }
    public String history() { return prefs.getString("history", "No decisions yet."); }

    public void save(String endpoint, String model, String prompt, boolean monitoring, boolean autoSend) {
        save(endpoint, model, prompt, monitoring, autoSend, systemOne(), cannedReply());
    }

    public void save(String endpoint, String model, String prompt, boolean monitoring,
                     boolean autoSend, boolean useSystemOne, String cannedReply) {
        prefs.edit().putString("endpoint", endpoint.trim())
                .putString("model", model.trim())
                .putString("prompt", prompt.trim())
                .putBoolean("system_one", useSystemOne)
                .putString("canned_reply", cannedReply)
                .putBoolean("monitor", monitoring)
                .putBoolean("auto_send", autoSend).apply();
    }

    /** Reserve a digest for 15 minutes. Synchronized for workers running in this process. */
    public boolean reserve(String digest, long nowMs) {
        synchronized (RESERVATION_LOCK) {
        String previous = prefs.getString("last_digest", "");
        long previousMs = prefs.getLong("last_time", 0);
        if (digest.equals(previous) && nowMs - previousMs >= 0
                && nowMs - previousMs < 15 * 60 * 1000L) return false;
        return prefs.edit().putString("last_digest", digest)
                .putLong("last_time", nowMs).commit();
        }
    }

    public synchronized void addHistory(String text) {
        String cleaned = text.replace("\n", " ").replace("\r", " ");
        String history = prefs.getString("history", "");
        prefs.edit().putString("history",
                (java.time.LocalDateTime.now() + " " + cleaned + "\n" + history)
                        .substring(0, Math.min(2400, (java.time.LocalDateTime.now() + " " + cleaned + "\n" + history).length())))
                .apply();
    }
}
