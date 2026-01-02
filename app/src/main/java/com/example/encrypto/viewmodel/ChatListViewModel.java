package com.example.encrypto.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.data.SupabaseRealtimeClient;
import com.example.encrypto.model.Chat;
import com.example.encrypto.model.Message;
import com.example.encrypto.ui.chatlist.ChatsFragment;
import com.example.encrypto.utils.EncryptionUtils;
import com.example.encrypto.utils.NotificationHelper;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListViewModel extends ViewModel {

    private final SupabaseRepository repo = SupabaseRepository.getInstance();
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Chat>> chats = new MutableLiveData<>();
    private final MutableLiveData<Chat> newChat = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Message> incomingRealtimeMessage = new MutableLiveData<>();

    private String currentUserId;
    private SupabaseRealtimeClient realtimeClient;
    private final Map<String, String> localContactMap = new HashMap<>();

    public LiveData<List<Chat>> getChats() { return chats; }
    public LiveData<Chat> getNewChat() { return newChat; }
    public LiveData<String> getError() { return error; }
    public LiveData<Message> getIncomingRealtimeMessage() { return incomingRealtimeMessage; }

    public void clearNewChat() {
        newChat.setValue(null);
        Log.d("ChatListVM", "Cleared newChat LiveData");
    }

    private final SupabaseRealtimeClient.MessageListener realtimeListener = new SupabaseRealtimeClient.MessageListener() {
        @Override public void onOpen() { Log.i("ChatListVM", "WS Connected"); }
        @Override public void onClose(int code, String reason) {}
        @Override public void onError(Throwable t) { error.postValue("Realtime Connection Error"); }
        @Override public void onPhxReply(SupabaseRealtimeClient.Envelope env) {}

        @Override
        public void onPostgresChange(SupabaseRealtimeClient.Envelope env) {
            Log.i("ChatListVM", "OnPostgresChange called");
            backgroundExecutor.execute(() -> {
                try {
                    if (env.payload == null) return;
                    Map data = (Map) env.payload.get("data");
                    if (data == null) return;
                    Map rec = (Map) data.get("record");
                    if (rec == null) return;

                    String json = gson.toJson(rec);
                    Message msg = gson.fromJson(json, Message.class);

                    try {
                        msg.text = EncryptionUtils.decrypt(msg.text, msg.chatId);
                    } catch (Exception e) {
                        msg.text = "[Encrypted]";
                    }

                    incomingRealtimeMessage.postValue(msg);
                    updateChatPreview(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onNewMessage(Message message) {
            if (message != null) {
                incomingRealtimeMessage.postValue(message);
            }
        }
    };

    public ChatListViewModel() {}

    @Override
    protected void onCleared() {
        stopRealtime();
        backgroundExecutor.shutdown();
        super.onCleared();
    }

    public void startRealtime(String userId) {
        if (realtimeClient != null && realtimeClient.isConnected()) return;
        this.currentUserId = userId;
        realtimeClient = SupabaseRealtimeClient.getInstance();
        realtimeClient.addListener(realtimeListener);
        if (!realtimeClient.isConnected()) realtimeClient.connect();
        realtimeClient.subscribeToChat(null);
    }

    public void stopRealtime() {
        if (realtimeClient != null) realtimeClient.removeListener(realtimeListener);
    }

    public void loadChats(String userId, Context context) {
        backgroundExecutor.execute(() -> {
            loadLocalContactNames(context);
            mainHandler.post(() -> startRealtime(userId));

            repo.getChats(userId, new MutableLiveData<List<Chat>>() {
                @Override
                public void postValue(List<Chat> list) {
                    if (list != null) {
                        backgroundExecutor.execute(() -> {
                            processChatList(list);
                            chats.postValue(list);
                        });
                    } else {
                        error.postValue("Failed to load chats");
                    }
                }
            });
        });
    }

    public void getOrCreateChat(String userId1, String userId2) {
        MutableLiveData<SupabaseRepository.ApiResult<Map<String, Object>>> tempResult = new MutableLiveData<>();
        Observer<SupabaseRepository.ApiResult<Map<String, Object>>> obs = new Observer<>() {
            @Override
            public void onChanged(SupabaseRepository.ApiResult<Map<String, Object>> api) {
                tempResult.removeObserver(this);
                if (api == null || !api.success) {
                    error.postValue("Failed to create chat");
                    return;
                }
                try {
                    String json = gson.toJson(api.body);
                    Chat chat = gson.fromJson(json, Chat.class);
                    newChat.postValue(chat);
                } catch (Exception e) {
                    error.postValue("Failed to parse chat");
                }
            }
        };
        tempResult.observeForever(obs);
        repo.rpcGetOrCreateChat(userId1, userId2, tempResult);
    }

    public void updateChatPreview(Message msg) {
        if (msg == null || msg.chatId == null) return;

        List<Chat> currentList = chats.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        List<Chat> newList = new ArrayList<>(currentList);
        int foundIndex = -1;
        Chat oldChat = null;

        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i).id != null && newList.get(i).id.equals(msg.chatId)) {
                foundIndex = i;
                oldChat = newList.get(i);
                break;
            }
        }

        Chat chatToUpdate = new Chat();

        if (foundIndex != -1) {
            chatToUpdate.id = oldChat.id;
            chatToUpdate.title = oldChat.title;
            chatToUpdate.partnerPhone = oldChat.partnerPhone;

            chatToUpdate.lastMessage = msg.text;
            chatToUpdate.updatedAt = msg.createdAt;

            newList.remove(foundIndex);
        } else {
            chatToUpdate.id = msg.chatId;
            chatToUpdate.lastMessage = msg.text;
            chatToUpdate.updatedAt = msg.createdAt;
            chatToUpdate.title = resolveContactName(msg.senderId, null);
        }

        newList.add(0, chatToUpdate);
        chats.postValue(newList);
    }

    private String resolveContactName(String senderId, String phone) {
        if (senderId == null) return phone;

        String norm = normalize(senderId);
        String name = localContactMap.get(norm);
        return name != null ? name : senderId;
    }

    private void processChatList(List<Chat> chatList) {
        for (Chat chat : chatList) {
            if (chat.lastMessage != null && !chat.lastMessage.equals("New Chat")) {
                try {
                    chat.lastMessage = EncryptionUtils.decrypt(chat.lastMessage, chat.id);
                } catch (Exception e) {
                    chat.lastMessage = "[Encrypted]";
                }
            }
            if (chat.partnerPhone != null && !localContactMap.isEmpty()) {
                String normalized = normalize(chat.partnerPhone);
                String name = localContactMap.get(normalized);
                if (name != null) chat.title = name;
            }
        }
    }

    private void loadLocalContactNames(Context context) {
        localContactMap.clear();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIdx);
                    String num = cursor.getString(numIdx);
                    if (num != null) localContactMap.put(normalize(num), name);
                }
            }
        } catch (Exception e) {
            Log.e("ChatListVM", "Contact read error", e);
        }
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }
}