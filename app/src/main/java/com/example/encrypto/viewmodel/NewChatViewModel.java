package com.example.encrypto.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.model.User;

import java.util.List;

public class NewChatViewModel extends ViewModel {
    private final SupabaseRepository repo = SupabaseRepository.getInstance();

    private final MutableLiveData<List<User>> registeredUsers = new MutableLiveData<>();
    private final MutableLiveData<SupabaseRepository.ApiResult> rpcResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public NewChatViewModel() {
        rpcResult.observeForever(apiResult -> {
            if (apiResult == null) return;

            if (apiResult.success && apiResult.body != null) {
                registeredUsers.postValue((List<User>) apiResult.body);
            } else {
                error.postValue(apiResult.error != null ? apiResult.error : "Failed to find users");
            }
        });
    }

    public LiveData<List<User>> getRegisteredUsers() { return registeredUsers; }
    public LiveData<String> getError() { return error; }

    public void findRegisteredUsers(List<String> phoneNumbers, String currentUserId) {
        repo.rpcFindUsersByPhone(phoneNumbers, currentUserId, rpcResult);
    }
}