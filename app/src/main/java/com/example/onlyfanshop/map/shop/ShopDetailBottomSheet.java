package com.example.onlyfanshop.map.shop;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.onlyfanshop.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ShopDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SHOP = "arg_shop";

    public static ShopDetailBottomSheet newInstance(Shop shop) {
        ShopDetailBottomSheet f = new ShopDetailBottomSheet();
        Bundle b = new Bundle();
        b.putParcelable(ARG_SHOP, shop);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_shop_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Shop shop = getArguments() != null ? getArguments().getParcelable(ARG_SHOP) : null;
        if (shop == null) {
            dismissAllowingStateLoss();
            return;
        }

        ImageView ivCover = v.findViewById(R.id.ivCover);
        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvAddress = v.findViewById(R.id.tvAddress);
        TextView tvDesc = v.findViewById(R.id.tvDesc);
        TextView tvPhone = v.findViewById(R.id.tvPhone);
        TextView tvHours = v.findViewById(R.id.tvHours);

        tvName.setText(shop.getName());
        tvAddress.setText(shop.getAddress());
        tvDesc.setText(shop.getDescription());
        tvPhone.setText(TextUtils.isEmpty(shop.getPhone()) ? "—" : shop.getPhone());
        tvHours.setText(TextUtils.isEmpty(shop.getOpeningHours()) ? "—" : shop.getOpeningHours());

        RequestOptions imgOptions = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground) // ảnh tạm trong lúc tải
                .error(R.drawable.ic_launcher_foreground)       // ảnh fallback khi lỗi
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

        if (!TextUtils.isEmpty(shop.getImageUrl())) {
            Glide.with(ivCover)
                    .load(shop.getImageUrl())
                    .apply(imgOptions)
                    .into(ivCover);
        } else {
            ivCover.setImageResource(R.drawable.ic_launcher_foreground);
        }

        v.findViewById(R.id.btnClose).setOnClickListener(view -> dismiss());
    }
}