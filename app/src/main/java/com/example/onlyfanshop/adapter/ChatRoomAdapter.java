package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.chat.ChatRoom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private List<ChatRoom> chatRooms;
    private OnChatRoomClickListener listener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
        this.chatRooms = chatRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        
        // ✅ Fix: Luôn ưu tiên extract customerName từ roomId để đảm bảo hiển thị đúng
        // RoomId là source of truth, không phụ thuộc vào database local
        String customerName = extractCustomerNameFromRoomId(chatRoom.getRoomId());
        if (customerName == null || customerName.isEmpty()) {
            // Fallback về customerName từ API nếu không extract được từ roomId
            customerName = chatRoom.getCustomerName();
            if (customerName == null || customerName.isEmpty()) {
                customerName = "Customer";
            }
        }
        
        holder.customerName.setText(customerName);
        holder.lastMessage.setText(chatRoom.getLastMessage());
        
        // Format time
        if (chatRoom.getLastMessageTime() != null && !chatRoom.getLastMessageTime().isEmpty()) {
            try {
                // Parse ISO string to LocalDateTime and format
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(chatRoom.getLastMessageTime());
                holder.lastMessageTime.setText(dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception e) {
                // Fallback to raw string if parsing fails
                holder.lastMessageTime.setText(chatRoom.getLastMessageTime());
            }
        } else {
            holder.lastMessageTime.setText("");
        }
        
        // Set online status
        if (chatRoom.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }
        
        // Set unread count
        if (chatRoom.getUnreadCount() > 0) {
            holder.unreadCount.setText(String.valueOf(chatRoom.getUnreadCount()));
            holder.unreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatRoomClick(chatRoom);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }
    
    // ✅ Extract customer name từ roomId
    // Room ID format: chatRoom_username_userId (ví dụ: chatRoom_huy_4)
    private String extractCustomerNameFromRoomId(String roomId) {
        try {
            if (roomId != null && roomId.startsWith("chatRoom_")) {
                String[] parts = roomId.split("_");
                // parts[0] = "chatRoom"
                // parts[1] = username (e.g., "huy", "NTT")
                // parts[2] = userId (e.g., "4")
                if (parts.length >= 3) {
                    return parts[1]; // Return username
                } else if (parts.length == 2) {
                    // Fallback for old format: chatRoom_userId
                    return null; // Cannot extract username
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        TextView customerName;
        TextView lastMessage;
        TextView lastMessageTime;
        TextView unreadCount;
        ImageView customerAvatar;
        View onlineIndicator;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            lastMessageTime = itemView.findViewById(R.id.lastMessageTime);
            unreadCount = itemView.findViewById(R.id.unreadCount);
            customerAvatar = itemView.findViewById(R.id.customerAvatar);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}
