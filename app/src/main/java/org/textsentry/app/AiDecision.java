package org.textsentry.app;

import org.json.JSONException;
import org.json.JSONObject;

/** Strict, fail-closed parsing of untrusted model output. */
public final class AiDecision {
    public final boolean shouldReply;
    public final String reply;

    private AiDecision(boolean shouldReply, String reply) {
        this.shouldReply = shouldReply;
        this.reply = reply;
    }

    public static AiDecision parse(String output) throws JSONException {
        JSONObject data = new JSONObject(output);
        Object flag = data.opt("should_reply");
        if (!(flag instanceof Boolean)) throw new JSONException("Missing boolean should_reply");
        if (!((Boolean) flag)) return new AiDecision(false, "");
        Object candidate = data.opt("reply");
        if (!(candidate instanceof String)) throw new JSONException("Missing string reply");
        String reply = ((String) candidate).trim();
        if (reply.isEmpty() || reply.length() > 120 || reply.contains("\n") || reply.contains("\r")) {
            throw new JSONException("Unsafe reply length or format");
        }
        return new AiDecision(true, reply);
    }
}
