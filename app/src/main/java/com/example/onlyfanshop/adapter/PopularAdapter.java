package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.databinding.ViewholderPopularBinding;
import com.example.onlyfanshop.model.ProductDTO;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PopularAdapter extends RecyclerView.Adapter<PopularAdapter.VH> {

    public interface OnItemClick {
        void onClick(@NonNull ProductDTO item);
    }

    private final List<ProductDTO> items = new ArrayList<>();
    private final OnItemClick onItemClick;
    private Context context;

    // Nếu server trả ảnh tương đối, thêm host ở đây
    private static final String BASE_IMAGE_HOST = "http://10.0.2.2:8080";
    // Nếu ảnh yêu cầu Bearer token → bật true
    private static final boolean IMAGES_REQUIRE_AUTH = false;
    // Static NumberFormat để tránh tạo object mới mỗi lần
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final String DEFAULT_LOCATION = "Vietnam";
    private static final String DEFAULT_SOLD = "Sold 0";

    public PopularAdapter(@NonNull OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submitList(List<ProductDTO> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderPopularBinding binding = ViewholderPopularBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProductDTO p = items.get(position);

        // Bind text
        h.b.textTitle.setText(p.getProductName() != null ? p.getProductName() : "");
        h.b.textPrice.setText(formatCurrencyVND(p.getPrice()));
        h.b.textSold.setText(DEFAULT_SOLD);
        h.b.textLocation.setText(DEFAULT_LOCATION);

        // Load ảnh theo đúng XML: centerCrop, height cố định đã set ở layout
        String url = resolveImageUrl(p.getImageURL());
        Object model = url;
        if (IMAGES_REQUIRE_AUTH && url != null) {
            model = asGlideUrlWithAuth(context, url);
        }

        // Tối ưu: thumbnail để load nhanh hơn, placeholder để UX tốt hơn
        Glide.with(h.b.imageProduct.getContext())
                .load(model)
                .thumbnail(Glide.with(h.b.imageProduct.getContext())
                        .load(model)
                        .override(100, 100)) // Load thumbnail nhỏ trước
                .apply(new RequestOptions().transform(new CenterCrop()))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.b.imageProduct);

        h.itemView.setOnClickListener(v -> onItemClick.onClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ViewholderPopularBinding b;
        VH(@NonNull ViewholderPopularBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    // Tối ưu: dùng static NumberFormat để tránh tạo object mới mỗi lần
    private static String formatCurrencyVND(Double value) {
        double v = value != null ? value : 0d;
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(v);
        }
    }

    private String resolveImageUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String u = raw.trim();
        // Đổi localhost → 10.0.2.2 cho emulator
        u = u.replace("http://localhost:", "http://10.0.2.2:")
                .replace("http://127.0.0.1:", "http://10.0.2.2:");
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("/")) return BASE_IMAGE_HOST + u;
        return BASE_IMAGE_HOST + "/" + u;
    }

    private GlideUrl asGlideUrlWithAuth(Context ctx, String url) {
        SharedPreferences prefs = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        LazyHeaders.Builder headers = new LazyHeaders.Builder();
        if (token != null && !token.isEmpty()) {
            headers.addHeader("Authorization", "Bearer " + token);
        }
        return new GlideUrl(url, headers.build());
    }
}