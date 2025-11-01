package com.example.onlyfanshop.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(String priceSort, Integer brandId, Integer categoryId, Float priceMin, Float priceMax);
        void onFilterReset();
    }

    private FilterListener listener;
    private String currentPriceSort = "None"; // None, ASC, DESC
    private Integer currentBrandId = null;
    private Integer currentCategoryId = null;
    private Float currentPriceMin = 0f;
    private Float currentPriceMax = 195000000f;
    private List<BrandDTO> brandList = new ArrayList<>();
    private List<CategoryDTO> categoryList = new ArrayList<>();

    private RangeSlider rangeSliderPrice;
    private TextView tvPriceMin;
    private TextView tvPriceMax;
    private RadioGroup rgPriceSort;
    private Spinner spinnerFilterBrand;
    private Spinner spinnerFilterCategory;
    private MaterialButton btnApplyFilter;
    private MaterialButton btnResetFilter;

    public static FilterBottomSheetDialog newInstance() {
        return new FilterBottomSheetDialog();
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    public void setCurrentFilters(String priceSort, Integer brandId, Integer categoryId) {
        this.currentPriceSort = priceSort != null ? priceSort : "None";
        this.currentBrandId = brandId;
        this.currentCategoryId = categoryId;
    }

    public void setPriceRange(Float min, Float max) {
        this.currentPriceMin = min != null ? min : 0f;
        this.currentPriceMax = max != null ? max : 195000000f;
    }

    public void setBrandList(List<BrandDTO> brands) {
        this.brandList = brands != null ? new ArrayList<>(brands) : new ArrayList<>();
    }

    public void setCategoryList(List<CategoryDTO> categories) {
        this.categoryList = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.BottomSheetTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rangeSliderPrice = view.findViewById(R.id.rangeSliderPrice);
        tvPriceMin = view.findViewById(R.id.tvPriceMin);
        tvPriceMax = view.findViewById(R.id.tvPriceMax);
        rgPriceSort = view.findViewById(R.id.rgPriceSort);
        spinnerFilterBrand = view.findViewById(R.id.spinnerFilterBrand);
        spinnerFilterCategory = view.findViewById(R.id.spinnerFilterCategory);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnResetFilter = view.findViewById(R.id.btnResetFilter);
        ImageView btnCloseFilter = view.findViewById(R.id.btnCloseFilter);

        // Setup price range slider
        setupPriceRangeSlider();
        
        // Setup price sort radio group
        setupPriceSort();
        
        // Setup brand spinner
        setupBrandSpinner();
        
        // Setup category spinner
        setupCategorySpinner();

        // Close button
        btnCloseFilter.setOnClickListener(v -> dismiss());

        // Reset button
        btnResetFilter.setOnClickListener(v -> {
            resetFilters();
            if (listener != null) {
                listener.onFilterReset();
            }
            dismiss();
        });

        // Apply button
        btnApplyFilter.setOnClickListener(v -> {
            applyFilters();
            if (listener != null) {
                String priceSort = getSelectedPriceSort();
                Integer brandId = getSelectedBrandId();
                Integer categoryId = getSelectedCategoryId();
                
                // Get price values from RangeSlider
                List<Float> values = rangeSliderPrice.getValues();
                Float priceMin = values.get(0);
                Float priceMax = values.size() > 1 ? values.get(1) : values.get(0);
                
                listener.onFilterApplied(priceSort, brandId, categoryId, priceMin, priceMax);
            }
            dismiss();
        });
    }

    private void setupPriceRangeSlider() {
        // Set initial values
        rangeSliderPrice.setValues(currentPriceMin, currentPriceMax);
        
        // Update text when slider changes
        rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                updatePriceText(values.get(0), values.get(1));
            }
        });
        
        // Initialize text
        updatePriceText(currentPriceMin, currentPriceMax);
    }

    private void updatePriceText(float min, float max) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        tvPriceMin.setText(formatter.format((long)min) + "₫");
        tvPriceMax.setText(formatter.format((long)max) + "₫");
    }

    private void setupPriceSort() {
        RadioButton rbPriceNone = getView().findViewById(R.id.rbPriceNone);
        RadioButton rbPriceLow = getView().findViewById(R.id.rbPriceLow);
        RadioButton rbPriceHigh = getView().findViewById(R.id.rbPriceHigh);

        if ("ASC".equals(currentPriceSort)) {
            rbPriceLow.setChecked(true);
        } else if ("DESC".equals(currentPriceSort)) {
            rbPriceHigh.setChecked(true);
        } else {
            rbPriceNone.setChecked(true);
        }
    }

    private void setupBrandSpinner() {
        List<String> brandNames = new ArrayList<>();
        brandNames.add("All");
        int selectedIndex = 0;
        for (int i = 0; i < brandList.size(); i++) {
            BrandDTO brand = brandList.get(i);
            brandNames.add(brand.getName());
            if (currentBrandId != null && brand.getBrandID().equals(currentBrandId)) {
                selectedIndex = i + 1;
            }
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                brandNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterBrand.setAdapter(adapter);
        spinnerFilterBrand.setSelection(selectedIndex);
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("All");
        int selectedIndex = 0;
        for (int i = 0; i < categoryList.size(); i++) {
            CategoryDTO category = categoryList.get(i);
            categoryNames.add(category.getName());
            if (currentCategoryId != null && category.getId().equals(currentCategoryId)) {
                selectedIndex = i + 1;
            }
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(adapter);
        spinnerFilterCategory.setSelection(selectedIndex);
    }

    private String getSelectedPriceSort() {
        int selectedId = rgPriceSort.getCheckedRadioButtonId();
        if (selectedId == R.id.rbPriceLow) {
            return "ASC";
        } else if (selectedId == R.id.rbPriceHigh) {
            return "DESC";
        }
        return "None";
    }

    private Integer getSelectedBrandId() {
        int position = spinnerFilterBrand.getSelectedItemPosition();
        if (position > 0 && position <= brandList.size()) {
            return brandList.get(position - 1).getBrandID();
        }
        return null;
    }

    private Integer getSelectedCategoryId() {
        int position = spinnerFilterCategory.getSelectedItemPosition();
        if (position > 0 && position <= categoryList.size()) {
            return categoryList.get(position - 1).getId();
        }
        return null;
    }

    private void resetFilters() {
        // Reset price range slider
        rangeSliderPrice.setValues(0f, 195000000f);
        updatePriceText(0f, 195000000f);
        
        // Reset price sort
        RadioButton rbPriceNone = getView().findViewById(R.id.rbPriceNone);
        rbPriceNone.setChecked(true);
        
        // Reset spinners
        spinnerFilterBrand.setSelection(0);
        spinnerFilterCategory.setSelection(0);
    }

    private void applyFilters() {
        // Filters are already read through getter methods
    }
}

