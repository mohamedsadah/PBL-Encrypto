package com.example.encrypto.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.data.SupabaseRealtimeClient;
import com.example.encrypto.model.Message;
import com.example.encrypto.utils.EncryptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatViewModel extends ViewModel {

    private static final String TAG = "ChatViewModel";
    private final SupabaseRepository repo = SupabaseRepository.getInstance();
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<SupabaseRepository.ApiResult> sendResult = new MutableLiveData<>();
    private final MutableLiveData<Message> newMessage = new MutableLiveData<>();

    private SupabaseRealtimeClient realtimeClient;
    private String currentChatId;

    private final SupabaseRealtimeClient.MessageListener messageListener = new SupabaseRealtimeClient.MessageListener() {
        @Override public void onOpen() { Log.i(TAG, "WS Connected"); }
        @Override public void onClose(int code, String reason) {}
        @Override public void onError(Throwable t) { Log.e(TAG, "WS Error", t); }
        @Override public void onPhxReply(SupabaseRealtimeClient.Envelope env) {}
        @Override public void onPostgresChange(SupabaseRealtimeClient.Envelope env) {}

        @Override
        public void onNewMessage(Message msg) {
            if (msg == null) return;
            if (msg.chatId != null && msg.chatId.equals(currentChatId)) {
                backgroundExecutor.execute(() -> {
                    try {
                        msg.text = EncryptionUtils.decrypt(msg.text, msg.chatId);
                    } catch (Exception e) {
                        msg.text = "[Decryption Failed]";
                    }
                    newMessage.postValue(msg);
                });
            }
        }
    };

    public ChatViewModel() {
        realtimeClient = SupabaseRealtimeClient.getInstance();
        realtimeClient.addListener(messageListener);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<SupabaseRepository.ApiResult> getSendMessageResult() { return sendResult; }
    public LiveData<Message> getNewMessage() { return newMessage; }

    @Override
    protected void onCleared() {
        stopRealtime();
        backgroundExecutor.shutdown();
        super.onCleared();
    }

    public void loadMessages(String chatId) {
        if (chatId == null || chatId.isEmpty()) return;
        this.currentChatId = chatId;

        if (!realtimeClient.isConnected()) realtimeClient.connect();
        realtimeClient.subscribeToChat(chatId);

        repo.getMessages(chatId, new MutableLiveData<List<Message>>() {
            @Override
            public void postValue(List<Message> rawList) {
                if (rawList == null) return;
                backgroundExecutor.execute(() -> {
                    List<Message> decryptedList = new ArrayList<>();
                    for (Message m : rawList) {
                        try {
                            m.text = EncryptionUtils.decrypt(m.text, chatId);
                        } catch (Exception e) {
                            m.text = "[Decryption Failed]";
                        }
                        decryptedList.add(m);
                    }
                    messages.postValue(decryptedList);
                });
            }
        });
    }

    public void sendMessage(String chatId, String senderId, String content) {
        repo.rpcSendMessage(chatId, senderId, content, sendResult);
    }

    public void sendMessage(Message m) {
        if (m != null) sendMessage(m.chatId, m.senderId, m.text);
    }

    public void stopRealtime() {
        if (realtimeClient != null) {
            try {
                realtimeClient.removeListener(messageListener);
                realtimeClient.unsubscribeFromChat();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping realtime", e);
            }
        }
    }
}