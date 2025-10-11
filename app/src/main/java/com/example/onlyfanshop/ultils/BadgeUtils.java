package com.example.onlyfanshop.ultils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import me.leolin.shortcutbadger.ShortcutBadger;

public class BadgeUtils {
    @SuppressLint("SuspiciousIndentation")
    public static void updateCartBadge(Context context, int cartCount) {
        try {
            Log.d("BadgeUtils", "Updating cart badge with count:"+ cartCount);
            if (cartCount > 0)
            ShortcutBadger.applyCount(context, cartCount); // Hiển thị số trên icon
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Xóa badge khi người dùng checkout hoặc giỏ hàng rỗng
    public static void clearBadge(Context context) {
        try {
            ShortcutBadger.removeCount(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
