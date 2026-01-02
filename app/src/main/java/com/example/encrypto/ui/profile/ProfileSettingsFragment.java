package com.example.encrypto.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.encrypto.R;
import com.example.encrypto.ui.login.LoginActivity;
import com.example.encrypto.utils.TokenManager;
import com.example.encrypto.viewmodel.ProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileSettingsFragment extends Fragment {

    private ProfileViewModel vm;
    private ImageView avatar;
    private TextView tvName;
    private Button btnChangePass, btnLogout, btnChangeAvatar;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvName);
        btnChangePass = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);

        vm = new ViewModelProvider(this).get(ProfileViewModel.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("auth_user_id", null);

        Glide.with(this).load(R.drawable.ic_person).into(avatar);

        vm.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                if (user.displayName != null) {
                    tvName.setText(user.displayName);
                }

                if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
                    Glide.with(this)
                            .load(user.avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(avatar);
                }
            }
        });

        vm.getAvatarUploadUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                Glide.with(this).load(url).circleCrop().into(avatar);
                Toast.makeText(getContext(), "Saving profile...", Toast.LENGTH_SHORT).show();
                vm.updateProfilePicInDb(currentUserId, url);
            } else {
                Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
            }
        });

        vm.getUpdateProfileResult().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });

        vm.getPasswordChangeResult().observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
        });

        btnChangeAvatar.setOnClickListener(v -> pickImage());
        btnChangePass.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logout());

        if (currentUserId != null) {
            vm.loadUserProfile(currentUserId);
        }


        avatar.setOnClickListener(v -> {

            String currentName = tvName.getText().toString();
            String currentUrl = null;

            // Safely get the URL from the ViewModel's current value
            if (vm.getCurrentUser().getValue() != null) {
                currentUrl = vm.getCurrentUser().getValue().avatarUrl;
            }
            // Or fallback to the uploaded one
            if (currentUrl == null && vm.getAvatarUploadUrl().getValue() != null) {
                currentUrl = vm.getAvatarUploadUrl().getValue();
            }

            if (currentUrl != null) {
                openProfileViewer(currentUrl, currentName);
            } else {
                Toast.makeText(getContext(), "No profile picture set", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProfileViewer(String url, String name) {
        ProfileViewerFragment fragment = ProfileViewerFragment.newInstance(url, name);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .add(R.id.fragment_container, fragment) // keep profile settings underneath
                .addToBackStack(null)
                .commit();
    }

    private void logout() {
        try {
            TokenManager.clear();
        } catch (Exception ignored) {}

        SharedPreferences authPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        authPrefs.edit().clear().apply();

        SharedPreferences chatPrefs = requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE);
        chatPrefs.edit().clear().apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();
                        vm.uploadProfilePicture(requireContext(), uri);
                    }
                }
            }
    );

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(i);
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);
        TextInputLayout layoutNew = dialogView.findViewById(R.id.inputLayoutNew);
        TextInputLayout layoutConfirm = dialogView.findViewById(R.id.inputLayoutConfirm);

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setIcon(R.drawable.ic_person)
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Update", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String newPass = etNew.getText() != null ? etNew.getText().toString() : "";
                String confirmPass = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

                layoutNew.setError(null);
                layoutConfirm.setError(null);

                if (newPass.length() < 6) {
                    layoutNew.setError("Password must be at least 6 characters");
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    layoutConfirm.setError("Passwords do not match");
                    return;
                }

                vm.changePassword(newPass);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

}