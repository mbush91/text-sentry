package org.textsentry.app;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Fetches an already-issued OpenRouter key, or provisions this month's first key.
 * Only sends Cognito bearer tokens; never sends SMS or stores keys on disk.
 */
final class ManagedKeyClient {
    private static final String URL = "https://api.textsentry.org/v1/key";
    private ManagedKeyClient() {}

    static final class KeyResponse {
        final String key;
        final String period;
        final long expiresAtMillis;

        KeyResponse(String key, String period, long expiresAtMillis) {
            this.key = key;
            this.period = period;
            this.expiresAtMillis = expiresAtMillis;
        }

        String status() { return "✓ Managed key ready · $1/month · " + period; }
    }

    static KeyResponse parse(String json) {
        try {
            JSONObject data = new JSONObject(json);
            String key = data.getString("key");
            String period = data.getString("period");
            String expiry = data.getString("expires_at");
            double cap = data.getDouble("monthly_limit_usd");
            long expiresAtMillis = Instant.parse(expiry).toEpochMilli();
            if (key.isBlank() || key.length() > 512
                    || !period.matches("[0-9]{4}-(0[1-9]|1[0-2])")
                    || cap != 1.0 || expiresAtMillis <= System.currentTimeMillis()) {
                throw new IllegalArgumentException("Invalid managed key response");
            }
            return new KeyResponse(key, period, expiresAtMillis);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid managed key response");
        }
    }

    static KeyResponse fetch(String accessToken) throws IOException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("Missing account session");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-store");
            int status = connection.getResponseCode();
            if (status != 200 && status != 201) {
                // Never include response bodies, tokens or provider keys in errors.
                throw new IOException(status == 401 ? "Session expired. Sign in again."
                        : status == 429 ? "Key limit reached until next month."
                        : status == 409 ? "Provisioning in progress. Try again shortly."
                        : "Managed key unavailable (HTTP " + status + ").");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] chunk = new byte[1024];
                int n;
                while ((n = input.read(chunk)) != -1) {
                    if (buffer.size() + n > 8192) {
                        throw new IOException("Invalid key response size");
                    }
                    buffer.write(chunk, 0, n);
                }
            }
            try {
                return parse(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid key response");
            }
        } finally {
            connection.disconnect();
        }
    }
}
