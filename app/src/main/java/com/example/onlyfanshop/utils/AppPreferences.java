package com.example.onlyfanshop.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "OnlyFanshopPrefs";
    private static final String KEY_CART_COUNT = "cartCount";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ROLE = "userRole";

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

    // User ID methods
    public static void setUserId(Context context, String userId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public static String getUserId(Context context) {
        // First try to get from our custom preferences
        String userId = getPrefs(context).getString(KEY_USER_ID, null);
        if (userId != null) {
            return userId;
        }
        
        // If not found, try to get from MyAppPrefs (from LoginActivity)
        SharedPreferences myAppPrefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        int userIdInt = myAppPrefs.getInt("userId", -1);
        if (userIdInt != -1) {
            return String.valueOf(userIdInt);
        }
        
        return null;
    }

    // Token methods
    public static void setToken(Context context, String token) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    // User Role methods
    public static void setUserRole(Context context, String userRole) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_USER_ROLE, userRole);
        editor.apply();
    }

    public static String getUserRole(Context context) {
        // First try to get from our custom preferences
        String userRole = getPrefs(context).getString(KEY_USER_ROLE, null);
        if (userRole != null) {
            return userRole;
        }
        
        // If not found, try to get from MyAppPrefs (from LoginActivity)
        SharedPreferences myAppPrefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String role = myAppPrefs.getString("role", null);
        if (role != null) {
            return role;
        }
        
        return null;
    }

    // Username methods
    public static void setUsername(Context context, String username) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString("username", username);
        editor.apply();
    }

    public static String getUsername(Context context) {
        // First try to get from our custom preferences
        String username = getPrefs(context).getString("username", null);
        if (username != null) {
            return username;
        }
        
        // If not found, try to get from MyAppPrefs (from LoginActivity)
        SharedPreferences myAppPrefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String usernameFromMyApp = myAppPrefs.getString("username", null);
        if (usernameFromMyApp != null) {
            return usernameFromMyApp;
        }
        
        return "User"; // Default fallback
    }

    public static void clearUser(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER_ROLE);
        editor.remove("username");
        editor.apply();
    }
}
