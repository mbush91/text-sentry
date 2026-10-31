package org.textsentry.app;

import java.util.Locale;
import java.util.regex.Pattern;

/** Safety checks enforced locally before AI is consulted. */
public final class MessagePolicy {
    private static final Pattern CODE = Pattern.compile(
            "\\b(?:one[ -]?time|verification|verify your|security code|passcode|otp|2fa|authenticat(?:ion|or)|login code)\\b",
            Pattern.CASE_INSENSITIVE);

    private MessagePolicy() {}

    public static boolean eligible(String sender, String body) {
        if (sender == null || body == null) return false;
        String number = sender.replaceAll("[\\s()\\-]", "");
        if (!number.matches("\\+?[0-9]{10,15}")) return false;
        String text = body.trim();
        if (text.isEmpty() || text.length() > 1600) return false;
        if (CODE.matcher(text).find()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("emergency alert") && !lower.contains("your sign-in code");
    }

    public static boolean validEndpoint(String endpoint) {
        if (endpoint == null) return false;
        try {
            java.net.URI uri = new java.net.URI(endpoint.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && !uri.getHost().trim().isEmpty()
                    && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }
}
