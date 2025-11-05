package com.example.onlyfanshop.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import android.content.SharedPreferences;

import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.databinding.ActivityOrderDetailsBinding;
import com.example.onlyfanshop.adapter.OrderItemsAdapter;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.OrderDetailsDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.AppPreferences;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsActivity extends AppCompatActivity {
    private ActivityOrderDetailsBinding binding;
    private OrderApi orderApi;
    private int orderId;
    private NumberFormat formatter;
    private OrderItemsAdapter itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getIntExtra("orderId", -1);
        boolean back = getIntent().getBooleanExtra("payment", false);
        if (orderId == -1) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Setup back button
        binding.btnBack.setOnClickListener(v -> {
            if(back!=true){
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } else{
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }

        });

        // Setup cancel button click listener
        binding.btnCancelOrder.setOnClickListener(v -> showCancelOrderDialog());

        // Setup approve button click listener (for admin)
        binding.btnApproveOrder.setOnClickListener(v -> showApproveOrderDialog());

        // Gọi API lấy chi tiết đơn hàng
        loadOrderDetails(orderId);
    }

    private String resolveImageUrl(String raw) {
        if (raw == null) return null;
        String u = raw.trim();
        if (u.isEmpty()) return null;
        u = u.replace("http://localhost:", "http://10.0.2.2:")
             .replace("http://127.0.0.1:", "http://10.0.2.2:");
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("/")) return "http://10.0.2.2:8080" + u;
        return "http://10.0.2.2:8080/" + u;
    }

    private void loadOrderDetails(int orderId) {
        orderApi = ApiClient.getPrivateClient(this).create(OrderApi.class);
        orderApi.getOrderDetails(orderId).enqueue(new Callback<ApiResponse<OrderDetailsDTO>>() {
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

    private String formatCurrencyVND(double value) {
        try {
            return formatter.format(value);
        } catch (Exception e) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            return nf.format(value);
        }
    }

    private Object buildGlideModel(String url) {
        if (url == null) return null;
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        if (token == null || token.isEmpty()) return url;
        LazyHeaders headers = new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        return new GlideUrl(url, headers);
    }

    private void showOrderDetails(OrderDetailsDTO order) {
        // Order Status Banner
        String statusText = mapOrderStatus(order.getOrderStatus());
        binding.tvOrderStatusBanner.setText(statusText);
        
        // Set banner color based on status
        int statusColor = getStatusColor(order.getOrderStatus());
        binding.tvOrderStatusBanner.setBackgroundColor(statusColor);

        // Payment Method
        String paymentText = "Thanh toán bằng " + mapPaymentMethod(order.getPaymentMethod());
        binding.tvPaymentMethod.setText(paymentText);

        // Recipient Info
        String recipientInfo = order.getCustomerName();
        if (order.getPhone() != null && !order.getPhone().isEmpty()) {
            recipientInfo += " " + formatPhoneNumber(order.getPhone());
        }
        binding.tvRecipientInfo.setText(recipientInfo);

        // Shipping Address - Debug and display
        String address = order.getAddress();
        Log.d("OrderDetail", "Address from order: " + address);
        Log.d("OrderDetail", "Billing address: " + order.getBillingAddress());
        
        if (address != null && !address.isEmpty() && !address.trim().isEmpty()) {
            binding.tvShippingAddress.setText(address);
        } else {
            // Try billing address as fallback
            String billingAddress = order.getBillingAddress();
            if (billingAddress != null && !billingAddress.isEmpty() && !billingAddress.trim().isEmpty()) {
                binding.tvShippingAddress.setText(billingAddress);
                Log.d("OrderDetail", "Using billing address as fallback: " + billingAddress);
            } else {
                binding.tvShippingAddress.setText("Chưa có địa chỉ");
                Log.d("OrderDetail", "No address found - showing default message");
            }
        }

        double computedTotal = 0d;
        // Product Info (take first item from cart) and compute total from ALL items
        List<CartItemDTO> allItems = null;
        if (order.getItemsLite() != null && !order.getItemsLite().isEmpty()) {
            // Prefer lite list if server provides it
            allItems = new java.util.ArrayList<>();
            for (com.example.onlyfanshop.model.OrderItemLiteDTO l : order.getItemsLite()) {
                CartItemDTO ci = new CartItemDTO();
                ci.setQuantity(l.getQuantity());
                ci.setPrice(l.getPrice());
                ProductDTO p = new ProductDTO();
                p.setProductName(l.getProductName());
                p.setImageURL(l.getImageURL());
                ci.setProductDTO(p);
                allItems.add(ci);
            }
        } else if (order.getCartDTO() != null && order.getCartDTO().getItems() != null && !order.getCartDTO().getItems().isEmpty()) {
            allItems = order.getCartDTO().getItems();
        }
        if (allItems != null && !allItems.isEmpty()) {
            // setup recycler if needed
            if (itemsAdapter == null) {
                itemsAdapter = new OrderItemsAdapter();
                binding.recyclerOrderItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
                // Tối ưu scroll performance
                binding.recyclerOrderItems.setHasFixedSize(true);
                binding.recyclerOrderItems.setItemViewCacheSize(10);
                binding.recyclerOrderItems.setItemAnimator(null); // Tắt animation khi scroll để mượt hơn
                binding.recyclerOrderItems.setAdapter(itemsAdapter);
            }
            itemsAdapter.submitList(allItems);
            for (CartItemDTO it : allItems) {
                double p = it.getPrice() != null ? it.getPrice() : 0d;
                double q = it.getQuantity() != null ? it.getQuantity() : 0d;
                computedTotal += p * q;
                Log.d("OrderDetail", "Sum item price=" + p + " qty=" + q + " -> lineTotal=" + (p*q));
            }
            Log.d("OrderDetail", "Items count: " + allItems.size());
        }

        // Total Price: use computed sum from items to avoid backend mismatch
        double finalTotal = computedTotal > 0 ? computedTotal : (order.getTotalPrice() != null ? order.getTotalPrice() : 0d);
        Log.d("OrderDetail", "computedTotal=" + computedTotal + ", apiTotal=" + order.getTotalPrice() + ", finalTotal=" + finalTotal);
        binding.tvTotalPrice.setText(formatCurrencyVND(finalTotal));

        // Show/Hide buttons based on status and user role
        String status = order.getOrderStatus();
        String userRole = AppPreferences.getUserRole(this);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        
        if ("PENDING".equalsIgnoreCase(status)) {
            if (isAdmin) {
                // Admin can approve pending orders
                binding.btnApproveOrder.setVisibility(View.VISIBLE);
                binding.btnCancelOrder.setVisibility(View.GONE);
            } else {
                // Customer can cancel pending orders
                binding.btnApproveOrder.setVisibility(View.GONE);
                binding.btnCancelOrder.setVisibility(View.VISIBLE);
            }
        } else {
            binding.btnApproveOrder.setVisibility(View.GONE);
            binding.btnCancelOrder.setVisibility(View.GONE);
        }

        // Store Name (default)
        binding.tvStoreName.setText("OnlyFan Store");
    }

    private String mapOrderStatus(String status) {
        if (status == null) return "Không xác định";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ duyệt";
            case "PICKING":
                return "Chờ lấy hàng";
            case "SHIPPING":
                return "Đang giao hàng";
            case "DELIVERED":
                return "Đã giao hàng";
            case "RETURNS_REFUNDS":
                return "Hoàn trả/Hoàn tiền";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFF4CAF50;
        switch (status.toUpperCase()) {
            case "PENDING":
                return 0xFFFFC107; // Yellow/Amber
            case "PICKING":
                return 0xFF2196F3; // Blue
            case "SHIPPING":
                return 0xFFFF9800; // Orange
            case "DELIVERED":
                return 0xFF4CAF50; // Green
            case "RETURNS_REFUNDS":
                return 0xFFFF6B00; // Orange
            case "CANCELLED":
                return 0xFFF44336; // Red
            default:
                return 0xFF4CAF50;
        }
    }

    private String mapPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return "Chưa xác định";
        if (paymentMethod.equalsIgnoreCase("COD")) {
            return "Thanh toán khi nhận hàng";
        } else if (paymentMethod.equalsIgnoreCase("VNPAY")) {
            return "VNPay";
        }
        return paymentMethod;
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        // Format: (+84) 981 667 547
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() >= 9) {
            String last9 = cleaned.substring(cleaned.length() - 9);
            return "(+84) " + last9.substring(0, 3) + " " + last9.substring(3, 6) + " " + last9.substring(6);
        }
        return phone;
    }

    private void showCancelOrderDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder() {
        orderApi.cancelOrder(orderId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatusCode() == 200) {
                    Toast.makeText(OrderDetailsActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // Reload order details to update UI
                    loadOrderDetails(orderId);
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "Không thể hủy đơn hàng";
                    Toast.makeText(OrderDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("OrderDetail", "Error canceling order: ", t);
            }
        });
    }

    private void showApproveOrderDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận duyệt đơn hàng")
                .setMessage("Bạn có chắc chắn muốn duyệt đơn hàng này? Đơn hàng sẽ chuyển sang trạng thái 'Chờ lấy hàng'.")
                .setPositiveButton("Duyệt", (dialog, which) -> approveOrder())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void approveOrder() {
        orderApi.setOrderStatus(orderId, "PICKING").enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getStatusCode() == 200) {
                        Toast.makeText(OrderDetailsActivity.this, "Đã duyệt đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        // Reload order details to update UI
                        loadOrderDetails(orderId);
                    } else {
                        // Handle error response
                        String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Không thể duyệt đơn hàng";
                        Toast.makeText(OrderDetailsActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.e("OrderDetail", "Error approving order: " + message + " (Status code: " + apiResponse.getStatusCode() + ")");
                    }
                } else {
                    String errorMessage = "Không thể duyệt đơn hàng";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage();
                    }
                    Toast.makeText(OrderDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("OrderDetail", "Error response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("OrderDetail", "Error approving order: ", t);
            }
        });
    }
}
