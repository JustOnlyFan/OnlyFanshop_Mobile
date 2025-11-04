package com.example.onlyfanshop;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.ui.SplashActivity;

/**
 * MainActivity chỉ là entry point, chuyển ngay sang SplashActivity
 * để tối ưu thời gian khởi động app
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chuyển ngay sang SplashActivity để xử lý logic khởi động
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }
}