package com.example.onlyfanshop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.activity.AdminActivity;
import com.example.onlyfanshop.activity.DashboardActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        String role = prefs.getString("role", null);

        // Nếu là admin thì vào AdminActivity, còn lại (kể cả không có token) thì vào DashboardActivity
        if ("ADMIN".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, AdminActivity.class));
        } else {
            startActivity(new Intent(this, DashboardActivity.class));
        }
        finish();
    }
}