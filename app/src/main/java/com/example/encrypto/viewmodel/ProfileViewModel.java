package com.example.encrypto.viewmodel;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.model.User;
import com.example.encrypto.utils.ImageUtils;

public class ProfileViewModel extends ViewModel {
    private final SupabaseRepository repo = SupabaseRepository.getInstance();

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUploadUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateProfileResult = new MutableLiveData<>();
    private final MutableLiveData<String> passwordChangeResult = new MutableLiveData<>();

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<String> getAvatarUploadUrl() { return avatarUploadUrl; }
    public LiveData<Boolean> getUpdateProfileResult() { return updateProfileResult; }
    public LiveData<String> getPasswordChangeResult() { return passwordChangeResult; }

    public void loadUserProfile(String userId) {
        repo.getCurrentUser(userId, currentUser);
    }

    public void uploadProfilePicture(Context context, Uri uri) {
        new Thread(() -> {
            byte[] data = ImageUtils.getBytesFromUri(context, uri);
            if (data != null) {
                repo.uploadAvatar(data, avatarUploadUrl);
            } else {
                avatarUploadUrl.postValue(null);
            }
        }).start();
    }

    public void updateProfilePicInDb(String userId, String url) {
        repo.updateUserProfile(userId, url, updateProfileResult);
    }

    public void changePassword(String newPass) {
        repo.changePassword(newPass, passwordChangeResult);
    }
}