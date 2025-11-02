package com.example.onlyfanshop.adapter;


import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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

        // Shop name and order status
        holder.tvShopName.setText("OnlyFan Store");
        
        // Map order status to Vietnamese
        String statusText = mapOrderStatus(order.getOrderStatus());
        holder.tvOrderStatus.setText(statusText);

        // Product info
        if (order.getFirstProductName() != null && !order.getFirstProductName().isEmpty()) {
            holder.tvProductName.setText(order.getFirstProductName());
        } else {
            holder.tvProductName.setText("Sản phẩm");
        }

        if (order.getFirstProductQuantity() != null) {
            holder.tvQuantity.setText("x" + order.getFirstProductQuantity());
        } else {
            holder.tvQuantity.setText("x1");
        }

        // Price display
        if (order.getFirstProductPrice() != null && order.getFirstProductQuantity() != null) {
            double itemPrice = order.getFirstProductPrice();
            double totalItemPrice = itemPrice * order.getFirstProductQuantity();
            
            // Show original price with strikethrough (if different from item price)
            String originalPriceText = formatter.format(itemPrice);
            SpannableString spannableOriginal = new SpannableString(originalPriceText);
            spannableOriginal.setSpan(new StrikethroughSpan(), 0, originalPriceText.length(), 0);
            holder.tvOriginalPrice.setText(spannableOriginal);
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            
            // Show discounted/current price
            holder.tvDiscountedPrice.setText(formatter.format(itemPrice));
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
            holder.tvDiscountedPrice.setText(formatter.format(order.getTotalPrice()));
        }

        // Product image
        if (order.getFirstProductImage() != null && !order.getFirstProductImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(order.getFirstProductImage())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Total price with product count
        String totalText = formatter.format(order.getTotalPrice());
        int productCount = (order.getFirstProductQuantity() != null) ? order.getFirstProductQuantity() : 1;
        holder.tvTotalLabel.setText("Tổng số tiền (" + productCount + " sản phẩm):");
        holder.tvTotalPrice.setText(totalText);

        // Click listener for entire item view
        View.OnClickListener openOrderDetailListener = v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderID());
            context.startActivity(intent);
        };

        // Make entire item clickable
        holder.itemView.setOnClickListener(openOrderDetailListener);
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);

        // View details button (keep for backward compatibility)
        holder.btnViewDetails.setOnClickListener(openOrderDetailListener);
    }

    private String mapOrderStatus(String status) {
        if (status == null) return "Chờ xác nhận";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xác nhận";
            case "APPROVED":
                return "Chờ lấy hàng";
            case "SHIPPED":
                return "Đang vận chuyển";
            case "DELIVERED":
                return "Đã giao hàng";
            case "COMPLETED":
                return "Hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return "Chờ xác nhận";
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvShopName, tvOrderStatus, tvProductName, tvQuantity;
        TextView tvOriginalPrice, tvDiscountedPrice, tvTotalPrice, tvTotalLabel;
        ImageView imgProduct;
        Button btnViewDetails;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tvShopName);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvDiscountedPrice = itemView.findViewById(R.id.tvDiscountedPrice);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvTotalLabel = itemView.findViewById(R.id.tvTotalLabel);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}

