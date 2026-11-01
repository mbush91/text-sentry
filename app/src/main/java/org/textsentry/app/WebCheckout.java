package org.textsentry.app;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Identified-user RevenueCat checkout for the independent sideload edition.
 * Never uses an email address, Google token, SMS content, or anonymous identity as the buyer ID.
 */
final class WebCheckout {
    private static final String CHECKOUT = "https://pay.rev.cat/mimrlvpobvqnjikw/";
    private static final String ACCOUNT = "https://api.textsentry.org/v1/me";
    private WebCheckout() {}

    static String purchaseUrl(String userId) {
        if (userId == null || userId.isBlank() || userId.length() > 256) {
            throw new IllegalArgumentException("Missing customer identity");
        }
        return purchaseUrl(userId, CHECKOUT);
    }

    static String purchaseUrl(String userId, String checkoutBaseUrl) {
        if (userId == null || userId.isBlank() || userId.length() > 256) {
            throw new IllegalArgumentException("Missing customer identity");
        }
        if (checkoutBaseUrl == null || !checkoutBaseUrl.matches(
                "https://pay[.]rev[.]cat/[A-Za-z0-9_/-]+/")) {
            throw new IllegalArgumentException("Invalid checkout host");
        }
        return checkoutBaseUrl + URLEncoder.encode(userId, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /** Parse identity derived from the backend-validated Cognito JWT, not a client-supplied ID. */
    static String userIdFromAccount(String body) {
        try {
            String id = new JSONObject(body).getString("user_id");
            if (id.isBlank() || id.length() > 256) {
                throw new IllegalArgumentException("Invalid account identity");
            }
            return id;
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Invalid account identity");
        }
    }

    /** Only call on a background thread; the authenticated account endpoint returns no SMS. */
    static String fetchUserId(String accessToken) throws IOException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("Sign in again before subscribing.");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(ACCOUNT).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-store");
            if (connection.getResponseCode() != 200) {
                throw new IOException("Unable to confirm your account. Sign in again.");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] chunk = new byte[1024];
                int count;
                while ((count = stream.read(chunk)) != -1) {
                    if (buffer.size() + count > 8192) {
                        throw new IOException("Invalid account response");
                    }
                    buffer.write(chunk, 0, count);
                }
            }
            try {
                return userIdFromAccount(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                throw new IOException("Invalid account response");
            }
        } finally {
            connection.disconnect();
        }
    }
}
