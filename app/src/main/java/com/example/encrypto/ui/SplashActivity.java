package com.example.encrypto.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import com.example.encrypto.R;
import com.example.encrypto.ui.chatlist.ChatListActivity;
import com.example.encrypto.ui.login.LoginActivity;
import com.example.encrypto.utils.NotificationHelper;
import com.example.encrypto.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 8000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TokenManager.init(this);

        ImageView logo = findViewById(R.id.logo);

        PropertyValuesHolder scalex = PropertyValuesHolder
                .ofFloat("scaleX", .5f, 1f);

        PropertyValuesHolder scaley = PropertyValuesHolder
                .ofFloat("scaleY", .5f, 1f);


        ObjectAnimator animate = ObjectAnimator.ofPropertyValuesHolder(logo, scalex, scaley);
        animate.setDuration(900);
        animate.setRepeatCount(10);
        animate.setRepeatMode(ValueAnimator.REVERSE);
        animate.setInterpolator(new AnticipateInterpolator());
        animate.start();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissions, SPLASH_DELAY);

        NotificationHelper.createNotificationChannels(this);
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            checkLoginStatus();
        }
    }

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                checkLoginStatus();
            }
    );

    private void checkLoginStatus() {
        String token = null;
        try {
            token = TokenManager.getAccessToken();
        } catch (Exception e) {
            Log.e(TAG, "Token check failed", e);
        }

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String userId = prefs.getString("auth_user_id", null);

        if (token != null && !token.isEmpty() && userId != null) {
            startActivity(new Intent(SplashActivity.this, ChatListActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        finish();
    }
}