package com.example.onlyfanshop.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ui.chat.ChatRoomActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data payload: " + remoteMessage.getData());

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            if ("chat".equals(type)) {
                handleChatNotification(remoteMessage);
            }
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }

    private void handleChatNotification(RemoteMessage remoteMessage) {
        String roomId = remoteMessage.getData().get("roomId");
        String senderName = remoteMessage.getData().get("senderName");
        
        Log.d(TAG, "Chat notification - Room: " + roomId + ", Sender: " + senderName);
        
        // You can add additional logic here, such as:
        // - Updating local database
        // - Refreshing chat list
        // - Playing notification sound
    }

    private void showNotification(String title, String body, java.util.Map<String, String> data) {
        createNotificationChannel();

        // Create intent for notification click
        Intent intent = new Intent(this, ChatRoomActivity.class);
        String roomId = data.get("roomId");
        if (roomId != null) {
            intent.putExtra("roomId", roomId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chat Notifications";
            String description = "Notifications for chat messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Send token to server
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        // This should be implemented to send the token to your server
        // You can use your existing API service here
        Log.d(TAG, "Sending token to server: " + token);
        
        // Example implementation:
        // ChatService chatService = new ChatService(apiClient, appPreferences);
        // chatService.updateFCMToken(token, callback);
    }
}


