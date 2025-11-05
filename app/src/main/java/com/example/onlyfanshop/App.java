package com.example.onlyfanshop;

import android.app.Application;

import com.example.onlyfanshop.service.FirebaseAuthManager;
import com.example.onlyfanshop.utils.LocaleUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
	private static final String TAG = "App";
	private ExecutorService backgroundExecutor;

	@Override
	public void onCreate() {
		super.onCreate();
		
		// Tác vụ nhẹ: set locale ngay (cần thiết cho UI)
		LocaleUtil.setAppLocaleVi(this);
		
		// Tác vụ nặng: di chuyển sang background thread
		backgroundExecutor = Executors.newSingleThreadExecutor();
		backgroundExecutor.execute(() -> {
			// Firebase initialization có thể tốn thời gian - chạy trên background
			FirebaseAuthManager.ensureSignedIn(this);
		});
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		// Giải phóng executor khi app terminate
		if (backgroundExecutor != null) {
			backgroundExecutor.shutdown();
		}
	}
}
