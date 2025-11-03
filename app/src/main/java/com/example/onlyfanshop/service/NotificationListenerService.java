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

/**
 * Service này sẽ lắng nghe thông báo realtime từ Firebase Realtime Database
 * và hiển thị Notification cho người dùng theo userId.
 */
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
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "FirebaseApp initialized manually in service");
        }

        // ✅ Kết nối Firebase
        ref = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(String.valueOf(userId));
        Log.d(TAG, "Firebase reference path: notifications/" + userId);

        // ✅ Lắng nghe thay đổi realtime
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded() triggered. Snapshot key: " + snapshot.getKey());
                String message = snapshot.child("message").getValue(String.class);

                if (message != null) {
                    Log.d(TAG, "New notification received: " + message);

                    // 👉 Intent mở NotificationListActivity
                    Intent intent = new Intent(getApplicationContext(), NotificationListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    NotificationHelper.showOrderNotification(
                            getApplicationContext(),
                            "Thông báo mới",
                            message,
                            intent
                    );
                }
            }


            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled: " + error.getMessage());
            }
        };

        ref.addChildEventListener(listener);

        // Service sẽ tiếp tục chạy cho đến khi bị dừng thủ công
        return START_STICKY;
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
