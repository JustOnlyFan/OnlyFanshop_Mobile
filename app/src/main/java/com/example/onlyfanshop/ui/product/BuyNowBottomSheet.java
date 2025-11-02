package com.example.onlyfanshop.ui.product;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.databinding.LayoutBottomSheetBuyNowBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.Request.AddToCartRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.payment.ConfirmPaymentActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyNowBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_PRODUCT_NAME = "product_name";
    private static final String ARG_PRODUCT_PRICE = "product_price";
    private static final String ARG_PRODUCT_IMAGE = "product_image";
    private static final String ARG_PRODUCT_ID="product_id";

    private LayoutBottomSheetBuyNowBinding binding;

    public static BuyNowBottomSheet newInstance(String name, String price, String imageURL, int productID) {
        BuyNowBottomSheet fragment = new BuyNowBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_NAME, name);
        args.putString(ARG_PRODUCT_PRICE, price);
        args.putString(ARG_PRODUCT_IMAGE, imageURL);
        args.putInt(ARG_PRODUCT_ID, productID);
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = LayoutBottomSheetBuyNowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AtomicInteger quantity = new AtomicInteger(1);



        // Lấy dữ liệu từ Bundle
        if (getArguments() != null) {
            String name = getArguments().getString(ARG_PRODUCT_NAME);
            String price = getArguments().getString(ARG_PRODUCT_PRICE);

            binding.tvProductNameBottom.setText(name);
            binding.tvProductPriceBottom.setText(price);
            String imageUrl = getArguments().getString(ARG_PRODUCT_IMAGE);
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.imgProductThumb);
        }
        double totalPrice = Double.parseDouble(binding.tvProductPriceBottom.getText().toString().replace("$", ""));

        binding.tvQuantity.setText(String.valueOf(quantity.get()));

        binding.btnAdd.setOnClickListener(v -> {
            quantity.incrementAndGet();
            binding.tvProductPriceBottom.setText("$"+totalPrice*quantity.get());
            binding.tvQuantity.setText(String.valueOf(quantity.get()));
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (quantity.get() > 1) {
                quantity.decrementAndGet();
                binding.tvProductPriceBottom.setText("$"+totalPrice*quantity.get());
                binding.tvQuantity.setText(String.valueOf(quantity.get()));
            }
        });

        binding.btnConfirmBuy.setOnClickListener(v -> {
            instantBuy(getArguments().getInt(ARG_PRODUCT_ID), quantity.get(), totalPrice*quantity.get());
        });
    }

    private void instantBuy(int productID, int quantity, double totalPrice){
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        String token = sharedPreferences.getString("jwt_token", "");
        sharedPreferences.edit().putString("buyMethod", "Instant").apply();


        if (username == null || username.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            // Tạo dialog giống PleaseSignInFragment, nền trắng
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Please sign in")
                    .setMessage("You need to sign in to continue.")
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.login.LoginActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.white); // Nền trắng
            dialog.show();
            return;
        }
        AddToCartRequest request = new AddToCartRequest(productID, quantity, username);
        CartItemApi cartItemApi = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        cartItemApi.instantBuy(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()){
                    confirmInstantBuy(username, totalPrice);
                }else{
                    Log.e("CartItem", "Response not successful or body is null");
                    Toast.makeText(requireContext(), "Failed to load cart items ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("Payment", "Network error: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmInstantBuy(String userName, double totalPrice){
        CartItemApi cartItemApi = ApiClient.getPrivateClient(requireContext()).create(CartItemApi.class);
        cartItemApi.getInstantBuyItem(userName).enqueue(new Callback<ApiResponse<List<CartItemDTO>>>() {

            @Override
            public void onResponse(Call<ApiResponse<List<CartItemDTO>>> call, Response<ApiResponse<List<CartItemDTO>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<CartItemDTO> cartItems = response.body().getData();
                    Intent intent = new Intent(requireContext(), ConfirmPaymentActivity.class);
                    intent.putExtra("totalPrice", totalPrice);
                    intent.putExtra("cartItems", (Serializable) cartItems);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartItemDTO>>> call, Throwable throwable) {

            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
