package com.example.onlyfanshop.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.onlyfanshop.ui.notification.NotificationListActivity;
import com.example.onlyfanshop.utils.NotificationHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationListenerService extends Service {

    private static final String TAG = "NotificationService";
    private DatabaseReference ref;
    private ChildEventListener listener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "NotificationListenerService started");

        // ✅ Lấy userId từ SharedPreferences (được lưu sau khi đăng nhập)
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Log.e(TAG, "UserID not found. Service stopping...");
            stopSelf();
            return START_NOT_STICKY;
        }

        // ✅ Đảm bảo Firebase được khởi tạo
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "FirebaseApp initialized manually in service");
        }

        // ✅ Kết nối tới node notifications/{userId}
        ref = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(String.valueOf(userId));
        Log.d(TAG, "Firebase reference path: notifications/" + userId);

        // ✅ Lắng nghe các thông báo mới
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded() triggered. Snapshot key: " + snapshot.getKey());

                String message = snapshot.child("message").getValue(String.class);
                Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);
                Long notificationId = snapshot.child("notificationID").getValue(Long.class);

                // 👉 Bỏ qua nếu null hoặc đã đọc
                if (message == null || (isRead != null && isRead)) {
                    Log.d(TAG, "Bỏ qua thông báo cũ hoặc không hợp lệ");
                    return;
                }

                Log.d(TAG, "New notification received: " + message);

                // 👉 Intent mở NotificationListActivity
                Intent openIntent = new Intent(getApplicationContext(), NotificationListActivity.class);
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // ✅ Hiển thị notification
                NotificationHelper.showOrderNotification(
                        getApplicationContext(),
                        "Thông báo mới",
                        message,
                        openIntent
                );

                // ✅ Cập nhật isRead = true
                snapshot.getRef().child("isRead").setValue(true)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Đã cập nhật isRead=true cho notification: " + snapshot.getKey()))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Lỗi khi cập nhật isRead: ", e));
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled: " + error.getMessage());
            }
        };

        ref.addChildEventListener(listener);
        return START_STICKY; // Giữ service chạy nền
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationListenerService stopped");
        if (ref != null && listener != null) {
            ref.removeEventListener(listener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Không dùng binding
    }
}
