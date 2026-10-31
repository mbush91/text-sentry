package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;
import org.json.JSONException;

public class AiDecisionTest {
    @Test public void extractsApprovedReply() throws Exception {
        AiDecision result = AiDecision.parse("{\"should_reply\":true,\"reply\":\"STOP\"}");
        assertTrue(result.shouldReply);
        assertEquals("STOP", result.reply);
    }

    @Test public void acceptsNoReply() throws Exception {
        assertFalse(AiDecision.parse("{\"should_reply\":false,\"reply\":\"\"}").shouldReply);
    }

    @Test public void refusesMalformedOrUntrustedReplies() throws Exception {
        reject("{\"should_reply\":\"true\",\"reply\":\"STOP\"}");
        reject("{\"should_reply\":true,\"reply\":\"\"}");
        reject("{\"should_reply\":true,\"reply\":\"hello\\nworld\"}");
        reject("{\"should_reply\":true,\"reply\":3}");
        reject("STOP");
        reject("{\"should_reply\":true,\"reply\":\"" + "x".repeat(121) + "\"}");
    }

    private void reject(String input) {
        try {
            AiDecision.parse(input);
            fail("Expected rejection");
        } catch (JSONException expected) {
            // fail-closed behavior
        }
    }
}
