package com.example.encrypto.ui.signup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.encrypto.R;
import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.ui.login.LoginActivity;
import com.example.encrypto.viewmodel.SignUpViewModel;
import com.example.encrypto.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private TextInputEditText phoneField, passwordField, fullname;
    private MaterialButton btnSignUp, btnBackToLogin;
    private CircularProgressIndicator progressBar;
    private SignUpViewModel vm;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        try {
            TokenManager.init(getApplicationContext());
        } catch (Exception ignored) {}

        phoneField = findViewById(R.id.phoneField);
        fullname = findViewById(R.id.fullname);
        passwordField = findViewById(R.id.passwordField);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        vm = new ViewModelProvider(this).get(SignUpViewModel.class);

        vm.getSignUpResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            btnSignUp.setEnabled(true);

            if (result == null || !result.success) {
                String errorMsg = (result != null && result.error != null) ? result.error : "Sign up failed. Try again.";
                Log.d("SignUpActivity", "Error message: " + errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                return;
            }

            if (result.body == null) {
                Toast.makeText(this, "Account created. Please verify your phone.", Toast.LENGTH_LONG).show();
                Intent i = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
                return;
            }

            Map<String, Object> body = (Map<String, Object>) result.body;
            Log.d("SignUpActivity", "result body:"+ body.toString());

            if (body.containsKey("access_token")) {
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");

                if (accessToken != null && !accessToken.isEmpty()) {
                    try {
                        TokenManager.saveTokens(accessToken, refreshToken);
                    } catch (IllegalStateException e) {
                        getSharedPreferences("auth", MODE_PRIVATE)
                                .edit()
                                .putString("access_token", accessToken)
                                .putString("refresh_token", refreshToken)
                                .apply();
                    }
                    Toast.makeText(SignUpActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                }

                Intent i = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
                return;
            }

            Toast.makeText(SignUpActivity.this, "Account created. Please verify your phone.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            String phone = phoneField.getText() != null ? phoneField.getText().toString().trim() : "";
            String name = fullname.getText() != null ? fullname.getText().toString().trim() : "";
            String pass = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";

            if (!isValidPhone(phone)) {
                phoneField.setError("Phone must be 10+ with code (e.g. +911234567890)");
                return;
            }
            if (TextUtils.isEmpty(pass) || pass.length() < 6) {
                passwordField.setError("Password must be minimum 6 chars");
                return;
            }

            if (name.isEmpty()){
                fullname.setError("Name is required");
                return;
            }

            if (name.length() < 5){
                fullname.setError("Name must be minimum 5 chars");
                return;
            }

            Log.d("SignUpActivity", "phone:"+ phone);


            progressBar.setVisibility(View.VISIBLE);
            btnSignUp.setEnabled(false);
            vm.signUpWithPhone(phone, name, pass);
        });

        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        if (!phone.startsWith("+")) return false;
        String digits = phone.substring(1).replaceAll("\\s+", "");
        return digits.matches("\\d{10,12}");
    }
}