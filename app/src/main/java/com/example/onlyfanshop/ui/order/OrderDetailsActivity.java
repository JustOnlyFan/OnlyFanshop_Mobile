package com.example.onlyfanshop.ui.order;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.databinding.ActivityOrderDetailsBinding;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.OrderDetailsDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsActivity extends AppCompatActivity {
    private ActivityOrderDetailsBinding binding;
    private CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int orderId = getIntent().getIntExtra("orderId", -1);
        if (orderId == -1) {
            Toast.makeText(this, "Không tìm thấy ID đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cấu hình RecyclerView
        binding.recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));

        // Gọi API lấy chi tiết đơn hàng
        loadOrderDetails(orderId);
    }

    private void loadOrderDetails(int orderId) {
        OrderApi api = ApiClient.getPrivateClient(this).create(OrderApi.class);
        api.getOrderDetails(orderId).enqueue(new Callback<ApiResponse<OrderDetailsDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<OrderDetailsDTO>> call, Response<ApiResponse<OrderDetailsDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OrderDetailsDTO order = response.body().getData();
                    if (order != null) {
                        showOrderDetails(order);
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "Không tải được chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                    Log.e("OrderDetail", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<OrderDetailsDTO>> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("OrderDetail", "Error: ", t);
            }
        });
    }

    private void showOrderDetails(OrderDetailsDTO order) {
        binding.tvOrderId.setText("Mã đơn hàng: " + order.getOrderID());
        binding.tvCustomerName.setText("Khách hàng: " + order.getCustomerName());
        binding.tvEmail.setText("Email: " + order.getEmail());
        binding.tvPhone.setText("Số điện thoại: " + order.getPhone());
        binding.tvAddress.setText("Địa chỉ giao hàng: " + order.getAddress());
        binding.tvPaymentMethod.setText("Thanh toán: " + order.getPaymentMethod());
        binding.tvOrderStatus.setText("Trạng thái: " + order.getOrderStatus());
        binding.tvOrderDate.setText("Ngày đặt hàng: " + order.getOrderDate());
        binding.tvTotalPrice.setText("Tổng tiền: " + order.getTotalPrice() + " VND");
        binding.btnBackMain.setOnClickListener(v -> finish());
        if (order.getCartDTO() != null && order.getCartDTO().getItems() != null) {
            List<CartItemDTO> cartItems = order.getCartDTO().getItems();
            cartAdapter = new CartAdapter(this, cartItems, false);
            binding.recyclerViewProducts.setAdapter(cartAdapter);
        }
    }

}
