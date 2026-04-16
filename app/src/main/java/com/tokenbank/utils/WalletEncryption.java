package com.tokenbank.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;


/**
 * Utility for encrypting wallet private keys at rest.
 *
 * On API 23+ the Android Keystore is used to generate and store an AES-GCM
 * key that never leaves secure hardware.  The encrypted payload is a Base64
 * string prefixed with {@value #ENCRYPTED_PREFIX}.
 *
 * On API < 23 the value is stored as plain-text – identical to the original
 * behaviour – so no data-loss occurs on older devices.
 *
 * Decryption detects the prefix automatically, making the upgrade transparent:
 * existing plain-text private keys continue to work after an app update.
 */
public class WalletEncryption {

    private static final String KEY_ALIAS = "tp_wallet_pk_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** Prefix written before every encrypted payload. */
    public static final String ENCRYPTED_PREFIX = "enc:";

    private static final String TAG = "WalletEncryption";

    /**
     * Encrypts {@code plaintext} and returns the result prefixed with
     * {@value #ENCRYPTED_PREFIX}.  If encryption is not available (API < 23)
     * or fails, the original value is returned unchanged.
     */
    public static String encrypt(String plaintext) {
        if (TextUtils.isEmpty(plaintext)) {
            return plaintext;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return encryptInternal(plaintext);
            } catch (Exception e) {
                TLog.e(TAG, "Encryption failed, falling back to plaintext: " + e.getMessage());
            }
        }
        return plaintext;
    }

    /**
     * Decrypts a value that was previously returned by {@link #encrypt}.
     * Plain-text legacy values (no prefix) are returned as-is.
     * Returns {@code null} if decryption fails.
     */
    public static String decrypt(String stored) {
        if (TextUtils.isEmpty(stored)) {
            return stored;
        }
        if (stored.startsWith(ENCRYPTED_PREFIX)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    return decryptInternal(stored.substring(ENCRYPTED_PREFIX.length()));
                } catch (Exception e) {
                    TLog.e(TAG, "Decryption failed: " + e.getMessage());
                    return null;
                }
            }
            // Encrypted on a newer device, now running on API < 23 – inaccessible.
            TLog.e(TAG, "Cannot decrypt: API level too low");
            return null;
        }
        // Legacy plain-text private key – return unchanged.
        return stored;
    }

    // -------------------------------------------------------------------------
    // Internal helpers (guarded by @TargetApi so the compiler is satisfied)
    // -------------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.M)
    private static String encryptInternal(String plaintext) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Layout: [iv_len (1 byte)][iv][ciphertext]
        byte[] combined = new byte[1 + iv.length + encryptedBytes.length];
        combined[0] = (byte) iv.length;
        System.arraycopy(iv, 0, combined, 1, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, 1 + iv.length, encryptedBytes.length);

        return ENCRYPTED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static String decryptInternal(String base64Data) throws Exception {
        byte[] combined = Base64.decode(base64Data, Base64.NO_WRAP);
        int ivLength = combined[0] & 0xFF;
        byte[] iv = new byte[ivLength];
        System.arraycopy(combined, 1, iv, 0, ivLength);
        byte[] ciphertext = new byte[combined.length - 1 - ivLength];
        System.arraycopy(combined, 1 + ivLength, ciphertext, 0, ciphertext.length);

        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return new String(cipher.doFinal(ciphertext), "UTF-8");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build());
        return keyGenerator.generateKey();
    }
}
