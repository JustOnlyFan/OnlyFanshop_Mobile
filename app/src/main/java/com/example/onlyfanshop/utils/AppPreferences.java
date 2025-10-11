package com.example.onlyfanshop.ultils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "OnlyFanshopPrefs";
    private static final String KEY_CART_COUNT = "cartCount";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setCartCount(Context context, int count) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(KEY_CART_COUNT, count);
        editor.apply();
    }

    public static int getCartCount(Context context) {
        return getPrefs(context).getInt(KEY_CART_COUNT, 0);
    }

    public static void clearCart(Context context) {
        getPrefs(context).edit().remove(KEY_CART_COUNT).apply();
    }
}
