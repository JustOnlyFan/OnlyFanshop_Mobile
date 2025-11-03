package com.example.onlyfanshop.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.onlyfanshop.R;

public class NotificationHelper {

    private static final String CART_CHANNEL_ID = "cart_channel";
    private static final String ORDER_CHANNEL_ID = "order_channel";

    static final int CART_NOTIFICATION_ID = 1001;
    private static final int ORDER_NOTIFICATION_BASE_ID = 2000;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    // ✅ Kiểm tra quyền thông báo
    private static boolean hasPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ✅ Xin quyền thông báo (chỉ dùng trong Activity)
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    /** 🔔 Giỏ hàng (ghi đè notification cũ nếu có) */
    public static void showCartNotification(Context context, String title, String message) {
        if (context == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission(context)) {
            Log.w("NotificationHelper", "Permission POST_NOTIFICATIONS not granted, skipping cart notification.");
            return;
        }

        createChannel(context, CART_CHANNEL_ID, "Cart Notifications", "Thông báo giỏ hàng");

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CART_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setSilent(true);

            NotificationManagerCompat.from(context).notify(CART_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "SecurityException when showing cart notification", e);
        }
    }

    /** ❌ Xóa thông báo giỏ hàng */
    public static void clearCartNotification(Context context) {
        if (context == null || !hasPermission(context)) return;

        try {
            NotificationManagerCompat.from(context).cancel(CART_NOTIFICATION_ID);
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "SecurityException when clearing cart notification", e);
        }
    }

    /** 📦 Đơn hàng (mỗi order hiển thị riêng biệt) */
    public static void showOrderNotification(Context context, String title, String message, Intent intent) {
        if (context == null) return;

        // 🔒 Kiểm tra quyền thông báo (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationHelper", "Permission POST_NOTIFICATIONS not granted, skipping order notification.");
            return;
        }

        // 🪣 Tạo channel nếu chưa có
        createChannel(context, ORDER_CHANNEL_ID, "Order Notifications", "Thông báo đơn hàng");

        // 🎯 PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // Không cần orderId nữa
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            // 🛎️ Tạo notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // 🚀 Hiển thị notification
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify((int) System.currentTimeMillis(), builder.build()); // dùng time làm ID để tránh trùng
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "SecurityException when showing order notification", e);
        }
    }


    /** 🔧 Tạo Notification Channel nếu chưa có */
    private static void createChannel(Context context, String id, String name, String desc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(id) == null) {
                NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(desc);
                channel.setSound(null, null);
                manager.createNotificationChannel(channel);
            }
        }
    }
}
