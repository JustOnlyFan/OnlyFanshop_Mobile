package com.example.onlyfanshop.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Utility class để quản lý FCM Token
 * Lưu FCM token của user vào Firebase Database
 */
public class FCMTokenManager {
    
    private static final String TAG = "FCMTokenManager";
    
    /**
     * Lưu FCM token của user hiện tại vào Firebase Database
     * Gọi method này khi user đăng nhập thành công
     */
    public static void saveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    String userId = FirebaseAuth.getInstance().getUid();
                    
                    if (userId != null && token != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("fcmToken");
                            
                        userRef.setValue(token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "FCM token saved successfully for user: " + userId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save FCM token: " + e.getMessage());
                            });
                    } else {
                        Log.w(TAG, "User ID or FCM token is null");
                    }
                } else {
                    Log.e(TAG, "Failed to get FCM token: " + task.getException());
                }
            });
    }
    
    /**
     * Lưu FCM token cho user cụ thể
     * @param userId ID của user
     * @param token FCM token
     */
    public static void saveFCMTokenForUser(String userId, String token) {
        if (userId != null && token != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("fcmToken");
                
            userRef.setValue(token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved successfully for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save FCM token: " + e.getMessage());
                });
        }
    }
    
    /**
     * Lấy FCM token của user hiện tại
     */
    public static void getCurrentUserFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    Log.d(TAG, "Current FCM token: " + token);
                } else {
                    Log.e(TAG, "Failed to get FCM token: " + task.getException());
                }
            });
    }
}
