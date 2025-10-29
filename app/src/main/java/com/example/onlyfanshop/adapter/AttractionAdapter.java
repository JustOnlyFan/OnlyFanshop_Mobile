package com.example.onlyfanshop.adapter;

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
import com.example.onlyfanshop.model.Attraction;

import java.util.ArrayList;
import java.util.List;

public class AttractionAdapter extends RecyclerView.Adapter<AttractionAdapter.VH> {

    public interface OnAttractionClickListener {
        void onAttractionClick(Attraction attraction);
        void onDirectionsClick(Attraction attraction);
    }

    private final List<Attraction> items;
    private final OnAttractionClickListener listener;

    public AttractionAdapter(@NonNull List<Attraction> items,
                             @NonNull OnAttractionClickListener listener) {
        // Don't use reference to external list - create new list
        this.items = new ArrayList<>(items);
        this.listener = listener;
        android.util.Log.d("AttractionAdapter", "Constructor - created with " + this.items.size() + " items");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attraction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Attraction a = items.get(position);
        h.tvTitle.setText(a.getTitle());
        h.tvDescription.setText(a.getDescription());

        // Load ảnh cửa hàng
        Glide.with(h.imgAttraction)
                .load(a.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(h.imgAttraction);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAttractionClick(a);
        });

        h.btnDirections.setOnClickListener(v -> {
            if (listener != null) listener.onDirectionsClick(a);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // Hỗ trợ CarouselController (nếu bạn dùng phiên bản gọi adapter.getItemAt)
    public Attraction getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return items.get(position);
    }
    
    // Update data và notify adapter
    public void updateData(List<Attraction> newData) {
        android.util.Log.d("AttractionAdapter", "updateData called with " + (newData != null ? newData.size() : "null") + " items");
        items.clear();
        if (newData != null && !newData.isEmpty()) {
            items.addAll(newData);
            android.util.Log.d("AttractionAdapter", "Added " + items.size() + " items to adapter");
        }
        notifyDataSetChanged();
        android.util.Log.d("AttractionAdapter", "notifyDataSetChanged called - final count: " + getItemCount());
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAttraction;
        TextView tvTitle;
        TextView tvDescription;
        Button btnDirections;

        VH(@NonNull View itemView) {
            super(itemView);
            imgAttraction = itemView.findViewById(R.id.imgAttraction);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnDirections = itemView.findViewById(R.id.btnDirections);
        }
    }
}






