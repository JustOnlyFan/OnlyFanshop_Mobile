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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.OrderItemLiteDTO;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
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

        // Product info - always show first product
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
            
            // Show original price with strikethrough
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

        // Total price
        String totalText = formatter.format(order.getTotalPrice());
        holder.tvTotalPrice.setText(totalText);

        // Setup dropdown for additional products
        List<OrderItemLiteDTO> products = order.getProducts();
        int totalProductCount = order.getTotalProductCount() != null ? order.getTotalProductCount() : 0;
        
        // Show dropdown button if there are more than 1 product
        if (products != null && products.size() > 1) {
            int remainingCount = totalProductCount - (order.getFirstProductQuantity() != null ? order.getFirstProductQuantity() : 0);
            holder.layoutDropdownButton.setVisibility(View.VISIBLE);
            holder.tvProductCount.setText(String.format("Xem thêm %d sản phẩm", remainingCount));
            
            // Get additional products (skip first one)
            List<OrderItemLiteDTO> additionalProducts = new ArrayList<>(products.subList(1, products.size()));
            
            // Setup recycler for additional products
            if (holder.additionalProductsAdapter == null) {
                holder.additionalProductsAdapter = new OrderProductAdapter();
                holder.recyclerAdditionalProducts.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                holder.recyclerAdditionalProducts.setItemAnimator(null); // Disable animation for smooth scrolling
                holder.recyclerAdditionalProducts.setHasFixedSize(true);
                holder.recyclerAdditionalProducts.setAdapter(holder.additionalProductsAdapter);
            }
            
            holder.additionalProductsAdapter.submitList(additionalProducts);
            
            // Setup dropdown toggle
            holder.layoutDropdownButton.setOnClickListener(v -> {
                v.setClickable(false); // Prevent double click
                boolean isExpanded = holder.layoutAdditionalProducts.getVisibility() == View.VISIBLE;
                if (isExpanded) {
                    holder.layoutAdditionalProducts.setVisibility(View.GONE);
                    holder.ivDropdownArrow.animate().rotation(0).setDuration(150).start();
                } else {
                    holder.layoutAdditionalProducts.setVisibility(View.VISIBLE);
                    holder.ivDropdownArrow.animate().rotation(180).setDuration(150).start();
                }
                v.postDelayed(() -> v.setClickable(true), 200);
            });
            
            // Make dropdown clickable but don't propagate to item
            holder.layoutDropdownButton.setClickable(true);
            holder.layoutDropdownButton.setFocusable(true);
        } else {
            holder.layoutDropdownButton.setVisibility(View.GONE);
            holder.layoutAdditionalProducts.setVisibility(View.GONE);
        }

        // Click listener for entire item view
        View.OnClickListener openOrderDetailListener = v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderID());
            // Không thêm flag "fromPaymentSuccess" hoặc set = false để biết là từ order pending
            intent.putExtra("fromPaymentSuccess", false);
            context.startActivity(intent);
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right, R.anim.slide_out_left);
            }
        };

        // Make card clickable, but if dropdown exists, only clickable outside dropdown area
        if (holder.layoutDropdownButton != null && holder.layoutDropdownButton.getVisibility() == View.VISIBLE) {
            // Set click on individual views instead of whole item
            holder.imgProduct.setOnClickListener(openOrderDetailListener);
            holder.tvProductName.setOnClickListener(openOrderDetailListener);
            holder.tvQuantity.setOnClickListener(openOrderDetailListener);
            holder.tvDiscountedPrice.setOnClickListener(openOrderDetailListener);
        } else {
            // No dropdown, make entire item clickable
            holder.itemView.setOnClickListener(openOrderDetailListener);
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
        }

        // View details button may not exist in minimal layout
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(openOrderDetailListener);
        }
    }

    private String mapOrderStatus(String status) {
        if (status == null) return "Chờ xác nhận";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xác nhận";
            case "PICKING":
                return "Chờ lấy hàng";
            case "SHIPPING":
                return "Đang vận chuyển";
            case "DELIVERED":
                return "Đã giao hàng";
            case "RETURNS_REFUNDS":
                return "Hoàn trả/Hoàn tiền";
            case "CANCELLED":
                return "Đã hủy";
            // Backward compatibility with old statuses
            case "APPROVED":
                return "Chờ lấy hàng";
            case "SHIPPED":
                return "Đang vận chuyển";
            case "COMPLETED":
                return "Đã giao hàng";
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
        TextView tvOriginalPrice, tvDiscountedPrice, tvTotalPrice;
        ImageView imgProduct;
        Button btnViewDetails;
        
        // Dropdown views
        LinearLayout layoutDropdownButton;
        LinearLayout layoutAdditionalProducts;
        RecyclerView recyclerAdditionalProducts;
        TextView tvProductCount;
        ImageView ivDropdownArrow;
        OrderProductAdapter additionalProductsAdapter;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tvShopName);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvDiscountedPrice = itemView.findViewById(R.id.tvDiscountedPrice);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            
            // Dropdown views
            layoutDropdownButton = itemView.findViewById(R.id.layoutDropdownButton);
            layoutAdditionalProducts = itemView.findViewById(R.id.layoutAdditionalProducts);
            recyclerAdditionalProducts = itemView.findViewById(R.id.recyclerAdditionalProducts);
            tvProductCount = itemView.findViewById(R.id.tvProductCount);
            ivDropdownArrow = itemView.findViewById(R.id.ivDropdownArrow);
        }
    }
}

