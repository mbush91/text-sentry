package org.textsentry.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import net.openid.appauth.AuthState;
import org.json.JSONException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Keystore-backed OAuth token storage, separate from BYOK and SMS history. */
public final class AuthSessionStore {
    private static final String ALIAS = "textsentry_oauth_session_v1";
    private static final String PREFS = "textsentry_auth_session";
    private final SharedPreferences preferences;

    public AuthSessionStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey secretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public synchronized void save(AuthState state) throws Exception {
        if (state == null || !state.isAuthorized() || state.getRefreshToken() == null) {
            throw new IllegalArgumentException("Only a complete OAuth session can be stored");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey());
        String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
        String ciphertext = Base64.encodeToString(
                cipher.doFinal(state.jsonSerializeString().getBytes(StandardCharsets.UTF_8)),
                Base64.NO_WRAP);
        if (!preferences.edit().putString("state", iv + ":" + ciphertext).commit()) {
            throw new IllegalStateException("Could not store encrypted session");
        }
    }

    public synchronized AuthState load() throws Exception {
        String saved = preferences.getString("state", "");
        if (saved.isEmpty()) return null;
        String[] parts = saved.split(":", 2);
        if (parts.length != 2) throw new IllegalStateException("Invalid encrypted session");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        try {
            return AuthState.jsonDeserialize(new String(
                    cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new IllegalStateException("Invalid session", e);
        }
    }

    public synchronized boolean hasSession() {
        return preferences.contains("state");
    }

    public synchronized void clear() {
        if (!preferences.edit().remove("state").commit()) {
            throw new IllegalStateException("Could not remove account tokens");
        }
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not erase account encryption key", e);
        }
    }
}
