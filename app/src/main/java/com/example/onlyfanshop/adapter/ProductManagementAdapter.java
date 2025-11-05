package com.example.onlyfanshop.adapter;

import static com.example.onlyfanshop.adapter.ProductAdapter.asGlideUrlWithAuth;
import static com.example.onlyfanshop.adapter.ProductAdapter.resolveImageUrl;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.ProductDTO;

import java.util.List;

public class ProductManagementAdapter extends RecyclerView.Adapter<ProductManagementAdapter.ProductViewHolder>{
    private List<ProductDTO> productList;
    private OnProductActionListener listener;
    private static final String BASE_IMAGE_HOST = "http://10.0.2.2:8080";

    // Bật nếu ảnh nằm sau auth và cần gắn Bearer token
    private static final boolean IMAGES_REQUIRE_AUTH = false;
    public interface OnProductActionListener {
        void onEdit(ProductDTO product);
        void onDelete(ProductDTO product);
        void onView(ProductDTO product);
        void onToggleActive(ProductDTO product, boolean isActive);

    }

    public ProductManagementAdapter(List<ProductDTO> productList, OnProductActionListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_admin, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductDTO product = productList.get(position);
        holder.tvName.setText(product.getProductName());
        holder.tvPrice.setText("Giá: " + product.getPrice() + "đ");
        holder.tvCategory.setText("Loại: " + product.getCategory().getCategoryName());
        holder.tvBrand.setText("Hãng: " + product.getBrand().getName());
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(product));
//        holder.btnDelete.setOnClickListener(v -> listener.onDelete(product));
        holder.btnView.setOnClickListener(v->listener.onView(product));
        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(product.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onToggleActive(product, product.isActive());
        });
        String rawUrl = product.getImageURL();
        String url = resolveImageUrl(rawUrl);


        if (url != null && !url.isEmpty()) {
            Object model = url;

            // Nếu ảnh yêu cầu token, bật cờ IMAGES_REQUIRE_AUTH = true
            if (IMAGES_REQUIRE_AUTH) {
                model = asGlideUrlWithAuth(holder.itemView.getContext(), url);
            }

            Glide.with(holder.tvImg.getContext())
                    .load(model)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.tvImg);
        } else {
            holder.tvImg.setImageResource(R.drawable.ic_launcher_foreground);
        }


    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setFilteredList(List<ProductDTO> filteredList) {
        this.productList = filteredList;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvCategory, tvBrand;
        Button btnEdit, btnDelete, btnView;

        ImageView tvImg;
        Switch switchActive;
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvBrand = itemView.findViewById(R.id.tvProductBrand);
            btnEdit = itemView.findViewById(R.id.btnEditProduct);
//            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
            btnView = itemView.findViewById(R.id.btnViewProduct);
            tvImg = itemView.findViewById(R.id.tvImg);
            switchActive = itemView.findViewById(R.id.switchActive);
        }
    }
}
