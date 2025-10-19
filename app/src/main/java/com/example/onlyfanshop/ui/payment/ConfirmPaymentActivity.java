package com.example.onlyfanshop.ui.payment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.PaymentApi;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.databinding.ActivityConfirmPaymentBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.PaymentDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmPaymentActivity extends AppCompatActivity {
    private ActivityConfirmPaymentBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItemDTO> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityConfirmPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        cartItems = (List<CartItemDTO>) getIntent().getSerializableExtra("cartItems");
        double totalPrice = getIntent().getDoubleExtra("totalPrice", 0.0);
        binding.totalPrice.setText(totalPrice + " VND");
        binding.rclViewCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, cartItems, false);
        binding.rclViewCart.setAdapter(cartAdapter);
        binding.btnCancle.setOnClickListener(v -> finish());
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        sharedPreferences.edit().putString("chosenAddress", "").apply();
        binding.radioAddress.setOnCheckedChangeListener((group, checkedID) -> {
            if (checkedID == binding.radioBtnDfAd.getId()) {
                chooseDfAddress(sharedPreferences);
            } else if (checkedID == binding.radioBtnOtherAd.getId()) {
                chooseOtherAddress(sharedPreferences);
            }
        });
        String checkAddress = sharedPreferences.getString("chosenAddress","");
//        if(checkAddress.isEmpty()){
//            binding.checkoutBtn.setEnabled(false);
//        }else binding.checkoutBtn.setEnabled(true);
        binding.checkoutBtn.setOnClickListener(v -> {testPayment(totalPrice, sharedPreferences);
        });
    }
    private void changeAddress(String address, String token) {

        UserApi userApi = ApiClient.getPrivateClient(this).create(UserApi.class);
        userApi.changeAddress(address, "Bearer "+token).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConfirmPaymentActivity.this, "Cập nhật địa chỉ thành công ✅", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfirmPaymentActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Toast.makeText(ConfirmPaymentActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void chooseDfAddress(SharedPreferences sharedPreferences){
        String address = sharedPreferences.getString("address", "");
        String token = sharedPreferences.getString("jwt_token", "");
        binding.edtOtherAddress.setVisibility(View.GONE);
        if (!address.isEmpty()) {
            binding.textViewDfAd.setVisibility(View.VISIBLE);
            binding.textViewDfAd.setText(address);
            sharedPreferences.edit().putString("chosenAddress", address).apply();
            binding.edtDefaultAddress.setVisibility(View.GONE);
        } else {
            binding.edtDefaultAddress.setVisibility(View.VISIBLE);
            binding.edtDefaultAddress.setHint("Bạn chưa có địa chỉ, vui lòng nhập địa chỉ....");
            binding.edtDefaultAddress.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) { // rời khỏi ô nhập
                    String address_1 = binding.edtDefaultAddress.getText().toString().trim();
                    if (!address_1.isEmpty()) {
                        changeAddress(address_1, token);
                        sharedPreferences.edit().putString("address", address_1).apply();
                        sharedPreferences.edit().putString("chosenAddress", address_1).apply();
                    }
                    Log.d("TAG", "Giá trị khi rời khỏi ô: " + address_1);
                }
            });
        }
    }

    private void chooseOtherAddress(SharedPreferences sharedPreferences){
        binding.textViewDfAd.setVisibility(View.GONE);
        binding.edtDefaultAddress.setVisibility(View.GONE);
        binding.edtOtherAddress.setVisibility(View.VISIBLE);
        binding.edtOtherAddress.setHint("Vui lòng nhập địa chỉ....");
        binding.edtOtherAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // rời khỏi ô nhập
                String address_2 = binding.edtOtherAddress.getText().toString().trim();
                if (!address_2.isEmpty()) {
                    sharedPreferences.edit().putString("chosenAddress", address_2).apply();
                }
                Log.d("TAG", "Giá trị khi rời khỏi ô: " + address_2);
            }
        });
    }
    private void testPayment(Double totalPrice, SharedPreferences sharedPreferences) {

//        String priceString = textBottomPrice.getText().toString().replace("$", "");
//        double amount;
//        try {
//            amount = Double.parseDouble(priceString);
//        } catch (NumberFormatException e) {
//            Toast.makeText(this, "Invalid product price", Toast.LENGTH_SHORT).show();
//            return;
//        }
        String address = sharedPreferences.getString("chosenAddress","");

        String bankCode = "NCB";
//        Log.d("Payment", "Creating payment with amount: " + amount + " and bankCode: " + bankCode);
////        showLoading(true);

        PaymentApi api = ApiClient.getPrivateClient(this).create(PaymentApi.class);
        api.createPayment(totalPrice, bankCode, address).enqueue(new Callback<ApiResponse<PaymentDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentDTO>> call, Response<ApiResponse<PaymentDTO>> response) {
//                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String paymentUrl = response.body().getData().getPaymentUrl();
                    Log.d("Payment", "Payment URL: " + paymentUrl);
                    Toast.makeText(ConfirmPaymentActivity.this, "Redirecting to payment...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ConfirmPaymentActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra(PaymentWebViewActivity.EXTRA_URL, paymentUrl);
                    startActivity(intent);
                } else {
                    Log.e("Payment", "API call failed with response code: " + response.code());
                    Toast.makeText(ConfirmPaymentActivity.this, "Failed to create payment.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentDTO>> call, Throwable t) {
//                showLoading(false);
                Log.e("Payment", "Network error: " + t.getMessage(), t);
                Toast.makeText(ConfirmPaymentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
//    private void showLoading(boolean show) {
//        if (progressBar != null) {
//            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        }
//    }
}