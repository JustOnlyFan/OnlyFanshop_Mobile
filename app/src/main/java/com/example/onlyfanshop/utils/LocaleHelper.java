package com.example.onlyfanshop.utils; // Đặt trong package utils của bạn

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    private static final String MY_APP_PREFS = "MyAppPrefs";

    // Lấy ngôn ngữ đã lưu trữ (hoặc mặc định là "en")
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MY_APP_PREFS, Context.MODE_PRIVATE);
        // Mặc định là 'en' (English) hoặc ngôn ngữ bạn muốn
        return prefs.getString(SELECTED_LANGUAGE, "en");
    }

    // Lưu ngôn ngữ mới vào SharedPreferences
    public static void persist(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(MY_APP_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply();
    }

    // Setup new language for Context (from Android N onwards)
    @SuppressWarnings("deprecation")
    public static Context setLocale(Context context, String language) {
        persist(context, language);

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
        return context;
    }
}