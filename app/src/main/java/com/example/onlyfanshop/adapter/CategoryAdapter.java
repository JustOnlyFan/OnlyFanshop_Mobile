package com.example.onlyfanshop.adapter;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.model.CategoryDTO;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryVH> {

    private final List<CategoryDTO> items = new ArrayList<>();
    @Nullable
    private Integer selectedId = null;

    public interface OnCategoryClickListener {
        void onClick(@Nullable Integer id, @NonNull String name);
    }

    private final OnCategoryClickListener listener;

    public CategoryAdapter(@NonNull OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<CategoryDTO> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    public void setSelectedId(@Nullable Integer id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final int padH = (int) (parent.getResources().getDisplayMetrics().density * 12);
        final int padV = (int) (parent.getResources().getDisplayMetrics().density * 8);

        android.widget.TextView tv = new android.widget.TextView(parent.getContext());
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // Margin sẽ được set trong onBindViewHolder để item đầu tiên có marginStart = 16dp
        lp.setMargins(0, padV, padH, padV);
        tv.setLayoutParams(lp);

        tv.setPadding(padH, padV, padH, padV);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);

        return new CategoryVH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryVH holder, int position) {
        CategoryDTO item = items.get(position);
        boolean isSelected = (item.getId() == null && selectedId == null)
                || (item.getId() != null && item.getId().equals(selectedId));
        holder.bind(item, isSelected);
        
        // Set marginStart: item đầu tiên = 16dp để thẳng với title, các item khác = 12dp
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (params != null) {
            final int padH = (int) (holder.itemView.getContext().getResources().getDisplayMetrics().density * 12);
            final int padV = (int) (holder.itemView.getContext().getResources().getDisplayMetrics().density * 8);
            int marginStart = position == 0 ? 16 : padH; // 16dp cho item đầu, 12dp cho các item khác
            params.setMargins(
                (int) (marginStart * holder.itemView.getContext().getResources().getDisplayMetrics().density),
                padV,
                padH,
                padV
            );
            holder.itemView.setLayoutParams(params);
        }
        
        holder.itemView.setOnClickListener(v -> {
            selectedId = item.getId();
            notifyDataSetChanged();
            listener.onClick(item.getId(), item.getName() != null ? item.getName() : "");
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CategoryVH extends RecyclerView.ViewHolder {
        private final android.widget.TextView tv;

        public CategoryVH(@NonNull View itemView) {
            super(itemView);
            tv = (android.widget.TextView) itemView;
        }

        void bind(@NonNull CategoryDTO item, boolean selected) {
            tv.setText(item.getName() != null ? item.getName() : "");

            float radius = tv.getResources().getDisplayMetrics().density * 16;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(radius);
            if (selected) {
                bg.setColor(0xFF212121);
                tv.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0xFFEFEFEF);
                tv.setTextColor(0xFF212121);
            }
            tv.setBackground(bg);
        }
    }
}