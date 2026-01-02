package com.example.encrypto.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.encrypto.R;
import com.example.encrypto.ui.chatlist.ChatListActivity;
import com.example.encrypto.ui.signup.SignUpActivity;
import com.example.encrypto.utils.TokenManager;
import com.example.encrypto.viewmodel.LoginViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private LoginViewModel vm;
    private TextInputEditText phoneField, passwordField;
    private MaterialButton btnLogin, btnSignUp;
    private CircularProgressIndicator progressBar;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        phoneField = findViewById(R.id.phoneField);
        passwordField = findViewById(R.id.passwordField);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);

        TokenManager.init(this);

        vm = new ViewModelProvider(this).get(LoginViewModel.class);

        vm.getAuthResult().observe(this, new Observer<Map<String, Object>>() {
            @Override
            public void onChanged(Map<String, Object> result) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (result == null) {
                    Toast.makeText(LoginActivity.this, "An unexpected error occurred", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (result.containsKey("error")) {
                    String errorMsg = (String) result.get("error");
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("LoginActivity", "Auth Result: " + result);

                String accessToken = (String) result.get("access_token");
                String refreshToken = (String) result.get("refresh_token");

                Map<String, Object> userMap = (Map<String, Object>) result.get("user");
                String userId = null;
                String fullName = null;

                if (userMap != null) {
                    userId = (String) userMap.get("id");
                    Map<String, Object> metaData = (Map<String, Object>) userMap.get("user_metadata");
                    if (metaData != null && metaData.containsKey("full_name")) {
                        fullName = (String) metaData.get("full_name");
                    }
                }

                if (accessToken != null && !accessToken.isEmpty() && userId != null) {
                    TokenManager.saveTokens(accessToken, refreshToken);

                    SharedPreferences.Editor editor = getSharedPreferences("auth", MODE_PRIVATE).edit();
                    editor.putString("auth_user_id", userId);

                    if (fullName != null) {
                        editor.putString("auth_user_name", fullName);
                    }
                    editor.apply();

                    startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed. Invalid response data.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLogin.setOnClickListener(v -> {
            String phone = phoneField.getText() != null ? phoneField.getText().toString().trim() : "";
            String pass = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";

            if (!isValidPhone(phone)) {
                phoneField.setError("invalid Phone Number (+91...)");
                return;
            }
            if (TextUtils.isEmpty(pass)) {
                passwordField.setError("Required");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            vm.signInWithPhone(phone, pass);
        });

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        if (!phone.startsWith("+")) return false;
        String digits = phone.substring(1).replaceAll("\\s+", "");
        return digits.matches("\\d{8,15}");
    }
}