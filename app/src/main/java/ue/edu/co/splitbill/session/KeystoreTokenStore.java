package ue.edu.co.splitbill.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Guarda el token cifrado con AES-GCM usando una llave del Android Keystore.
 *
 * El token es una credencial: quien lo tenga puede actuar a nombre del usuario hasta que venza. Por
 * eso no se guarda en texto plano. La llave AES vive dentro del Keystore del sistema (en hardware
 * seguro cuando el celular lo tiene) y nunca sale de ahi: ni siquiera la app puede leerla, solo
 * pedirle al Keystore que cifre o descifre. En SharedPreferences queda unicamente el texto cifrado.
 *
 * Se usa esto en vez de EncryptedSharedPreferences porque esa libreria (security-crypto) esta
 * deprecada; el mecanismo de fondo es el mismo.
 */
public class KeystoreTokenStore implements TokenStore {

    private static final String TAG = "KeystoreTokenStore";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "splitbill_token_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private static final String PREFS_NAME = "splitbill_token";
    private static final String PREF_TOKEN = "token";
    private static final String PREF_IV = "iv";

    private final SharedPreferences preferences;

    public KeystoreTokenStore(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void save(String token) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            //el vector de inicializacion no es secreto, pero hace falta para descifrar
            this.preferences.edit()
                    .putString(PREF_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "ERROR AL CIFRAR EL TOKEN", e);
            clear();
        }
    }

    @Override
    public String read() {
        String encrypted = this.preferences.getString(PREF_TOKEN, null);
        String iv = this.preferences.getString(PREF_IV, null);
        if (encrypted == null || iv == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)));
            byte[] token = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
            return new String(token, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            //si el texto fue alterado o la llave cambio, GCM lo detecta: se descarta y toca iniciar sesion
            Log.e(TAG, "ERROR AL DESCIFRAR EL TOKEN", e);
            clear();
            return null;
        }
    }

    @Override
    public void clear() {
        this.preferences.edit().clear().apply();
    }

    /** La llave se crea la primera vez y despues el Keystore la entrega por su alias. */
    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            }
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException("No se pudo abrir el Keystore", e);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
