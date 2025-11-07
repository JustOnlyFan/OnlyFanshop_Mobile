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
		
		// Tối ưu: set locale ngay (cần thiết cho UI)
		LocaleUtil.setAppLocaleVi(this);
		
		// Tối ưu: dùng thread pool thay vì single thread để xử lý nhiều tasks song song
		backgroundExecutor = Executors.newFixedThreadPool(2);
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

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		// Clear Glide cache khi memory thấp
		com.bumptech.glide.Glide.get(this).clearMemory();
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		// Trim Glide cache dựa trên memory pressure
		if (level >= TRIM_MEMORY_MODERATE) {
			com.bumptech.glide.Glide.get(this).clearMemory();
		}
	}
}
