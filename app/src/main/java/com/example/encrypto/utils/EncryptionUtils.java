package com.example.encrypto.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_SIZE = 256;
    private static final int ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;

    // KEY DERIVATION (PBKDF2)
    private static SecretKey getKey(String keySource, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(
                keySource.toCharArray(),
                salt,
                ITERATIONS,
                AES_KEY_SIZE
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    // ENCRYPT
    public static String encrypt(String plaintext, String keySource) throws Exception {
        // Generate new Salt and IV for every message

        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // Derive Key using the generated Salt
        SecretKey key = getKey(keySource, salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // Initialize and Encrypt
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Combine Salt, IV, and Ciphertext (Salt + IV + Ciphertext + Tag)
        // Array size = 16 (Salt) + 12 (IV) + CiphertextLength (includes Tag)
        int combinedLength = SALT_LENGTH + GCM_IV_LENGTH + cipherText.length;
        byte[] encrypted = new byte[combinedLength];

        System.arraycopy(salt, 0, encrypted, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, encrypted, SALT_LENGTH, GCM_IV_LENGTH);
        System.arraycopy(cipherText, 0, encrypted, SALT_LENGTH + GCM_IV_LENGTH, cipherText.length);

        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    // DECRYPT
    public static String decrypt(String encryptedPayload, String keySource) throws Exception {
        byte[] encryptedBytes = Base64.decode(encryptedPayload, Base64.NO_WRAP);

        // Minimum required length (SALT + IV + Tag) = 16 + 12 + 16 = 44
        if (encryptedBytes.length < SALT_LENGTH + GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Encrypted payload too short/corrupted.");
        }

        // Extract SALT (0 to 16)
        byte[] salt = Arrays.copyOfRange(encryptedBytes, 0, SALT_LENGTH);

        // Extract IV (16 to 28)
        byte[] iv = Arrays.copyOfRange(encryptedBytes, SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH);

        // Extract Ciphertext + Tag (28 to end)
        int cipherTextStart = SALT_LENGTH + GCM_IV_LENGTH; // Index 28
        // Using copyOfRange is safer as it handles array length checks internally
        byte[] cipherText = Arrays.copyOfRange(encryptedBytes, cipherTextStart, encryptedBytes.length);

        // Derive Key using the extracted Salt
        SecretKey key = getKey(keySource, salt);

        // Decrypt and Verify Tag
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] original = cipher.doFinal(cipherText);
        return new String(original, StandardCharsets.UTF_8);
    }
}