package com.example.onlyfanshop;

import android.app.Application;

import com.example.onlyfanshop.service.FirebaseAuthManager;
import com.example.onlyfanshop.utils.LocaleUtil;

public class App extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		// Set default app locale to Vietnamese
		LocaleUtil.setAppLocaleVi(this);
		FirebaseAuthManager.ensureSignedIn(this);
	}
}
