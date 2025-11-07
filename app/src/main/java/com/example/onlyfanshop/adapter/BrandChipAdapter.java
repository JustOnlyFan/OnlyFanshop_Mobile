package com.example.onlyfanshop.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.BrandDTO;

import java.util.ArrayList;
import java.util.List;

public class BrandChipAdapter extends RecyclerView.Adapter<BrandChipAdapter.VH> {
    public interface Listener { void onBrandSelected(Integer brandId); void onSeeAll(); }

    private final List<BrandDTO> brands = new ArrayList<>();
    private final Listener listener;
    private RecyclerView recyclerView;

    public BrandChipAdapter(Listener listener) { this.listener = listener; }

    public void submitList(List<BrandDTO> items) {
        brands.clear();
        if (items != null) brands.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand_chip, parent, false);
        if (parent instanceof RecyclerView) {
            recyclerView = (RecyclerView) parent;
        }
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        // Set marginStart
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
        if (params != null) {
            int marginStart = position == 0 ? 16 : 4; // dp
            int marginEnd = 4; // dp
            float density = h.itemView.getContext().getResources().getDisplayMetrics().density;
            params.setMargins(
                    (int) (marginStart * density),
                    params.topMargin,
                    (int) (marginEnd * density),
                    params.bottomMargin
            );
            h.itemView.setLayoutParams(params);
        }

        // Width chia đều 4 ô
        h.itemView.post(() -> {
            RecyclerView rv = recyclerView != null ? recyclerView : findRecyclerView(h.itemView);
            if (rv != null && rv.getWidth() > 0) {
                int recyclerWidth = rv.getWidth();
                float density = h.itemView.getContext().getResources().getDisplayMetrics().density;
                int totalMargin = (int) (16 * density) + (int) (3 * 4 * density) + (int) (4 * 4 * density) + (int) (16 * density);
                int itemWidth = (recyclerWidth - totalMargin) / 4;
                ViewGroup.LayoutParams itemParams = h.itemView.getLayoutParams();
                if (itemParams != null) {
                    itemParams.width = itemWidth;
                    h.itemView.setLayoutParams(itemParams);
                }
            }
        });

        int maxDisplay = 3;
        boolean isBrandItem = position < maxDisplay && position < brands.size();

        if (isBrandItem) {
            BrandDTO b = brands.get(position);

            // Hiện logo, ẩn "Xem tất cả"
            h.logo.setVisibility(View.VISIBLE);
            h.seeAll.setVisibility(View.GONE);

            String imageUrl = b.getImageURL();
            android.util.Log.d("BrandChip", "Brand: " + b.getName() + ", ImageURL: " + imageUrl);

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
                if (listener != null) listener.onBrandSelected(b.getBrandID());
            });
        } else {
            // Ô "Xem tất cả"
            h.logo.setVisibility(View.GONE);
            h.seeAll.setVisibility(View.VISIBLE);
            h.seeAll.setText("Xem tất cả");

            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSeeAll();
            });
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(brands.size(), 3) + 1; // 3 brand + 1 "Xem tất cả"
    }

    private RecyclerView findRecyclerView(View view) {
        View parent = (View) view.getParent();
        while (parent != null) {
            if (parent instanceof RecyclerView) {
                return (RecyclerView) parent;
            }
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
        return null;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView seeAll;
        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imgLogo);
            seeAll = itemView.findViewById(R.id.tvSeeAll);
        }
    }
}