package com.example.encrypto.data;

import android.util.Log;

import com.example.encrypto.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized Realtime Manager
 * Manages a single WebSocket connection and distributes messages to multiple listeners
 */
public class RealtimeManager {

    private static final String TAG = "RealtimeManager";
    private static RealtimeManager instance;

    private SupabaseRealtimeClient client;
    private final List<MessageCallback> messageCallbacks = new ArrayList<>();
    private String currentSubscribedChatId = null;
    private boolean isInitialized = false;

    public interface MessageCallback {
        void onMessageReceived(Message message);
        default void onConnectionStateChanged(boolean connected) {}
        default void onSubscriptionChanged(String chatId) {}
    }

    private RealtimeManager() {
        client = SupabaseRealtimeClient.getInstance();
    }

    public static synchronized RealtimeManager getInstance() {
        if (instance == null) {
            instance = new RealtimeManager();
        }
        return instance;
    }

    /**
     * Initialize the WebSocket connection (call once from Application or first Activity)
     */
    public void initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized");
            return;
        }

        Log.i(TAG, "🚀 Initializing RealtimeManager");

        client.addListener(new SupabaseRealtimeClient.MessageListener() {
            @Override
            public void onOpen() {
                Log.i(TAG, "✅ WebSocket connected");
                notifyConnectionState(true);
            }

            @Override
            public void onClose(int code, String reason) {
                Log.w(TAG, "❌ WebSocket closed: " + code + " - " + reason);
                notifyConnectionState(false);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "⚠️ WebSocket error", t);
                notifyConnectionState(false);
            }

            @Override
            public void onPhxReply(SupabaseRealtimeClient.Envelope env) {
                Log.d(TAG, "📨 Reply: " + env.topic);
            }

            @Override
            public void onPostgresChange(SupabaseRealtimeClient.Envelope env) {
                Log.d(TAG, "🔔 Change detected");
            }

            @Override
            public void onNewMessage(Message message) {
                if (message != null) {
                    Log.i(TAG, "📬 New message - ChatId: " + message.chatId +
                            ", Sender: " + message.senderId);
                    notifyMessageReceived(message);
                }
            }
        });

        if (!client.isConnected()) {
            Log.d(TAG, "Connecting WebSocket...");
            client.connect();
        }

        isInitialized = true;
    }


    // =============== Private notification methods ===============

    private void notifyMessageReceived(Message message) {
        List<MessageCallback> callbacksCopy = new ArrayList<>(messageCallbacks);
        for (MessageCallback callback : callbacksCopy) {
            try {
                callback.onMessageReceived(message);
            } catch (Exception e) {
                Log.e(TAG, "Error in message callback", e);
            }
        }
    }

    private void notifyConnectionState(boolean connected) {
        List<MessageCallback> callbacksCopy = new ArrayList<>(messageCallbacks);
        for (MessageCallback callback : callbacksCopy) {
            try {
                callback.onConnectionStateChanged(connected);
            } catch (Exception e) {
                Log.e(TAG, "Error in connection callback", e);
            }
        }
    }
}