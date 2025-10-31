package com.example.onlyfanshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.databinding.ViewholderBrandBinding;
import com.example.onlyfanshop.model.BrandManagementDTO;

import java.util.List;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {
    private Context context;
    private List<BrandManagementDTO> listBrand;
    private OnEditBrandListener listener;

    public BrandAdapter(Context context, List<BrandManagementDTO> listBrand) {
        this.context = context;
        this.listBrand = listBrand;
    }

    public void setOnEditBrandListener(OnEditBrandListener listener) {
        this.listener = listener;
    }


    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderBrandBinding binding = ViewholderBrandBinding.inflate(
                LayoutInflater.from(context),parent,false);
        return new BrandViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        BrandManagementDTO brand = listBrand.get(position);
        holder.binding.brandName.setText(brand.getName());
        holder.binding.brandCountry.setText(brand.getCountry());
        holder.binding.brandDes.setText(brand.getDescription());
        holder.binding.switchActive.setChecked(brand.isActive());
        holder.binding.btnEdit.setOnClickListener(v -> {
            holder.binding.brandName.setVisibility(View.INVISIBLE);
            holder.binding.brandDes.setVisibility(View.INVISIBLE);
            holder.binding.brandCountry.setVisibility(View.INVISIBLE);
            holder.binding.edtName.setVisibility(View.VISIBLE);
            holder.binding.edtDes.setVisibility(View.VISIBLE);
            holder.binding.edtCountry.setVisibility(View.VISIBLE);
            holder.binding.btnConfirmEdit.setVisibility(View.VISIBLE);
            holder.binding.btnEdit.setVisibility(View.INVISIBLE);
            holder.binding.edtName.setText(brand.getName());
            holder.binding.edtDes.setText(brand.getDescription());
            holder.binding.edtCountry.setText(brand.getCountry());
        });
        holder.binding.btnConfirmEdit.setOnClickListener(v -> {
            String newName = holder.binding.edtName.getText().toString();
            String newDes = holder.binding.edtDes.getText().toString();
            String newCountry = holder.binding.edtCountry.getText().toString();
            if (!newName.isEmpty() && !newDes.isEmpty() && !newCountry.isEmpty()) {
                brand.setName(newName);
                brand.setDescription(newDes);
                brand.setCountry(newCountry);
                if (listener != null) listener.onEdit(brand);
                holder.binding.brandName.setVisibility(View.VISIBLE);
                holder.binding.brandDes.setVisibility(View.VISIBLE);
                holder.binding.brandCountry.setVisibility(View.VISIBLE);
                holder.binding.edtName.setVisibility(View.GONE);
                holder.binding.edtDes.setVisibility(View.GONE);
                holder.binding.edtCountry.setVisibility(View.GONE);
                holder.binding.btnConfirmEdit.setVisibility(View.INVISIBLE);
                holder.binding.btnEdit.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }

        });
        holder.binding.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onSwitchActive(brand.getBrandID(), isChecked);
        });


    }

    @Override
    public int getItemCount() {
        return listBrand != null ? listBrand.size() : 0;
    }

    public void setData(List<BrandManagementDTO> list){
        if(listBrand != null){
            listBrand.clear();
        }
        this.listBrand = list;
        notifyDataSetChanged();
    }

    public static class BrandViewHolder extends RecyclerView.ViewHolder{
        ViewholderBrandBinding binding;
        public BrandViewHolder(ViewholderBrandBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnEditBrandListener {
        void onEdit(BrandManagementDTO brand);
        void onSwitchActive(Integer brandID, boolean isActive);
    }
}
