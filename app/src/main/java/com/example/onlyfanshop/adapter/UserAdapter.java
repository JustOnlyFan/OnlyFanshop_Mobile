package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.UserDTO;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<UserDTO> userList;
    private OnUserClickListener listener;

    // Interface to handle click events (e.g.: view details, edit, delete)
    public interface OnUserClickListener {
        void onUserClick(UserDTO user);
    }

    public UserAdapter(Context context, List<UserDTO> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserDTO user = userList.get(position);
        if (user == null) return;

        holder.tvUsername.setText("Username: " + user.getUsername());
        holder.tvEmail.setText("Email: " + user.getEmail());
        holder.tvPhone.setText("Phone: " + user.getPhoneNumber());
        holder.tvAddress.setText("Address: " + user.getAddress());
        holder.tvRole.setText("Role: " + user.getRole());

        // Handle click event on item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername, tvEmail, tvPhone, tvAddress, tvRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvRole = itemView.findViewById(R.id.tvRole);
        }
    }

    // Function to update user list (when filter or search)
    public void updateUserList(List<UserDTO> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }
}
