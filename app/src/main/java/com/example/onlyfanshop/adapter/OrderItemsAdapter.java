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
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.ProductDTO;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.VH> {

    private final List<CartItemDTO> items = new ArrayList<>();

    public void submitList(List<CartItemDTO> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CartItemDTO it = items.get(position);
        ProductDTO p = it.getProductDTO();
        Context ctx = h.itemView.getContext();

        h.tvProductName.setText(p != null ? p.getProductName() : "");
        int qty = it.getQuantity() != null ? it.getQuantity() : 0;
        // API provides price as line total; derive unit price for display
        double line = it.getPrice() != null ? it.getPrice() : 0d;
        double unit = qty > 0 ? (line / qty) : line;
        h.tvQuantity.setText("x" + qty);
        // Keep only one price visible (requested to remove red one)
        h.tvUnitPrice.setText(formatCurrency(unit));
        h.tvLineTotal.setVisibility(View.GONE);

        String url = p != null ? resolveImageUrl(p.getImageURL()) : null;
        Object model = buildGlideModel(ctx, url);
        Glide.with(ctx)
                .load(model)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.imgProduct);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imgProduct;
        final TextView tvProductName;
        final TextView tvQuantity;
        final TextView tvUnitPrice;
        final TextView tvLineTotal;
        VH(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvLineTotal = itemView.findViewById(R.id.tvLineTotal);
        }
    }

    private static String resolveImageUrl(String raw) {
        if (raw == null) return null;
        String u = raw.trim();
        if (u.isEmpty()) return null;
        u = u.replace("http://localhost:", "http://10.0.2.2:")
                .replace("http://127.0.0.1:", "http://10.0.2.2:");
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("/")) return "http://10.0.2.2:8080" + u;
        return "http://10.0.2.2:8080/" + u;
    }

    private static Object buildGlideModel(Context ctx, String url) {
        if (url == null) return null;
        SharedPreferences prefs = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        if (token == null || token.isEmpty()) return url;
        LazyHeaders headers = new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        return new GlideUrl(url, headers);
    }

    private static String formatCurrency(double v) {
        return NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(v);
    }
}








