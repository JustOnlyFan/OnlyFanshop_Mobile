package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.chat.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LEFT = 0;
    private static final int TYPE_RIGHT = 1;
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isMe() ? TYPE_RIGHT : TYPE_LEFT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RIGHT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_right, parent, false);
            return new RightViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_left, parent, false);
            return new LeftViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof LeftViewHolder) {
            LeftViewHolder vh = (LeftViewHolder) holder;
            vh.message.setText(message.getMessage());
            vh.time.setText(message.getTime());
            vh.avatar.setImageResource(message.getAvatarRes());
        } else if (holder instanceof RightViewHolder) {
            RightViewHolder vh = (RightViewHolder) holder;
            vh.message.setText(message.getMessage());
            vh.time.setText(message.getTime());
            vh.avatar.setImageResource(message.getAvatarRes());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // --- ViewHolder tin nhắn bên trái ---
    public static class LeftViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;
        ImageView avatar;

        public LeftViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.messageLeft);
            time = itemView.findViewById(R.id.timeLeft);
            avatar = itemView.findViewById(R.id.avatarLeft);
        }
    }

    // --- ViewHolder tin nhắn bên phải ---
    public static class RightViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;
        ImageView avatar;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.messageRight);
            time = itemView.findViewById(R.id.timeRight);
            avatar = itemView.findViewById(R.id.avatarRight);
        }
    }
}