package com.example.encrypto.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.encrypto.R;
import com.example.encrypto.model.Message;
import com.example.encrypto.ui.chat.ChatActivity;
import com.example.encrypto.ui.chatlist.ChatListActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_MESSAGES = "channel_messages";
    public static final String CHANNEL_ID_STATUS = "channel_status";

    private static final String CHANNEL_NAME_MESSAGES = "Messages";
    private static final String CHANNEL_NAME_STATUS = "Status Updates";

    // Create channels (Call this from SplashActivity)
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // 1. Message Channel (High Importance)
            NotificationChannel msgChannel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    CHANNEL_NAME_MESSAGES,
                    NotificationManager.IMPORTANCE_HIGH
            );
            msgChannel.setDescription("Notifications for new chat messages");
            msgChannel.enableVibration(true);

            // 2. Status Channel (Low Importance)
            NotificationChannel statusChannel = new NotificationChannel(
                    CHANNEL_ID_STATUS,
                    CHANNEL_NAME_STATUS,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            statusChannel.setDescription("Notifications for contact status updates");

            manager.createNotificationChannel(msgChannel);
            manager.createNotificationChannel(statusChannel);
        }
    }

    public static void showMessageNotification(Context context, Message message, String senderName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // Intent to open ChatActivity when clicked
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", message.chatId);
        intent.putExtra("chatTitle", senderName);
        intent.putExtra("currentUserId", message.senderId); // Note: Logic might need actual current user ID
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                message.chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Decrypt preview if needed (assuming text might still be encrypted if not handled by VM)
        String contentText = message.text;
        try {
            // Quick check if it looks encrypted (Base64 length/chars), usually VM handles this
            if (!contentText.contains(" ")) contentText = EncryptionUtils.decrypt(message.text, message.chatId);
        } catch (Exception ignored) {}

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(senderName)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(message.chatId.hashCode(), builder.build());
    }

    // Placeholder for Status Notifications
    public static void showStatusNotification(Context context, String userName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }

        Intent intent = new Intent(context, ChatListActivity.class); // Open main app
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_STATUS)
                .setSmallIcon(R.drawable.ic_status) // Ensure this drawable exists
                .setContentTitle("New Status")
                .setContentText(userName + " added a new status update")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(userName.hashCode(), builder.build());
    }
}