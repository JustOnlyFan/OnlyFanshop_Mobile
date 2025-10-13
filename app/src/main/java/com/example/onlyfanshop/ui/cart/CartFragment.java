package com.example.onlyfanshop.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.databinding.FragmentCartBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.payment.ConfirmPaymentActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItemDTO> cartItems = new ArrayList<>();
    private double totalPrice = 0;
    private String USERNAME;

    public static CartFragment newInstance(String username) {
        CartFragment fragment = new CartFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            USERNAME = getArguments().getString("username");
        }

        binding.rclViewCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(requireContext(), cartItems, true);
        binding.rclViewCart.setAdapter(cartAdapter);

        cartAdapter.setOnQuantityChangeListener(new CartAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(int productId) {
                addQuantity(USERNAME, productId);
            }

            @Override
            public void onDecrease(int productId) {
                minusQuantity(USERNAME, productId);
            }
        });

        getCartItems(USERNAME);

        binding.checkoutBtn.setOnClickListener(v -> confirmCheckout());
    }

    private void confirmCheckout() {
        Intent intent = new Intent(requireContext(), ConfirmPaymentActivity.class);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("cartItems", (Serializable) cartItems);
        startActivity(intent);
    }

    private void getCartItems(String username) {
        CartItemApi cartItemApi = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        cartItemApi.getCartItem(username).enqueue(new Callback<ApiResponse<List<CartItemDTO>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CartItemDTO>>> call, Response<ApiResponse<List<CartItemDTO>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CartItemDTO> list = response.body().getData();
                    if (list == null) {
                        Toast.makeText(requireContext(), "Không có dữ liệu giỏ hàng", Toast.LENGTH_SHORT).show();
                    } else {
                        totalPrice = 0;
                        cartItems = new ArrayList<>();
                        cartItems.addAll(list);
                        cartAdapter.setData(cartItems);

                        if (cartAdapter.getItemCount() == 0) {
                            binding.textEmpty.setVisibility(View.VISIBLE);
                            binding.checkoutBtn.setVisibility(View.GONE);
                        } else {
                            binding.textEmpty.setVisibility(View.GONE);
                            binding.checkoutBtn.setVisibility(View.VISIBLE);
                        }

                        for (CartItemDTO item : cartItems) {
                            totalPrice += item.getPrice();
                        }
                        binding.productTotal.setText(totalPrice + " VND");
                        binding.totalPrice.setText(totalPrice + " VND");

                        // Cập nhật badge ngay sau khi dữ liệu giỏ hàng đã được làm mới
                        if (isAdded() && requireActivity() instanceof DashboardActivity) {
                            ((DashboardActivity) requireActivity()).updateCartBadgeNow();
                        }
                    }
                } else {
                    Log.e("CartItem", "Response not successful or body is null");
                    Toast.makeText(requireContext(), "Failed to load cart items ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartItemDTO>>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addQuantity(String username, int productId) {
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.addQuantity(username, productId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Sau khi server cập nhật, tải lại giỏ -> onResponse của getCartItems sẽ cập nhật badge
                    getCartItems(USERNAME);
                    Toast.makeText(requireContext(), "Tăng số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Tăng số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void minusQuantity(String username, int productId) {
        CartItemApi api = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        api.minusQuantity(username, productId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Sau khi server cập nhật, tải lại giỏ -> onResponse của getCartItems sẽ cập nhật badge
                    getCartItems(USERNAME);
                    Toast.makeText(requireContext(), "Giảm số lượng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Giảm số lượng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}