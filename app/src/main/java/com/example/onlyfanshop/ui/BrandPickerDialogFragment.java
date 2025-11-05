package com.example.onlyfanshop.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.BrandGridAdapter;
import com.example.onlyfanshop.model.BrandDTO;

import java.util.ArrayList;
import java.util.List;

public class BrandPickerDialogFragment extends DialogFragment {

    public interface OnBrandSelectedListener {
        void onBrandSelected(@Nullable Integer brandId);
    }

    private final List<BrandDTO> brands = new ArrayList<>();
    private OnBrandSelectedListener listener;

    public static BrandPickerDialogFragment newInstance() {
        return new BrandPickerDialogFragment();
    }

    public void setBrandList(@Nullable List<BrandDTO> list) {
        brands.clear();
        if (list != null) brands.addAll(list);
        // Nếu view đã tạo, cập nhật ngay
        if (getView() != null) {
            RecyclerView rv = getView().findViewById(R.id.rvAllBrands);
            if (rv != null && rv.getAdapter() instanceof BrandGridAdapter) {
                ((BrandGridAdapter) rv.getAdapter()).submitList(brands);
            }
        }
    }

    public void setOnBrandSelectedListener(OnBrandSelectedListener l) {
        this.listener = l;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // Set popup giữa màn hình, rộng ~90% màn hình
            DisplayMetrics dm = requireContext().getResources().getDisplayMetrics();
            int width = (int) (dm.widthPixels * 0.9f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_brand_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Close button
        View btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(view -> dismissAllowingStateLoss());

        RecyclerView rv = v.findViewById(R.id.rvAllBrands);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rv.setHasFixedSize(true);
        rv.setItemAnimator(null);

        BrandGridAdapter adapter = new BrandGridAdapter(b -> {
            if (listener != null) listener.onBrandSelected(b != null ? b.getBrandID() : null);
            dismissAllowingStateLoss();
        });
        rv.setAdapter(adapter);
        adapter.submitList(brands);
    }
}