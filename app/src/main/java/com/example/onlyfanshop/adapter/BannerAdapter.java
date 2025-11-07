package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.BannerModel;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private final ArrayList<BannerModel> sliderItems = new ArrayList<>();
    private final ViewPager2 viewPager2; // vẫn giữ tham chiếu nếu bạn muốn dùng hiệu ứng đặc biệt
    private Context context;

    public BannerAdapter(ArrayList<BannerModel> sliderItems, ViewPager2 viewPager2) {
        this.viewPager2 = viewPager2;
        if (sliderItems != null) this.sliderItems.addAll(sliderItems);
    }

    // Cho phép cập nhật lại danh sách banner
    public void submit(List<BannerModel> items) {
        this.sliderItems.clear();
        if (items != null) this.sliderItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerAdapter.BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.banner_item, parent, false);
        return new BannerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerAdapter.BannerViewHolder holder, int position) {
        holder.bind(sliderItems.get(position));
        // Không nhân đôi list nữa, để Fragment tự auto-slide và reset về 0 khi hết
    }

    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageSlider);
        }
        public void bind(BannerModel item) {
            // Tối ưu banner loading với cache và thumbnail
            Glide.with(itemView.getContext())
                    .load(item.getUrl())
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .thumbnail(Glide.with(itemView.getContext())
                            .load(item.getUrl())
                            .override(200, 200))
                    .into(imageView);
        }
    }
}