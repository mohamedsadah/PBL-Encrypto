package com.example.encrypto.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.encrypto.Constants;
import com.example.encrypto.model.Message;
import com.example.encrypto.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SupabaseRealtimeClient {

    private static final String TAG = "SupabaseRealtimeClient";
    private static SupabaseRealtimeClient instance;

    private WebSocket socket;
    private final List<MessageListener> listeners = new ArrayList<>();

    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int refCounter = 1;
    private boolean connected = false;
    private boolean manualClose = false;
    private int reconnectAttempts = 0;

    private String activeChatId = null;
    private String activeTopic = null;
    private String joinRef = null;
    private boolean channelJoined = false;
    private String pendingChatId = null;
    private boolean hasPendingSubscription = false;

    // Heartbeat
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable;
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 seconds

    private final OkHttpClient httpClient;
    private final String wsUrl;

    private SupabaseRealtimeClient() {
        String host = Constants.SUPABASE_URL.replaceFirst("^https?://", "");

        this.wsUrl = "wss://" + host +
                "/realtime/v1/websocket" +
                "?apikey=" + Constants.SUPABASE_ANON_KEY +
                "&vsn=1.0.0";

        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseRealtimeClient getInstance() {
        if (instance == null) instance = new SupabaseRealtimeClient();
        return instance;
    }

    // CALLBACK INTERFACE
    public interface MessageListener {
        void onNewMessage(Message message);
        void onOpen();
        void onClose(int code, String reason);
        void onError(Throwable t);
        void onPhxReply(Envelope env);
        void onPostgresChange(Envelope env);
    }

    public void addListener(MessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Added listener, total: " + listeners.size());
        }
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "Removed listener, remaining: " + listeners.size());
    }

    @Deprecated
    public void setListener(MessageListener listener) {
        // for backwards compatibility but we are using addListener
        addListener(listener);
    }

    public boolean isConnected() { return connected; }

    public String getCurrentSubscribedChatId() {
        return activeChatId;
    }

    public boolean isSubscribedToAllMessages() {
        return connected && channelJoined && activeChatId == null;
    }

    // CONNECT
    public void connect() {
        manualClose = false;

        String token = null;
        try { token = TokenManager.getAccessToken(); } catch (Exception ignored) {}

        Headers.Builder headers = new Headers.Builder();
        headers.add("apikey", Constants.SUPABASE_ANON_KEY);

        if (token != null)
            headers.add("Authorization", "Bearer " + token);

        Request req = new Request.Builder()
                .url(wsUrl)
                .headers(headers.build())
                .build();

        socket = httpClient.newWebSocket(req, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                connected = true;
                reconnectAttempts = 0;
                Log.i(TAG, "WS OPEN");

                // Start heartbeat
                startHeartbeat();

                // Notify all listeners
                for (MessageListener listener : new ArrayList<>(listeners)) {
                    main.post(listener::onOpen);
                }

                // Subscribe to pending chat if any
                if (pendingChatId != null) {
                    String chatToSubscribe = pendingChatId;
                    pendingChatId = null;
                    main.post(() -> subscribeToChat(chatToSubscribe));
                }
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleIncoming(text);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                connected = false;
                channelJoined = false;
                stopHeartbeat();

                String codeDesc = getCloseCodeDescription(code);
                Log.w(TAG, "WS CLOSED - Code: " + code + " (" + codeDesc + "), Reason: '" + reason + "'");
                Log.w(TAG, "Was manually closed: " + manualClose + ", Channel was joined: " + channelJoined);

                // Notify all listeners
                for (MessageListener listener : new ArrayList<>(listeners)) {
                    main.post(() -> listener.onClose(code, reason));
                }

                if (!manualClose)
                    scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, @Nullable Response response) {
                connected = false;
                channelJoined = false;
                stopHeartbeat();
                Log.e(TAG, "WS FAILURE", t);

                // Notify all listeners
                for (MessageListener listener : new ArrayList<>(listeners)) {
                    main.post(() -> listener.onError(t));
                }

                if (!manualClose)
                    scheduleReconnect();
            }
        });
    }

    public void disconnect() {
        manualClose = true;
        stopHeartbeat();

        if (socket != null)
            socket.close(1000, "manual close");

        socket = null;
        channelJoined = false;
    }

    // HEARTBEAT
    private void startHeartbeat() {
        stopHeartbeat(); // Clear any existing

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (connected && socket != null) {
                    Envelope heartbeat = new Envelope();
                    heartbeat.topic = "phoenix";
                    heartbeat.event = "heartbeat";
                    heartbeat.payload = new HashMap<>();
                    heartbeat.ref = nextRef();
                    send(heartbeat);

                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };

        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    private void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= 5) return;

        reconnectAttempts++;

        long delay = Math.min(
                (long) Math.pow(2, reconnectAttempts) * 1000,
                15000
        );

        Log.i(TAG, "Reconnecting in " + delay + "ms (attempt " + reconnectAttempts + ")");
        main.postDelayed(this::connect, delay);
    }

    // SUBSCRIBE TO CHAT
    public void subscribeToChat(String chatId) {
        Log.d(TAG, "subscribeToChat called for: " + chatId + ", connected: " + connected);

        // If not connected yet, store for later
        if (!connected) {
            pendingChatId = chatId;
            hasPendingSubscription = true; // Mark that we have a pending subscription
            Log.i(TAG, "Not connected yet, will subscribe after connection opens");
            return;
        }

        // Check if already subscribed to the same thing
        boolean sameSubscription = (chatId == null && activeChatId == null) ||
                (chatId != null && chatId.equals(activeChatId));

        if (sameSubscription && channelJoined) {
            Log.i(TAG, "Already subscribed to: " + (chatId != null ? chatId : "ALL messages"));
            return;
        }

        // Unsubscribe from previous subscription if needed
        if (activeTopic != null) {
            Log.i(TAG, "Switching subscription from " +
                    (activeChatId != null ? activeChatId : "ALL") +
                    " to " + (chatId != null ? chatId : "ALL"));
            unsubscribeFromChat();
        }

        this.activeChatId = chatId;
        this.activeTopic = "realtime:public:messages";
        this.channelJoined = false;

        // JOIN CHANNEL
        joinRef = nextRef();

        Envelope join = new Envelope();
        join.topic = activeTopic;
        join.event = "phx_join";
        join.payload = new HashMap<>();

        Map<String, Object> config = new HashMap<>();
        config.put("broadcast", new HashMap<>());
        config.put("presence", new HashMap<>());

        Map<String, Object> postgresChanges = new HashMap<>();
        postgresChanges.put("event", "INSERT");
        postgresChanges.put("schema", "public");
        postgresChanges.put("table", "messages");

        // Only add filter if chatId is not null
        if (chatId != null) {
            postgresChanges.put("filter", "chat_id=eq." + chatId);
            Log.i(TAG, "Subscribing to specific chat: " + chatId);
        } else {
            Log.i(TAG, "Subscribing to ALL messages (no filter)");
        }

        config.put("postgres_changes", new Object[]{postgresChanges});
        join.payload.put("config", config);

        join.ref = joinRef;
        join.joinRef = joinRef;

        send(join);
    }

    public void unsubscribeFromChat() {
        if (activeTopic == null) return;

        Envelope leave = new Envelope();
        leave.topic = activeTopic;
        leave.event = "phx_leave";
        leave.payload = new HashMap<>();
        leave.ref = nextRef();
        leave.joinRef = joinRef;
        send(leave);

        activeTopic = null;
        activeChatId = null;
        channelJoined = false;
    }

    // INCOMING MESSAGE HANDLER
    public void handleIncoming(String json) {
        Log.d(TAG, "============================================");
        Log.d(TAG, "WS INCOMING: " + json);
        Log.d(TAG, "============================================");

        try {
            Envelope env = gson.fromJson(json, Envelope.class);

            if (env == null) {
                Log.e(TAG, "ERROR: Envelope is null after parsing");
                return;
            }

            if (env.event == null) {
                Log.e(TAG, "ERROR: Event is null in envelope");
                return;
            }

            Log.d(TAG, "Event Type: " + env.event);
            Log.d(TAG, "Topic: " + env.topic);
            Log.d(TAG, "Has Payload: " + (env.payload != null));

            switch (env.event) {
                case "phx_reply":
                    Log.d(TAG, "HANDLING: phx_reply");
                    handlePhxReply(env);
                    for (MessageListener l : new ArrayList<>(listeners)) {
                        main.post(() -> l.onPhxReply(env));
                    }
                    break;

                case "postgres_changes":
                    Log.d(TAG, "HANDLING: postgres_changes");
                    handlePostgresChange(env);
                    break;

                case "INSERT":
                    Log.d(TAG, "HANDLING: INSERT");
                    handlePostgresChange(env);
                    break;

                case "UPDATE":
                    Log.d(TAG, "HANDLING: UPDATE");
                    handlePostgresChange(env);
                    break;

                case "DELETE":
                    Log.d(TAG, "HANDLING: DELETE");
                    handlePostgresChange(env);
                    break;

                default:
                    Log.e(TAG, "UNKNOWN EVENT TYPE: " + env.event);
                    Log.e(TAG, "Full envelope: " + gson.toJson(env));
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION in handleIncoming", e);
            e.printStackTrace();
        }
    }

    private void handlePhxReply(Envelope env) {
        if (env.payload == null) {
            Log.w(TAG, "phx_reply with null payload - topic: " + env.topic + " ref: " + env.ref);
            return;
        }

        Object statusObj = env.payload.get("status");
        String status = statusObj != null ? statusObj.toString() : null;

        Log.d(TAG, "PHX_REPLY - Topic: " + env.topic +
                ", Status: " + status +
                ", Ref: " + env.ref +
                ", JoinRef: " + env.joinRef +
                ", Full payload: " + gson.toJson(env.payload));

        if ("ok".equals(status)) {
            if (activeTopic != null && activeTopic.equals(env.topic)) {
                channelJoined = true;
                Log.i(TAG, "Channel joined successfully: " + env.topic);
            }
        } else if ("error".equals(status)) {
            Object response = env.payload.get("response");
            Log.e(TAG, "Phoenix reply ERROR: " + gson.toJson(response));

            // Log specific error details
            if (response instanceof Map) {
                Map<String, Object> respMap = (Map<String, Object>) response;
                Log.e(TAG, "Error reason: " + respMap.get("reason"));
            }
        } else {
            Log.w(TAG, "Unknown phx_reply status: " + status);
        }
    }

    private void handlePostgresChange(Envelope env) {
        Log.d(TAG, "============================================");
        Log.d(TAG, "handlePostgresChange CALLED");
        Log.d(TAG, "Listener count: " + listeners.size());
        Log.d(TAG, "============================================");

        if (env.payload == null) {
            Log.e(TAG, "ERROR: Payload is null");
            return;
        }

        Log.d(TAG, "Payload keys: " + env.payload.keySet());
        Log.d(TAG, "Full payload: " + gson.toJson(env.payload));

        // Notify listeners about postgres change
        Log.d(TAG, "Notifying " + listeners.size() + " listeners of postgres change");
        for (MessageListener listener : new ArrayList<>(listeners)) {
            String listenerName = listener.getClass().getName();
            Log.d(TAG, "Notifying listener: " + listenerName);
            main.post(() -> {
                Log.d(TAG, "POSTING to listener: " + listenerName);
                listener.onPostgresChange(env);
            });
        }

        Map<String, Object> rec = null;

        // Try payload.record
        Object recordObj = env.payload.get("record");
        if (recordObj instanceof Map) {
            rec = (Map<String, Object>) recordObj;
            Log.d(TAG, "Found record at payload.record");
        } else {
            Log.d(TAG, "No record at payload.record");
        }

        // Try payload.data.record
        if (rec == null) {
            Object dataObj = env.payload.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object nestedRecord = data.get("record");
                if (nestedRecord instanceof Map) {
                    rec = (Map<String, Object>) nestedRecord;
                    Log.d(TAG, "Found record at payload.data.record");
                }
            }
        }

        if (rec == null) {
            Log.e(TAG, "ERROR: Could not find record in payload");
            return;
        }

        try {
            Log.d(TAG, "Parsing message from record");
            Message msg = gson.fromJson(gson.toJson(rec), Message.class);

            if (msg == null) {
                Log.e(TAG, "ERROR: Parsed message is null");
                return;
            }

            Log.i(TAG, "SUCCESS: Message parsed");
            Log.i(TAG, "Message ID: " + msg.id);
            Log.i(TAG, "Chat ID: " + msg.chatId);
            Log.i(TAG, "Sender ID: " + msg.senderId);

            // Notify listeners about new message
            Log.d(TAG, "Notifying " + listeners.size() + " listeners of new message");
            for (MessageListener listener : new ArrayList<>(listeners)) {
                String listenerName = listener.getClass().getName();
                Log.d(TAG, "Calling onNewMessage for: " + listenerName);
                main.post(() -> {
                    Log.d(TAG, "EXECUTING onNewMessage for: " + listenerName);
                    listener.onNewMessage(msg);
                });
            }


        } catch (Exception e) {
            Log.e(TAG, "ERROR: Failed to parse message", e);
            e.printStackTrace();
        }
    }

    private String nextRef() {
        return String.valueOf(refCounter++);
    }

    private String getCloseCodeDescription(int code) {
        switch(code) {
            case 1000: return "Normal Closure";
            case 1001: return "Going Away";
            case 1002: return "Protocol Error";
            case 1003: return "Unsupported Data";
            case 1006: return "Abnormal Closure";
            case 1007: return "Invalid frame payload data";
            case 1008: return "Policy Violation";
            case 1009: return "Message Too Big";
            case 1011: return "Internal Server Error";
            case 1015: return "TLS Handshake Failed";
            default: return "Unknown";
        }
    }

    private void send(Envelope env) {
        if (socket == null || !connected) {
            Log.w(TAG, "Cannot send - socket not connected");
            return;
        }

        String json = gson.toJson(env);
        Log.d(TAG, "WS >> " + json);
        socket.send(json);
    }

    // ENVELOPE
    public static class Envelope {
        public String topic;
        public String event;
        public Map<String, Object> payload;
        @SerializedName("ref") public String ref;
        @SerializedName("join_ref") public String joinRef;
    }
}