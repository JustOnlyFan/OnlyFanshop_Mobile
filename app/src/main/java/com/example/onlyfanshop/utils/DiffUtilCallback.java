package com.example.onlyfanshop.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.example.onlyfanshop.model.ProductDTO;

import java.util.List;

/**
 * DiffUtil callback để tối ưu RecyclerView updates
 * Giảm thiểu việc re-bind views không cần thiết
 */
public class DiffUtilCallback extends DiffUtil.ItemCallback<ProductDTO> {
    
    @Override
    public boolean areItemsTheSame(@NonNull ProductDTO oldItem, @NonNull ProductDTO newItem) {
        // So sánh ID - nếu cùng ID thì là cùng item
        return oldItem.getProductID() != null && 
               newItem.getProductID() != null && 
               oldItem.getProductID().equals(newItem.getProductID());
    }

    @Override
    public boolean areContentsTheSame(@NonNull ProductDTO oldItem, @NonNull ProductDTO newItem) {
        // So sánh nội dung - nếu giống nhau thì không cần update view
        return oldItem.getProductName() != null && 
               oldItem.getProductName().equals(newItem.getProductName()) &&
               oldItem.getPrice() != null && 
               oldItem.getPrice().equals(newItem.getPrice()) &&
               (oldItem.getImageURL() != null ? oldItem.getImageURL().equals(newItem.getImageURL()) : newItem.getImageURL() == null);
    }
}
