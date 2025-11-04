package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.ProductDTO;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnItemClick {
        void onClick(@NonNull ProductDTO item);
    }

    private final List<ProductDTO> items = new ArrayList<>();
    private final OnItemClick onItemClick;

    // Cập nhật host này cho đúng backend của bạn (không có slash ở cuối)
    private static final String BASE_IMAGE_HOST = "http://10.0.2.2:8080";

    // Bật nếu ảnh nằm sau auth và cần gắn Bearer token
    private static final boolean IMAGES_REQUIRE_AUTH = false;

    // Static NumberFormat để tránh tạo object mới mỗi lần
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final String DEFAULT_LOCATION = "Việt Nam";

    public ProductAdapter(@NonNull OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submitList(List<ProductDTO> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProductDTO p = items.get(position);
        h.textTitle.setText(p.getProductName());
        h.textPrice.setText(formatCurrencyVND(p.getPrice()));
        h.textSold.setText(h.itemView.getContext().getString(R.string.sold_format, 0));
        h.textLocation.setText(DEFAULT_LOCATION);

        String rawUrl = p.getImageURL();
        String url = resolveImageUrl(rawUrl);

        if (url != null && !url.isEmpty()) {
            Object model = url;

            // Nếu ảnh yêu cầu token, bật cờ IMAGES_REQUIRE_AUTH = true
            if (IMAGES_REQUIRE_AUTH) {
                model = asGlideUrlWithAuth(h.itemView.getContext(), url);
            }

            // Tối ưu: thumbnail để load nhanh hơn, placeholder để UX tốt hơn
            Glide.with(h.image.getContext())
                    .load(model)
                    .thumbnail(Glide.with(h.image.getContext())
                            .load(model)
                            .override(100, 100)) // Load thumbnail nhỏ trước
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(h.image);
        } else {
            h.image.setImageResource(R.drawable.ic_launcher_foreground);
        }

        h.itemView.setOnClickListener(v -> onItemClick.onClick(p));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView textTitle, textPrice, textSold, textLocation;
        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageProduct);
            textTitle = itemView.findViewById(R.id.textTitle);
            textPrice = itemView.findViewById(R.id.textPrice);
            textSold = itemView.findViewById(R.id.textSold);
            textLocation = itemView.findViewById(R.id.textLocation);
        }
    }

    // Tối ưu: dùng static NumberFormat để tránh tạo object mới mỗi lần
    private static String formatCurrencyVND(double value) {
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(value).replace("₫", "₫");
        }
    }

    // Chuẩn hóa URL ảnh: nếu tương đối -> thêm host
    public static String resolveImageUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String u = raw.trim();

        // Trường hợp backend trả "localhost" hoặc "127.0.0.1" -> đổi sang 10.0.2.2 cho emulator
        // Tối ưu: dùng StringBuilder để tránh tạo nhiều String object
        if (u.contains("localhost:") || u.contains("127.0.0.1:")) {
            StringBuilder sb = new StringBuilder(u);
            int idx;
            if ((idx = sb.indexOf("localhost:")) != -1) {
                sb.replace(idx, idx + "localhost:".length(), "10.0.2.2:");
            }
            if ((idx = sb.indexOf("127.0.0.1:")) != -1) {
                sb.replace(idx, idx + "127.0.0.1:".length(), "10.0.2.2:");
            }
            u = sb.toString();
        }

        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        if (u.startsWith("/")) {
            return BASE_IMAGE_HOST + u;
        }
        return BASE_IMAGE_HOST + "/" + u;
    }

    // Tạo GlideUrl kèm Authorization header từ SharedPreferences
    public static GlideUrl asGlideUrlWithAuth(Context ctx, String url) {
        SharedPreferences prefs = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        LazyHeaders.Builder headers = new LazyHeaders.Builder();
        if (token != null && !token.isEmpty()) {
            headers.addHeader("Authorization", "Bearer " + token);
        }
        return new GlideUrl(url, headers.build());
    }
}