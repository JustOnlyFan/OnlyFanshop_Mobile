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
        // Set marginStart: item đầu tiên = 16dp để thẳng với title, các item khác = 4dp
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
        if (params != null) {
            int marginStart = position == 0 ? 16 : 4; // 16dp cho item đầu, 4dp cho các item khác
            int marginEnd = 4; // Giữ marginEnd = 4dp
            params.setMargins(
                (int) (marginStart * h.itemView.getContext().getResources().getDisplayMetrics().density),
                params.topMargin,
                (int) (marginEnd * h.itemView.getContext().getResources().getDisplayMetrics().density),
                params.bottomMargin
            );
            h.itemView.setLayoutParams(params);
        }
        
        // Set width đều nhau cho 4 button (mỗi button = recycler width / 4)
        h.itemView.post(() -> {
            RecyclerView rv = recyclerView != null ? recyclerView : findRecyclerView(h.itemView);
            if (rv != null && rv.getWidth() > 0) {
                int recyclerWidth = rv.getWidth();
                // Item đầu: 16dp marginStart, các item khác: 4dp marginStart, tất cả: 4dp marginEnd, paddingEnd: 16dp
                // Total: 16 (item đầu) + 12 (3 items * 4dp) + 16 (4 items * 4dp marginEnd) + 16 (paddingEnd) = 60dp
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

        int maxDisplay = 3; // Chỉ hiển thị 3 brand đầu
        if (position < maxDisplay && position < brands.size()) {
            BrandDTO b = brands.get(position);
            
            // Load logo - đảm bảo luôn hiển thị logo
            h.logo.setVisibility(View.VISIBLE);
            String imageUrl = b.getImageURL();
            
            // Debug: log để kiểm tra imageURL
            android.util.Log.d("BrandChip", "Brand: " + b.getName() + ", ImageURL: " + imageUrl);
            
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Có imageURL - load từ URL
                Glide.with(h.logo.getContext())
                        .load(imageUrl.trim())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .fitCenter()
                        .into(h.logo);
            } else {
                // Không có imageURL - hiển thị placeholder
                h.logo.setImageResource(R.drawable.ic_launcher_foreground);
                h.logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            }
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBrandSelected(b.getBrandID());
            });
        } else {
            // See all item (ô cuối cùng) - hiển thị icon mũi tên hoặc grid
            h.logo.setVisibility(View.VISIBLE);
            // Có thể dùng icon đặc biệt cho "Xem hết" - tạm thời dùng placeholder
            h.logo.setImageResource(R.drawable.ic_launcher_foreground);
            h.logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            h.itemView.setOnClickListener(v -> { 
                if (listener != null) listener.onSeeAll(); 
            });
        }
    }

    @Override
    public int getItemCount() {
        // Tối đa 3 brand + 1 ô "Xem tất cả" = 4 ô
        return Math.min(brands.size(), 3) + 1;
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
        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imgLogo);
        }
    }
}


