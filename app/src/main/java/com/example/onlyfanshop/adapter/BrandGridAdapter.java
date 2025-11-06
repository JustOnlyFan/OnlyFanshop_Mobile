package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.BrandDTO;

import java.util.ArrayList;
import java.util.List;

public class BrandGridAdapter extends RecyclerView.Adapter<BrandGridAdapter.VH> {

    public interface Listener {
        void onClick(BrandDTO brand);
    }

    private final List<BrandDTO> data = new ArrayList<>();
    private final Listener listener;

    public BrandGridAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<BrandDTO> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BrandDTO b = data.get(position);
        String imageUrl = b.getImageURL();
        h.logo.setVisibility(View.VISIBLE);
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(h.logo.getContext())
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .fitCenter()
                    .into(h.logo);
        } else {
            h.logo.setImageResource(R.drawable.ic_launcher_foreground);
            h.logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(b);
        });

        // Xóa mọi tinh chỉnh margin/width đặc thù của strip; để GridLayoutManager tự chia đều
        ViewGroup.LayoutParams params = h.itemView.getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            h.itemView.setLayoutParams(params);
        }
        if (h.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
            mlp.setMargins(
                    (int) (4 * h.itemView.getResources().getDisplayMetrics().density),
                    mlp.topMargin,
                    (int) (4 * h.itemView.getResources().getDisplayMetrics().density),
                    mlp.bottomMargin
            );
            h.itemView.setLayoutParams(mlp);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView logo;
        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imgLogo);
        }
    }
}