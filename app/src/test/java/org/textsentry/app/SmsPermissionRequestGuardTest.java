package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class SmsPermissionRequestGuardTest {
    @Test public void acceptedRequestPasses() {
        assertTrue(SmsPermissionRequestGuard.run(() -> { }));
    }

    @Test public void restrictedPermissionRejectionDoesNotEscape() {
        assertFalse(SmsPermissionRequestGuard.run(() -> { throw new SecurityException("restricted"); }));
        assertFalse(SmsPermissionRequestGuard.run(() -> { throw new IllegalArgumentException("invalid"); }));
        assertFalse(SmsPermissionRequestGuard.run(() -> { throw new IllegalStateException("busy"); }));
    }
}
