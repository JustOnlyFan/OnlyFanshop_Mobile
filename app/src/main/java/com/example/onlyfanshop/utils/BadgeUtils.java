package com.example.onlyfanshop.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.CartDTO;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.leolin.shortcutbadger.ShortcutBadger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BadgeUtils {
    public void updateCartBadge(Context context, BottomNavigationView bottomNavigationView, int userId) {
        CartItemApi api = ApiClient.getPrivateClient(context).create(CartItemApi.class);
        Log.d("BadgeUtils", "Fetching cart for user ID: " + userId);
        api.getCart(userId).enqueue(new Callback<ApiResponse<CartDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<CartDTO>> call, Response<ApiResponse<CartDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalQuantity = response.body().getData().getTotalQuantity();
                    Log.d("BadgeUtils", "Total quantity: " + totalQuantity);

                    BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_car);
                    if (totalQuantity > 0) {
                        badge.setVisible(true);
                        badge.setNumber(totalQuantity);
                        com.example.onlyfanshop.utils.NotificationHelper.showNotification(
                                context,
                                "Cart",
                                "You have " + totalQuantity + " products in your cart!"
                        );
                        Log.d("BadgeUtils", "Updating badge with number: " + totalQuantity);
                    } else {
                        badge.clearNumber();
                        badge.setVisible(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CartDTO>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }


    // Clear badge when user checkout or cart is empty
    public static void clearBadge(Context context) {
        try {
            ShortcutBadger.removeCount(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clear badge when user logs out
    public void clearCartBadge(BottomNavigationView bottomNav) {
        bottomNav.removeBadge(R.id.nav_car); // hoặc id cart của bạn
    }
    public void updateNotificationBadge(Context context, BottomNavigationView bottomNavigationView, int userId) {
        com.example.onlyfanshop.api.NotificationApi api = ApiClient.getPrivateClient(context).create(com.example.onlyfanshop.api.NotificationApi.class);

        api.getUnreadCount(userId).enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int unreadCount = response.body().intValue();

                    BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.btnNotif);
                    if (unreadCount > 0) {
                        badge.setVisible(true);
                        badge.setNumber(unreadCount);
                        Log.d("BadgeUtils", "Unread notifications: " + unreadCount);
                    } else {
                        badge.clearNumber();
                        badge.setVisible(false);
                    }
                } else {
                    Log.w("BadgeUtils", "Could not load unread notification count");
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                    Log.e("BadgeUtils", "Error loading notification badge", t);
            }
        });
    }

}
