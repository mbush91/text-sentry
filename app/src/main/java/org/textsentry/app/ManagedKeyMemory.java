package org.textsentry.app;

/**
 * Process-only managed key cache. No SharedPreferences, Keystore, database,
 * backup, saved-instance-state or logs. Cleared on sign-out and data erasure;
 * lost automatically whenever Android kills this process.
 */
final class ManagedKeyMemory {
    private static String key;
    private static long expiresAtMillis;
    private static long generation;
    private ManagedKeyMemory() {}

    static synchronized void install(String newKey, long expiry) {
        if (newKey == null || newKey.isBlank() || expiry <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Invalid managed key");
        }
        key = newKey;
        expiresAtMillis = expiry;
    }

    static synchronized String currentKey() {
        if (System.currentTimeMillis() >= expiresAtMillis) {
            clear();
            return null;
        }
        return key;
    }

    static synchronized long generation() { return generation; }

    /** A signed-out session must never repopulate RAM from an in-flight request. */
    static synchronized boolean installIfUnchanged(String newKey, long expiry, long expected) {
        if (generation != expected) return false;
        install(newKey, expiry);
        return true;
    }

    static synchronized boolean hasActiveKey() { return currentKey() != null; }

    static synchronized void clear() {
        key = null;
        expiresAtMillis = 0;
        generation++;
    }
}
