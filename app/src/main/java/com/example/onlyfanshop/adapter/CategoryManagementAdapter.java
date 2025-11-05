package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.databinding.ViewholderCategoryManagementBinding;
import com.example.onlyfanshop.model.CategoryManagementDTO;

import java.util.List;

public class CategoryManagementAdapter extends RecyclerView.Adapter<CategoryManagementAdapter.CategoryManagementViewHolder> {
    private Context context;
    private List<CategoryManagementDTO> listCategory;
    private OnEditCategoryListener listener;

    public CategoryManagementAdapter(Context context, List<CategoryManagementDTO> listCategory) {
        this.context = context;
        this.listCategory = listCategory;
    }

    public void setOnEditCategoryListener(OnEditCategoryListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryManagementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderCategoryManagementBinding binding = ViewholderCategoryManagementBinding.inflate(
                LayoutInflater.from(context),parent,false);
        return new CategoryManagementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryManagementViewHolder holder, int position) {
        CategoryManagementDTO category = listCategory.get(position);
        holder.binding.switchActive.setOnCheckedChangeListener(null);

        holder.binding.switchActive.setChecked(category.isActive());

        holder.binding.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onSwitchActive(category.getCategoryID(), isChecked);
        });
        holder.binding.categoryName.setText(category.getCategoryName());
        holder.binding.edtCategoryName.setOnClickListener(v -> {
            holder.binding.categoryName.setVisibility(View.GONE);
            holder.binding.edtCategoryName.setVisibility(View.GONE);
            holder.binding.confirmEdit.setVisibility(View.VISIBLE);
            holder.binding.edtName.setVisibility(View.VISIBLE);
            holder.binding.edtName.setText(category.getCategoryName());
        });
        holder.binding.confirmEdit.setOnClickListener(v -> {
            String newInfor = holder.binding.edtName.getText().toString();
            if (!newInfor.isEmpty()) {
                category.setCategoryName(newInfor);
                listener.onEdit(category);
                holder.binding.categoryName.setVisibility(View.VISIBLE);
                holder.binding.edtCategoryName.setVisibility(View.VISIBLE);
                holder.binding.confirmEdit.setVisibility(View.GONE);
                holder.binding.edtName.setVisibility(View.GONE);
            } else {
                Toast.makeText(context, "Vui lòng không để trống", Toast.LENGTH_SHORT).show();
            }
        });
        holder.binding.delete.setOnClickListener(v -> {
            listener.onDelete(category.getCategoryID());
        });



    }

    @Override
    public int getItemCount() {
        if (listCategory != null) {
            return listCategory.size();
        }
        return 0;
    }

    public void setData(List<CategoryManagementDTO> list){
        if(listCategory != null){
            listCategory.clear();
        }
        this.listCategory = list;
        notifyDataSetChanged();
    }

    public static class CategoryManagementViewHolder extends RecyclerView.ViewHolder {
        ViewholderCategoryManagementBinding binding;
        public CategoryManagementViewHolder(ViewholderCategoryManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnEditCategoryListener {
        void onEdit(CategoryManagementDTO category);
        void onDelete(Integer categoryID);
        void onSwitchActive(Integer categoryID, boolean isActive);

    }
}
