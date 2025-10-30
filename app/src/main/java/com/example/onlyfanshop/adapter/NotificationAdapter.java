package com.example.onlyfanshop.adapter;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;
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

        // Đổi màu nền theo trạng thái đọc/chưa đọc
        if (n.isRead()) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        }

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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null) return "";
        return dateTime.replace("T", " "); // ví dụ "2025-10-28T21:00" → "2025-10-28 21:00"
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
