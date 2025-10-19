package com.example.onlyfanshop.adapter;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<OrderDTO> orderList;

    public OrderAdapter(List<OrderDTO> orderList) {
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
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderDTO order = orderList.get(position);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        holder.tvOrderId.setText("Mã đơn: #" + order.getOrderID());
        holder.tvOrderDate.setText("Ngày đặt: " + order.getOrderDate());
        holder.tvOrderStatus.setText("Trạng thái: " + order.getOrderStatus());
        holder.tvPaymentMethod.setText("Thanh toán: " + order.getPaymentMethod());
        holder.tvTotalPrice.setText("Tổng: " + formatter.format(order.getTotalPrice()));
        holder.btnViewDetails.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderID()); // Truyền orderId sang activity chi tiết
            context.startActivity(intent);        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderStatus, tvPaymentMethod, tvTotalPrice;
        Button btnViewDetails;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}

