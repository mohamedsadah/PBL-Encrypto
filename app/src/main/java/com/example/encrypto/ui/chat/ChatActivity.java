package com.example.encrypto.ui.chat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.encrypto.R;
import com.example.encrypto.model.Message;
import com.example.encrypto.utils.EncryptionUtils;
import com.example.encrypto.utils.NotificationHelper;
import com.example.encrypto.viewmodel.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Map;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private ChatViewModel vm;
    private RecyclerView rv;
    private MessageAdapter adapter;
    private EditText input;
    private ImageButton sendBtn;
    private ProgressBar progress;
    private MaterialToolbar toolbar;
    private TextView tvTitle;

    private String chatId;
    private String currentUserId;
    private String optimisticMessageId = null;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rv = findViewById(R.id.rvMessages);
        input = findViewById(R.id.etMessage);
        sendBtn = findViewById(R.id.btnSend);
        progress = findViewById(R.id.progress);
        toolbar = findViewById(R.id.chatToolbar);
        tvTitle = findViewById(R.id.tvChatTitle);

        chatId = getIntent().getStringExtra("chatId");
        String title = getIntent().getStringExtra("chatTitle");
        currentUserId = getIntent().getStringExtra("currentUserId");

        if (currentUserId == null) {
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            currentUserId = prefs.getString("auth_user_id", null);
        }

        tvTitle.setText(title != null ? title : "Chat");

        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                finish();
            }
        });

        adapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        vm.getNewMessage().observe(this, message -> {
            if (message == null) return;
            if (!message.senderId.equals(currentUserId)) {
                adapter.autoAppend(message);
                handler.postDelayed(() -> rv.smoothScrollToPosition(adapter.getItemCount() - 1), 100);
            }

        });

        vm.getMessages().observe(this, messages -> {
            progress.setVisibility(View.GONE);
            if (messages != null) {
                adapter.submitList(messages);
                if (adapter.getItemCount() > 0) rv.scrollToPosition(adapter.getItemCount() - 1);
            }
        });

        vm.getSendMessageResult().observe(this, apiResult -> {
            if (apiResult == null) return;
            if (apiResult.success) {
                if (optimisticMessageId != null) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) apiResult.body;
                        Message realMessage = new Message();
                        realMessage.id = String.valueOf(map.get("id"));
                        realMessage.chatId = String.valueOf(map.get("chat_id"));
                        realMessage.senderId = String.valueOf(map.get("sender_id"));
                        realMessage.text = String.valueOf(map.get("content"));
                        realMessage.createdAt = String.valueOf(map.get("created_at"));

                        try {
                            realMessage.text = EncryptionUtils.decrypt(realMessage.text, chatId);
                        } catch (Exception e) {
                            Log.e(TAG, "Response decryption failed");
                        }

                        adapter.replaceOptimistic(optimisticMessageId, realMessage);
                        optimisticMessageId = null;
                    } catch (Exception e) {
                        Log.w(TAG, "Optimistic update failed", e);
                    }
                }
            } else {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
                if (optimisticMessageId != null) {
                    adapter.removeOptimistic(optimisticMessageId);
                    optimisticMessageId = null;
                }
            }
        });

        progress.setVisibility(View.VISIBLE);
        vm.loadMessages(chatId);

        sendBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty() || currentUserId == null) return;

            optimisticMessageId = UUID.randomUUID().toString();
            String encryptedPayload;
            try {
                encryptedPayload = EncryptionUtils.encrypt(text, chatId);
            } catch (Exception e) {
                Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Message optimistic = new Message();
            optimistic.id = optimisticMessageId;
            optimistic.chatId = chatId;
            optimistic.senderId = currentUserId;
            optimistic.text = text;
            optimistic.createdAt = String.valueOf(System.currentTimeMillis());

            input.setText("");
            adapter.autoAppend(optimistic);
            handler.postDelayed(() -> rv.smoothScrollToPosition(adapter.getItemCount() - 1), 950);

            vm.sendMessage(chatId, currentUserId, encryptedPayload);
        });
    }
}