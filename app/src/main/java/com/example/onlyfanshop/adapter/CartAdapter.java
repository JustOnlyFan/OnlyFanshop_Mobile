package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.databinding.ViewholderCartBinding;
import com.example.onlyfanshop.model.CartItemDTO;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewholder> {

    // Static NumberFormat để tránh tạo object mới mỗi lần
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private Context context;
    private List<CartItemDTO> cartItems;
    private List<CartItemDTO> subList;
    private OnQuantityChangeListener listener;
    private OnCheckItem checkListener;
    private boolean cartView;

    public CartAdapter(Context context,List<CartItemDTO> cartItems,boolean cartView) {
        this.context = context;

        this.cartItems = cartItems;
        this.cartView = cartView;
    }
    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }
    public void setOnCheckItem(OnCheckItem checkListener) {
        this.checkListener = checkListener;
    }



    @NonNull
    @Override
    public CartViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderCartBinding binding = ViewholderCartBinding.inflate(
                LayoutInflater.from(context),parent,false);
        return new CartViewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewholder holder, int position) {

            CartItemDTO item = cartItems.get(position);
            holder.binding.checkBox.setChecked(item.isChecked());
            holder.binding.productName.setText(item.getProductDTO().getProductName());
            holder.binding.numberItem.setText(item.getQuantity()+"");
            holder.binding.totalEach.setText(formatCurrencyVND(item.getPrice()));
        if (item.getProductDTO().getImageURL() != null && !item.getProductDTO().getImageURL().isEmpty()) {
            // Tối ưu: thumbnail để load nhanh hơn
            Glide.with(holder.itemView.getContext())
                    .load(item.getProductDTO().getImageURL())
                    .thumbnail(Glide.with(holder.itemView.getContext())
                            .load(item.getProductDTO().getImageURL())
                            .override(80, 80)) // Load thumbnail nhỏ trước
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(holder.binding.pic);
        }
        if(!cartView){
            holder.binding.addQuantity.setVisibility(View.GONE);
            holder.binding.minusQuantity.setVisibility(View.GONE);
            holder.binding.checkBox.setVisibility(View.GONE);
        }

        holder.binding.addQuantity.setOnClickListener(v -> {
            if (listener != null) listener.onIncrease(item.getProductDTO().getProductID());
        });
        holder.binding.minusQuantity.setOnClickListener(v -> {
            if (listener != null) listener.onDecrease(item.getProductDTO().getProductID());
        });
        holder.binding.checkBox.setOnClickListener(v -> {
            if (checkListener != null) checkListener.onCheckItem(item);
        });

    }

    @Override
    public int getItemCount() {
        int count=0;
        if (cartItems!=null){
            count = cartItems.size();
        }
        return count;
    }
    public void setData (List<CartItemDTO> list){
        if(cartItems != null){
            cartItems.clear();
        }
        this.cartItems = list;
        notifyDataSetChanged();
    }
    public static class CartViewholder extends RecyclerView.ViewHolder {
        ViewholderCartBinding binding;
        public CartViewholder(ViewholderCartBinding binding ) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnCheckItem{
        void onCheckItem(CartItemDTO cartItem);
    }

    public interface OnQuantityChangeListener {
        void onIncrease(int productId);
        void onDecrease(int productId);
    }
    
    // Tối ưu: dùng static NumberFormat để tránh tạo object mới mỗi lần
    private static String formatCurrencyVND(double value) {
        synchronized (CURRENCY_FORMATTER) {
            return CURRENCY_FORMATTER.format(value).replace("₫", "₫");
        }
    }

}
