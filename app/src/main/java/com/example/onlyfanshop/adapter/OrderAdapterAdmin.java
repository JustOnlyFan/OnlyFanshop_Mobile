package com.example.onlyfanshop.adapter;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderAdapterAdmin extends RecyclerView.Adapter<OrderAdapterAdmin.OrderViewHolder> {

    private List<OrderDTO> orderList;

    public OrderAdapterAdmin(List<OrderDTO> orderList) {
        this.orderList = orderList;
    }

    public void setOrderList(List<OrderDTO> newOrders) {
        this.orderList = newOrders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_admin, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderDTO order = orderList.get(position);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvOrderId.setText("Mã đơn: #" + order.getOrderID());
        holder.tvOrderDate.setText("Ngày đặt: " + order.getOrderDate());
        holder.tvPaymentMethod.setText("Thanh toán: " + order.getPaymentMethod());
        holder.tvTotalPrice.setText("Tổng: " + formatter.format(order.getTotalPrice()));

        String status = order.getOrderStatus();
        holder.tvOrderStatus.setText(formatStatusText(status));
        holder.updateStatusDisplay(status);

        // Ẩn nút prev/next status vì chỉ xem chi tiết để approve
        holder.btnPrevStatus.setVisibility(View.GONE);
        holder.btnNextStatus.setVisibility(View.GONE);

        holder.btnViewDetails.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderID());
            context.startActivity(intent);
        });
    }

    private String formatStatusText(String status) {
        if (status == null) return "Unknown";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Pending";
            case "PICKING":
                return "Picking";
            case "SHIPPING":
                return "Shipping";
            case "DELIVERED":
                return "Delivered";
            case "RETURNS_REFUNDS":
                return "Returns/Refunds";
            case "CANCELLED":
                return "Cancelled";
            default:
                return status;
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderStatus, tvPaymentMethod, tvTotalPrice;
        Button btnViewDetails;
        ImageView btnPrevStatus, btnNextStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnPrevStatus = itemView.findViewById(R.id.btnPrevStatus);
            btnNextStatus = itemView.findViewById(R.id.btnNextStatus);
        }

        public void updateStatusDisplay(String status) {
            if (status == null) {
                tvOrderStatus.setBackground(null);
                tvOrderStatus.setTextColor(Color.BLACK);
                return;
            }
            
            switch (status.toUpperCase()) {
                case "PENDING":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvOrderStatus.setTextColor(Color.parseColor("#856404"));
                    break;
                case "PICKING":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    tvOrderStatus.setTextColor(Color.parseColor("#0C5460"));
                    break;
                case "SHIPPING":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_shipping);
                    tvOrderStatus.setTextColor(Color.parseColor("#004085"));
                    break;
                case "DELIVERED":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    tvOrderStatus.setTextColor(Color.parseColor("#155724"));
                    break;
                case "RETURNS_REFUNDS":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvOrderStatus.setTextColor(Color.parseColor("#FF6B00"));
                    break;
                case "CANCELLED":
                    tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvOrderStatus.setTextColor(Color.parseColor("#DC3545"));
                    break;
                default:
                    tvOrderStatus.setBackground(null);
                    tvOrderStatus.setTextColor(Color.BLACK);
                    break;
            }
        }
    }
    private void updateOrderStatus(Context context, int orderId, String newStatus) {
        OrderApi api = ApiClient.getPrivateClient(context).create(OrderApi.class);

        api.setOrderStatus(orderId, newStatus).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.getStatusCode() == 200) {
                        Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}


