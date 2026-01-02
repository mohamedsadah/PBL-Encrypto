package com.example.encrypto.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREF_FILE = "secure_prefs_v1";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (prefs != null) return;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREF_FILE,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to plaintext SharedPreferences", e);
            prefs = context.getApplicationContext().getSharedPreferences(PREF_FILE + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public static void saveTokens(String accessToken, @Nullable String refreshToken) {
        if (prefs == null) throw new IllegalStateException("TokenManager not initialized. Call TokenManager.init(context)");
        prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .apply();
    }

    @Nullable
    public static String getAccessToken() {
        if (prefs == null) throw new IllegalStateException("TokenManager not initialized. Call TokenManager.init(context)");
        return prefs.getString(KEY_ACCESS, null);
    }

    @Nullable
    public static String getRefreshToken() {
        if (prefs == null) throw new IllegalStateException("TokenManager not initialized. Call TokenManager.init(context)");
        return prefs.getString(KEY_REFRESH, null);
    }

    public static void clear() {
        if (prefs == null) return;
        prefs.edit().clear().apply();
    }
}
