package org.textsentry.app;

/** A hard-restricted SMS permission request can be refused by device policy or installer. */
final class SmsPermissionRequestGuard {
    private SmsPermissionRequestGuard() { }

    static boolean run(Runnable request) {
        try {
            request.run();
            return true;
        } catch (SecurityException | IllegalArgumentException | IllegalStateException rejected) {
            // Never crash a setup activity because the OS will not show the permission dialog.
            return false;
        }
    }
}
