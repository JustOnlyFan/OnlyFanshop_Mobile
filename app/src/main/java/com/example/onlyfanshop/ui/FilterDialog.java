package com.example.onlyfanshop.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.onlyfanshop.R;

public class FilterDialog extends Dialog {
    public String brand = "";
    public Integer priceMin = null, priceMax = null, rating = null;
    private final OnFilterApplyListener listener;

    public interface OnFilterApplyListener {
        void onApply(String brand, Integer priceMin, Integer priceMax, Integer rating);
    }

    public FilterDialog(Context context, OnFilterApplyListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter);

        EditText etBrand = findViewById(R.id.etBrand);
        EditText etPriceMin = findViewById(R.id.etPriceMin);
        EditText etPriceMax = findViewById(R.id.etPriceMax);
        EditText etRating = findViewById(R.id.etRating);
        Button btnApply = findViewById(R.id.btnApplyFilter);

        btnApply.setOnClickListener(v -> {
            String brandText = etBrand.getText().toString().trim();
            String priceMinText = etPriceMin.getText().toString().trim();
            String priceMaxText = etPriceMax.getText().toString().trim();
            String ratingText = etRating.getText().toString().trim();

            Integer min = priceMinText.isEmpty() ? null : Integer.valueOf(priceMinText);
            Integer max = priceMaxText.isEmpty() ? null : Integer.valueOf(priceMaxText);
            Integer rat = ratingText.isEmpty() ? null : Integer.valueOf(ratingText);

            listener.onApply(brandText, min, max, rat);
            dismiss();
        });
    }
}