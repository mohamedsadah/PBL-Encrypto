package com.example.encrypto.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.encrypto.data.SupabaseRepository;

import java.util.Map;

public class SignUpViewModel extends ViewModel {
    private final SupabaseRepository repo = SupabaseRepository.getInstance();

    private final MutableLiveData<SupabaseRepository.ApiResult> sendMessageResult =
            new MutableLiveData<>();
    private final MutableLiveData<SupabaseRepository.ApiResult> signUpResult = new MutableLiveData<>();

    public LiveData<SupabaseRepository.ApiResult> getSignUpResult() { return signUpResult; }

    public void signUpWithPhone(String phone, String fullname, String password) {
        repo.signUpWithPhone(phone, fullname, password, signUpResult);
    }

    public void sendMessage(String chatId, String senderId, String content) {
        repo.rpcSendMessage(chatId, senderId, content, sendMessageResult);
    }
}
