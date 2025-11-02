package com.example.onlyfanshop.adapter;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.NotificationApi;
import com.example.onlyfanshop.model.NotificationDTO;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private Context context;
    private List<NotificationDTO> list;

    public NotificationAdapter(Context context, List<NotificationDTO> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationDTO n = list.get(position);

        holder.tvMessage.setText(n.getMessage());
        holder.tvCreatedAt.setText(formatDateTime(n.getCreatedAt()));

        // Set icon based on notification type
        setNotificationIcon(holder.ivNotificationIcon, holder.ivBadgeIcon, n.getMessage());

        holder.itemView.setOnClickListener(v -> {
            Integer orderId = extractOrderIdFromMessage(n.getMessage());
            if (orderId != null) {
                // Gọi API đánh dấu đã đọc
                NotificationApi apiService = ApiClient.getPrivateClient(context).create(NotificationApi.class);
                apiService.markAsRead(n.getNotificationID()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            int currentPosition = holder.getAdapterPosition();
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                NotificationDTO currentNotification = list.get(currentPosition);
                                currentNotification.setRead(true);
                                notifyItemChanged(currentPosition);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("API_ERROR", "Không thể cập nhật trạng thái", t);
                    }
                });

                // Chuyển sang trang chi tiết đơn hàng
                Intent intent = new Intent(context, OrderDetailsActivity.class);
                intent.putExtra("orderId", orderId);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Order ID not found in notification", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvCreatedAt;
        ImageView ivNotificationIcon, ivBadgeIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
        }
    }

    private void setNotificationIcon(ImageView iconView, ImageView badgeView, String message) {
        if (message == null) {
            iconView.setImageResource(R.drawable.ic_notification);
            iconView.clearColorFilter();
            badgeView.setVisibility(View.GONE);
            return;
        }

        String lowerMessage = message.toLowerCase();
        
        // Determine icon based on message content
        if (lowerMessage.contains("chúc mừng") || lowerMessage.contains("tặng") || 
            lowerMessage.contains("fgold") || lowerMessage.contains("tích điểm")) {
            // Celebration/Bonus/FGold notifications - gold badge icon
            iconView.setImageResource(R.drawable.ic_badge_star);
            iconView.clearColorFilter();
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setImageResource(R.drawable.badge_background);
            badgeView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
        } else if (lowerMessage.contains("đơn hàng") || lowerMessage.contains("order") || 
                   lowerMessage.contains("duyệt") || lowerMessage.contains("giao") ||
                   lowerMessage.contains("picking") || lowerMessage.contains("shipping")) {
            // Order notifications - shipping box
            iconView.setImageResource(R.drawable.ic_shipping_box);
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            badgeView.setVisibility(View.GONE);
        } else if (lowerMessage.contains("thanh toán") || lowerMessage.contains("payment")) {
            // Payment notifications - credit card
            iconView.setImageResource(R.drawable.ic_credit_card);
            iconView.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            badgeView.setVisibility(View.GONE);
        } else if (lowerMessage.contains("hợp đồng") || lowerMessage.contains("contract")) {
            // Contract notifications - document
            iconView.setImageResource(R.drawable.ic_document);
            iconView.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
            badgeView.setVisibility(View.GONE);
        } else if (lowerMessage.contains("sinh nhật") || lowerMessage.contains("birthday")) {
            // Birthday notifications - badge with special badge
            iconView.setImageResource(R.drawable.ic_badge_star);
            iconView.clearColorFilter();
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setImageResource(R.drawable.badge_background);
            badgeView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
        } else if (lowerMessage.contains("bảo vệ") || lowerMessage.contains("giáp") || 
                   lowerMessage.contains("wifi") || lowerMessage.contains("security") ||
                   lowerMessage.contains("nâng cấp")) {
            // Security/Shield notifications
            iconView.setImageResource(R.drawable.ic_shield_check);
            iconView.clearColorFilter();
            badgeView.setVisibility(View.GONE);
        } else {
            // Default notification icon
            iconView.setImageResource(R.drawable.ic_notification);
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            badgeView.setVisibility(View.GONE);
        }
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return "";
        
        try {
            // Parse ISO format: "2025-11-02T19:43:00" or "2025-11-02T19:43:00.000Z"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            
            // Remove milliseconds and timezone if present
            String cleanDateTime = dateTime.split("\\.")[0].replace("Z", "");
            Date date = inputFormat.parse(cleanDateTime);
            
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.e("NotificationAdapter", "Error parsing date: " + dateTime, e);
        }
        
        // Fallback: simple replacement
        return dateTime.replace("T", " ").substring(0, Math.min(dateTime.length(), 16));
    }
    private Integer extractOrderIdFromMessage(String message) {
        if (message == null) return null;

        // Tìm chuỗi có dạng #1234 hoặc Đơn hàng 1234
        Pattern pattern = Pattern.compile("(?:#|Đơn hàng\\s*)(\\d+)");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
