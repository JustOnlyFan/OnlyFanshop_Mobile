package com.example.onlyfanshop.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.AdminActivity;
import com.example.onlyfanshop.activity.DashboardActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "role";
    private static final long SPLASH_DELAY_MS = 1200L; // Hiển thị splash đủ lâu để người dùng thấy

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Di chuyển tác vụ nặng sang background thread
        backgroundExecutor.execute(() -> {
            // Đọc SharedPreferences trên background thread
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String token = prefs.getString(KEY_JWT_TOKEN, null);
            String role = prefs.getString(KEY_ROLE, null);

            // Chuyển về main thread để navigate
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navigateToNextScreen(role);
            }, SPLASH_DELAY_MS);
        });
    }

    private void navigateToNextScreen(String role) {
        Intent intent;
        if ("ADMIN".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        // Bỏ animation transition để chuyển mượt hơn
        startActivity(intent);
        finish();
        // Không dùng transition để tránh màn hình trung gian
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng executor
        backgroundExecutor.shutdown();
    }
}

