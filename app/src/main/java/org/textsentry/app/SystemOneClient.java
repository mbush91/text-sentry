package org.textsentry.app;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

/**
 * TypeSafe System One/Jev is a classifier, not a chat model. It decides whether
 * a pre-written reply is appropriate; it never invents the text to send.
 */
public final class SystemOneClient {
    public static final String ENDPOINT = "https://api.typesafe.ai/v1/systemone";
    public static final String MODEL = "jev-latest";
    public static final double APPROVAL_THRESHOLD = 0.95;

    /** Injectable transport keeps every PR test offline and deterministic. */
    public interface Transport {
        String post(String endpoint, String apiKey, String body) throws Exception;
    }

    private final Transport transport;
    public SystemOneClient() { this(new HttpsTransport()); }
    public SystemOneClient(Transport transport) {
        if (transport == null) throw new IllegalArgumentException("Transport required");
        this.transport = transport;
    }

    public double score(String apiKey, String model, String userRule, String message)
            throws Exception {
        if (isEmpty(apiKey) || !MODEL.equals(model) || isEmpty(userRule) || isEmpty(message)) {
            throw new IllegalArgumentException("Missing System One settings");
        }
        if (message.length() > 1600 || userRule.length() > 3000) {
            throw new IllegalArgumentException("Request too long");
        }
        JSONObject criteria = new JSONObject()
                .put("true", "The message unmistakably matches the owner's rule without assumptions")
                .put("false", "Personal, security, scam, transactional, ambiguous or unrelated message");
        JSONObject question = new JSONObject()
                .put("type", "noul")
                .put("instructions", "Does this message unambiguously meet the owner's rules? "
                        + "If unsure, answer NO. Apply the following owner rule: " + userRule)
                .put("criteria", criteria);
        JSONObject request = new JSONObject()
                .put("model", MODEL)
                .put("state", message)
                .put("questions", new JSONObject().put("reply", question));
        final String raw = transport.post(ENDPOINT, apiKey.trim(), request.toString());
        try {
            JSONObject json = new JSONObject(raw);
            if (isEmpty(json.optString("model", ""))) throw new IllegalArgumentException("No model");
            JSONObject answers = json.optJSONObject("answers");
            if (answers == null) throw new IllegalArgumentException("No answers");
            JSONObject reply = answers.optJSONObject("reply");
            if (reply == null || !"noul".equals(reply.optString("type"))) {
                throw new IllegalArgumentException("Wrong answer type");
            }
            Object probability = reply.opt("noul");
            if (!(probability instanceof Number)) throw new IllegalArgumentException("No probability");
            double value = ((Number) probability).doubleValue();
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Invalid probability");
            }
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unexpected TypeSafe response", e);
        }
    }

    public AiDecision decide(String apiKey, String model, String userRule,
                             String message, String cannedReply) throws Exception {
        return decide(apiKey, model, userRule, message, cannedReply, APPROVAL_THRESHOLD);
    }

    public AiDecision decide(String apiKey, String model, String userRule,
                             String message, String cannedReply, double threshold) throws Exception {
        if (!Double.isFinite(threshold) || threshold < 0.50 || threshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be 50–100%");
        }
        if (!validReply(cannedReply)) {
            throw new IllegalArgumentException("Invalid configured reply");
        }
        boolean approved = score(apiKey, model, userRule, message) >= threshold;
        // User-selected reply only; never send message text from an LLM response.
        return AiDecision.parse(new JSONObject().put("should_reply", approved)
                .put("reply", approved ? cannedReply.trim() : "").toString());
    }

    public static boolean validReply(String reply) {
        if (reply == null || reply.isEmpty() || reply.trim().isEmpty()
                || reply.length() > 120) return false;
        return !reply.contains("\n") && !reply.contains("\r");
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class HttpsTransport implements Transport {
        @Override public String post(String endpoint, String apiKey, String body) throws Exception {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try {
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
                if (connection.getResponseCode() != 200) {
                    // Do not expose upstream error body: it may include echoed SMS.
                    throw new IOException("System One HTTP " + connection.getResponseCode());
                }
                try (InputStream input = connection.getInputStream()) {
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int count;
                    while ((count = input.read(buf)) != -1) {
                        if (result.size() + count > 65536) throw new IOException("Response too large");
                        result.write(buf, 0, count);
                    }
                    return new String(result.toByteArray(), StandardCharsets.UTF_8);
                }
            } finally {
                connection.disconnect();
            }
        }
    }
}
