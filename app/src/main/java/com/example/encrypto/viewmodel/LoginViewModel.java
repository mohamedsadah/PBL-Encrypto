package com.example.encrypto.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.encrypto.data.SupabaseRepository;

import java.util.Map;

public class LoginViewModel extends ViewModel {
    private final SupabaseRepository repo = SupabaseRepository.getInstance();
    private final MutableLiveData<Map<String, Object>> authResult = new MutableLiveData<>();

    public LiveData<Map<String, Object>> getAuthResult() { return authResult; }

    public void signIn(String email, String password) {
        repo.signIn(email, password, authResult);
    }

    public void signInWithPhone(String phone, String password) {
        repo.signInWithPhone(phone, password, authResult);
    }

}
