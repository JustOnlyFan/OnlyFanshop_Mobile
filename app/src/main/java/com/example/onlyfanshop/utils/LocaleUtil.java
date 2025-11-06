package com.example.onlyfanshop.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public final class LocaleUtil {
    private LocaleUtil() {}

    public static void setAppLocaleVi(Context context) {
        Locale locale = new Locale("vi");
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            // Deprecated on newer APIs but safe fallback
            config.locale = locale;
        }
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}








