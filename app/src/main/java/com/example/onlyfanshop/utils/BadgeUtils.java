package com.example.onlyfanshop.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.CartDTO;
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


    // Xóa badge khi người dùng checkout hoặc giỏ hàng rỗng
    public static void clearBadge(Context context) {
        try {
            ShortcutBadger.removeCount(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
