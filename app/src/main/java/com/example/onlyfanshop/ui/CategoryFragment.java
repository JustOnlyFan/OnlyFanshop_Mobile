package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.CategoryAdapter;
import com.example.onlyfanshop.adapter.ProductAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFragment extends Fragment {

    private RecyclerView categoryView;
    private ProgressBar progressBarCategory;

    private RecyclerView recyclerSearchResult;
    private ProgressBar progressSearch;
    private TextView textEmptySearch;

    private EditText etSearchProduct;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private ProductApi productApi;

    private String keyword = null;
    @Nullable
    private Integer selectedCategoryId = null;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private ImageView btnFilter;
    private FilterBottomSheetDialog filterDialog;
    private String sortBy = "ProductID";
    private String sortOrder = "DESC";
    private Integer selectedBrandId = null;
    private final List<BrandDTO> brandList = new ArrayList<>();
    private final List<CategoryDTO> allCategoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        categoryView = v.findViewById(R.id.categoryView);
        progressBarCategory = v.findViewById(R.id.progressBarCategory);

        recyclerSearchResult = v.findViewById(R.id.recyclerSearchResult);
        progressSearch = v.findViewById(R.id.progressSearch);
        textEmptySearch = v.findViewById(R.id.textEmptySearch);

        etSearchProduct = v.findViewById(R.id.etSearchProduct);
        btnFilter = v.findViewById(R.id.btnFilter);

        setupCategoryRecycler();
        setupProductRecycler();

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);

        setupSearch();
        setupFilterButton();

        fetchHomePage();
    }

    private void setupCategoryRecycler() {
        categoryAdapter = new CategoryAdapter((id, name) -> {
            selectedCategoryId = id; // null = All
            fetchHomePage();
        });
        categoryView.setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        );
        categoryView.setAdapter(categoryAdapter);
    }

    private void setupProductRecycler() {
        productAdapter = new ProductAdapter(item -> {
            Integer pid = item.getProductID();
            if (pid == null || pid <= 0) {
                Toast.makeText(requireContext(), "Product ID không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(ProductDetailActivity.newIntent(requireContext(), pid));
        });
        recyclerSearchResult.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerSearchResult.setAdapter(productAdapter);
    }

    private void setupSearch() {
        if (etSearchProduct == null) return;

        etSearchProduct.setKeyListener(TextKeyListener.getInstance());
        etSearchProduct.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                keyword = s.toString().trim();
                if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
                pendingSearch = CategoryFragment.this::fetchHomePage;
                debounceHandler.postDelayed(pendingSearch, 350);
            }
        });
    }

    private void setCategoryLoading(boolean loading) {
        if (progressBarCategory != null) {
            progressBarCategory.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void setProductLoading(boolean loading) {
        if (progressSearch != null) progressSearch.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (textEmptySearch != null && loading) textEmptySearch.setVisibility(View.GONE);
    }

    private void fetchHomePage() {
        if (productApi == null) return;

        setCategoryLoading(true);
        setProductLoading(true);

        Call<ApiResponse<HomePageData>> call = productApi.getHomePagePost(
                1,
                20,
                sortBy,
                sortOrder,
                TextUtils.isEmpty(keyword) ? null : keyword,
                selectedCategoryId,
                selectedBrandId
        );

        call.enqueue(new Callback<ApiResponse<HomePageData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<HomePageData>> call,
                                   @NonNull Response<ApiResponse<HomePageData>> response) {
                setCategoryLoading(false);
                setProductLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    showEmptyProducts();
                    return;
                }

                HomePageData data = response.body().getData();

                // Cập nhật danh sách brand
                if (data.brands != null) {
                    brandList.clear();
                    brandList.addAll(data.brands);
                }

                // Category logic
                List<CategoryDTO> categories = data.categories != null ? data.categories : new ArrayList<>();
                allCategoryList.clear();
                allCategoryList.addAll(categories);
                
                CategoryDTO all = new CategoryDTO();
                all.setId(null);
                all.setName("All");
                List<CategoryDTO> display = new ArrayList<>();
                display.add(all);
                display.addAll(categories);
                categoryAdapter.submitList(display);

                List<ProductDTO> products = data.products != null ? data.products : new ArrayList<>();

                // Filter theo category nếu cần
                if (selectedCategoryId != null) {
                    List<ProductDTO> filteredList = new ArrayList<>();
                    for (ProductDTO p : products) {
                        if (p.getCategory() != null && p.getCategory().getId() != null &&
                                p.getCategory().getId().equals(selectedCategoryId)) {
                            filteredList.add(p);
                        }
                    }
                    products = filteredList;
                }

                // Sort giá đúng ý nghĩa: tăng dần (ASC) là nhỏ -> lớn, giảm dần (DESC) là lớn -> nhỏ
                if ("Price".equals(sortBy)) {
                    Comparator<ProductDTO> cmp = Comparator.comparingDouble(
                            p -> Double.parseDouble(String.valueOf(p.getPrice()))
                    );
                    if ("ASC".equals(sortOrder)) {
                        products.sort(cmp); // Tăng dần: nhỏ -> lớn
                    } else {
                        products.sort(cmp.reversed()); // Giảm dần: lớn -> nhỏ
                    }
                }

                // Log kiểm tra
                for (ProductDTO p : products) {
                    Log.d("SORTED_LIST", p.getProductName() + " - " + p.getPrice());
                }

                productAdapter.submitList(products);
                textEmptySearch.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                setCategoryLoading(false);
                setProductLoading(false);
                showEmptyProducts();
            }
        });
    }

    private void showEmptyProducts() {
        productAdapter.submitList(new ArrayList<>());
        textEmptySearch.setVisibility(View.VISIBLE);
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        if (filterDialog == null) {
            filterDialog = FilterBottomSheetDialog.newInstance();
        }

        // Set current filter values
        String priceSort = sortBy.equals("Price") ? sortOrder : "None";
        filterDialog.setCurrentFilters(priceSort, selectedBrandId, selectedCategoryId);
        filterDialog.setBrandList(brandList);
        filterDialog.setCategoryList(allCategoryList);

        // Set listener
        filterDialog.setFilterListener(new FilterBottomSheetDialog.FilterListener() {
            @Override
            public void onFilterApplied(String priceSort, Integer brandId, Integer categoryId, Float priceMin, Float priceMax) {
                // Update sort by price
                if ("None".equals(priceSort)) {
                    sortBy = "ProductID";
                    sortOrder = "DESC";
                } else {
                    sortBy = "Price";
                    sortOrder = priceSort;
                }

                selectedBrandId = brandId;
                selectedCategoryId = categoryId;
                
                // Note: Price range filtering would need to be implemented on backend
                // For now, we just store the values but don't use them in API call
                // You can filter products client-side if needed

                // Refresh products
                fetchHomePage();
            }

            @Override
            public void onFilterReset() {
                sortBy = "ProductID";
                sortOrder = "DESC";
                selectedBrandId = null;
                selectedCategoryId = null;
                fetchHomePage();
            }
        });

        filterDialog.show(getParentFragmentManager(), "FilterBottomSheet");
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}