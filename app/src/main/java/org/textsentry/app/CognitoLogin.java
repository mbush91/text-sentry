package org.textsentry.app;

import android.net.Uri;
import java.util.Collections;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.ResponseTypeValues;

/** Native Google-only Cognito OAuth authorization code + S256 PKCE. */
public final class CognitoLogin {
    private CognitoLogin() {}

    public static AuthorizationRequest createRequest(CognitoConfig config) {
        AuthorizationServiceConfiguration service = new AuthorizationServiceConfiguration(
                Uri.parse(config.authorizationEndpoint()),
                Uri.parse(config.tokenEndpoint()));
        return new AuthorizationRequest.Builder(service, config.clientId,
                ResponseTypeValues.CODE, Uri.parse(CognitoConfig.REDIRECT_URI))
                .setScope("openid email profile")
                .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier())
                .setAdditionalParameters(Collections.singletonMap("identity_provider", "Google"))
                .build();
    }
}
