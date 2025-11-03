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
import com.example.onlyfanshop.model.ProductDTO;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.VH> {

    public interface OnItemClick {
        void onClick(ProductDTO item);
    }

    private final List<ProductDTO> items = new ArrayList<>();
    private final OnItemClick onItemClick;

    public SearchSuggestionAdapter(OnItemClick onItemClick) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_search_suggestion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProductDTO p = items.get(position);
        h.title.setText(p.getProductName() != null ? p.getProductName() : "Unnamed");
        if (p.getPrice() != null) {
            h.price.setText(formatCurrencyVND(p.getPrice()));
        } else {
            h.price.setText("");
        }

        String url = p.getImageURL();
        Glide.with(h.thumb.getContext())
                .load(url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.thumb);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, price;
        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.imgThumb);
            title = itemView.findViewById(R.id.tvTitle);
            price = itemView.findViewById(R.id.tvPrice);
        }
    }
    
    private String formatCurrencyVND(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value).replace("₫", "₫");
    }
}