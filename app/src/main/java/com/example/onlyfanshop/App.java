package com.example.onlyfanshop;

import android.app.Application;

import com.example.onlyfanshop.service.FirebaseAuthManager;

public class App extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		FirebaseAuthManager.ensureSignedIn(this);
	}
}
