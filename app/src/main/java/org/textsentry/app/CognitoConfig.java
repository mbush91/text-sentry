package org.textsentry.app;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;

/** Nonsecret Cognito endpoints obtained from the first-party TextSentry API. */
public final class CognitoConfig {
    public static final String DISCOVERY_URL = "https://api.textsentry.org/v1/auth/config";
    public static final String REDIRECT_URI = "textsentry://auth/callback";

    public final String clientId;
    public final String authorizationServer;
    public final String redirectUri;

    private CognitoConfig(String clientId, String authorizationServer) {
        this.clientId = clientId;
        this.authorizationServer = authorizationServer;
        this.redirectUri = REDIRECT_URI;
    }

    public static CognitoConfig parse(String json) {
        try {
            JSONObject object = new JSONObject(json);
            String id = object.getString("client_id");
            String url = object.getString("authorization_server");
            String callback = object.getString("redirect_uri");
            if (!REDIRECT_URI.equals(callback) || !id.matches("[A-Za-z0-9]{3,128}")) {
                throw new IllegalArgumentException("Unexpected Cognito client or callback");
            }
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                    || !uri.getHost().matches(
                            "textsentry-prod-[0-9]{12}\\.auth\\.us-east-1\\.amazoncognito\\.com")
                    || uri.getPort() != -1 || uri.getUserInfo() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Untrusted Cognito login domain");
            }
            return new CognitoConfig(id, url);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid authentication configuration", e);
        }
    }

    public String authorizationEndpoint() { return authorizationServer + "/oauth2/authorize"; }
    public String tokenEndpoint() { return authorizationServer + "/oauth2/token"; }
}
