package com.example.encrypto.data;

import androidx.annotation.NonNull;

import com.example.encrypto.utils.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        if (path.contains("/auth/v1/signup") || path.contains("/auth/v1/token")) {
            return chain.proceed(original);
        }

        String token = null;
        try {
            token = TokenManager.getAccessToken();
        } catch (IllegalStateException ignored) {
        }

        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }

        Request.Builder builder = original.newBuilder()
                .header("Authorization", "Bearer " + token);

        Request request = builder.build();
        return chain.proceed(request);
    }
}