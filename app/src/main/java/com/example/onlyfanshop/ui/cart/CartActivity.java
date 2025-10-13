package com.example.onlyfanshop.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.databinding.ActivityCartBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.payment.ConfirmPaymentActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {
    private CartAdapter cartAdapter;
    private ActivityCartBinding binding;
    private List<CartItemDTO> cartItems;
    private double totalPrice=0;
    private String USERNAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.rclViewCart.setLayoutManager(new LinearLayoutManager(this));
        USERNAME = getIntent().getStringExtra("username");
        cartAdapter = new CartAdapter(this, cartItems, true);
        binding.rclViewCart.setAdapter(cartAdapter);
        cartAdapter.setOnQuantityChangeListener(new CartAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(int productId) {
                addQuantity(USERNAME, productId, cartAdapter);
            }
            @Override
            public void onDecrease(int productId) {
                minusQuantity(USERNAME, productId, cartAdapter);
            }
        });
        getCartItems(USERNAME, cartAdapter);
        binding.checkoutBtn.setOnClickListener(v->confirmCheckout());
        binding.backBtn.setOnClickListener(v -> finish());

    }

    private void confirmCheckout() {
        Intent intent = new Intent(CartActivity.this, ConfirmPaymentActivity.class);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("cartItems",(Serializable) cartItems);
        startActivity(intent);
    }

    private void getCartItems(String username, CartAdapter cartAdapter) {

        CartItemApi cartItemApi = ApiClient.getPrivateClient(this).create(CartItemApi.class);
        cartItemApi.getCartItem(username).enqueue(new Callback<>() {

            @Override
            public void onResponse(Call<ApiResponse<List<CartItemDTO>>> call, Response<ApiResponse<List<CartItemDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CartItemDTO> list = response.body().getData();
                    //Log.d("ProductDetail", "Product data: " + d);
                    if (list == null) {

                        Toast.makeText(CartActivity.this, "Không có dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
                    } else {
                         totalPrice =0;
                        cartItems = new ArrayList<>();
                        cartItems.addAll(list);
                        cartAdapter.setData(cartItems);
                        if(cartAdapter.getItemCount()==0){
                            binding.textEmpty.setVisibility(View.VISIBLE);
                            binding.checkoutBtn.setVisibility(View.GONE);
                        }
                        for (CartItemDTO item : cartItems) {
                            totalPrice += item.getPrice();
                        }
                        binding.productTotal.setText(totalPrice + " VND");
                        binding.totalPrice.setText(totalPrice + " VND");
                        }

                } else {
                    Log.e("CartItem", "Response not successful or body is null");
                    Toast.makeText(CartActivity.this, "Failed to load cart items ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartItemDTO>>> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }
    private void addQuantity(String username, int productId, CartAdapter cartAdapter) {
        CartItemApi api = ApiClient.getPrivateClient(this).create(CartItemApi.class);
        api.addQuantity(username, productId).enqueue(new Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    getCartItems(USERNAME, cartAdapter);
                    Toast.makeText(CartActivity.this, "Tăng số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CartActivity.this, "Tăng số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }

        });
    }

    private void minusQuantity(String username, int productId, CartAdapter cartAdapter) {
        CartItemApi api = ApiClient.getPrivateClient(this).create(CartItemApi.class);
        api.minusQuantity(username, productId).enqueue(new Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    getCartItems(USERNAME, cartAdapter);
                    Toast.makeText(CartActivity.this, "Giảm số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CartActivity.this, "Giảm số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }



        });
    }
}