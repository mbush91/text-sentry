package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class MessagePolicyTest {
    @Test public void allowsNormalPhoneAndPromotionalSms() {
        assertTrue(MessagePolicy.eligible("+15555550123", "Reply STOP to opt out"));
        assertTrue(MessagePolicy.eligible("(555) 555-0123", "Example message"));
    }

    @Test public void rejectsShortCodesAlphanumericSendersAndEmptyMessages() {
        assertFalse(MessagePolicy.eligible("12345", "Reply STOP"));
        assertFalse(MessagePolicy.eligible("SALE", "Reply STOP"));
        assertFalse(MessagePolicy.eligible("+15555550123", "  "));
    }

    @Test public void rejectsVerificationCodeMessages() {
        assertFalse(MessagePolicy.eligible("+15555550123", "Your verification code is 456789"));
        assertFalse(MessagePolicy.eligible("+15555550123", "One-time passcode for sign-in"));
    }

    @Test public void rejectsOversizedMessages() {
        assertFalse(MessagePolicy.eligible("+15555550123", "x".repeat(1601)));
    }

    @Test public void blocksStopOnlyAndCommonAutomatedResponseLoops() {
        assertFalse(MessagePolicy.eligible("+15555550123", "STOP"));
        assertFalse(MessagePolicy.eligible("+15555550123", "unsubscribe"));
        assertFalse(MessagePolicy.eligible("+15555550123", "  QUIT  "));
        assertTrue(MessagePolicy.eligible("+15555550123", "Save 20%, reply STOP to unsubscribe."));
    }

    @Test public void endpointMustBeHttpsAndHaveNoCredentials() {
        assertTrue(MessagePolicy.validEndpoint("https://api.example.com/v1/chat/completions"));
        assertFalse(MessagePolicy.validEndpoint("http://api.example.com/v1"));
        assertFalse(MessagePolicy.validEndpoint("https://user:pass@api.example.com/v1"));
        assertFalse(MessagePolicy.validEndpoint("not a url"));
    }
}
