package com.example.onlyfanshop.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.StoreLocation;

import java.util.ArrayList;
import java.util.List;

public class StoreLocationAdapter extends RecyclerView.Adapter<StoreLocationAdapter.ViewHolder> {

    public interface OnStoreActionListener {
        void onEditStore(StoreLocation store);
        void onDeleteStore(StoreLocation store);
        void onStoreClick(StoreLocation store);
    }

    private List<StoreLocation> stores = new ArrayList<>();
    private List<StoreLocation> storesFiltered = new ArrayList<>();
    private OnStoreActionListener listener;

    public StoreLocationAdapter(OnStoreActionListener listener) {
        this.listener = listener;
    }

    public void setStores(List<StoreLocation> stores) {
        this.stores = new ArrayList<>(stores);
        this.storesFiltered = new ArrayList<>(stores);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        storesFiltered.clear();
        if (TextUtils.isEmpty(query)) {
            storesFiltered.addAll(stores);
        } else {
            String lowerQuery = query.toLowerCase();
            for (StoreLocation store : stores) {
                if (store.getName().toLowerCase().contains(lowerQuery) ||
                    (store.getAddress() != null && store.getAddress().toLowerCase().contains(lowerQuery))) {
                    storesFiltered.add(store);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreLocation store = storesFiltered.get(position);
        holder.bind(store, listener);
    }

    @Override
    public int getItemCount() {
        return storesFiltered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivStoreImage, btnEditStore, btnDeleteStore;
        private TextView tvStoreName, tvStoreAddress, tvStorePhone, tvStoreHours;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoreImage = itemView.findViewById(R.id.ivStoreImage);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvStoreAddress = itemView.findViewById(R.id.tvStoreAddress);
            tvStorePhone = itemView.findViewById(R.id.tvStorePhone);
            tvStoreHours = itemView.findViewById(R.id.tvStoreHours);
            btnEditStore = itemView.findViewById(R.id.btnEditStore);
            btnDeleteStore = itemView.findViewById(R.id.btnDeleteStore);
        }

        public void bind(StoreLocation store, OnStoreActionListener listener) {
            tvStoreName.setText(store.getName());
            tvStoreAddress.setText(store.getAddress() != null ? store.getAddress() : "No address");
            tvStorePhone.setText(!TextUtils.isEmpty(store.getPhone()) ? "📞 " + store.getPhone() : "No phone");
            tvStoreHours.setText(!TextUtils.isEmpty(store.getOpeningHours()) ? "🕒 " + store.getOpeningHours() : "Hours not set");

            RequestOptions imgOptions = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

            if (!TextUtils.isEmpty(store.getImageUrl())) {
                Glide.with(ivStoreImage.getContext())
                        .load(store.getImageUrl())
                        .apply(imgOptions)
                        .into(ivStoreImage);
            } else {
                ivStoreImage.setImageResource(R.drawable.ic_launcher_foreground);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStoreClick(store);
            });

            btnEditStore.setOnClickListener(v -> {
                if (listener != null) listener.onEditStore(store);
            });

            btnDeleteStore.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteStore(store);
            });
        }
    }
}




