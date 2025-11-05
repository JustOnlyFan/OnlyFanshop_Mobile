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
import android.view.animation.AnimationUtils; // added
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFragment extends Fragment {

    private RecyclerView categoryView;
    private RecyclerView brandStripView;
    private ProgressBar progressBarCategory;

    private RecyclerView recyclerSearchResult;
    private ProgressBar progressSearch;
    private TextView textEmptySearch;
    private SwipeRefreshLayout swipeRefreshLayout;

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

    private boolean isFirstLoad = true;

    private View searchBarContainerView;
    private TextView tvCategoryTitleView;
    private TextView tvBrandTitleView;

    // NEW: dialog chọn brand
    private BrandPickerDialogFragment brandPickerDialog;

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

        searchBarContainerView = v.findViewById(R.id.searchBarContainer);
        tvBrandTitleView = v.findViewById(R.id.tvBrandTitle);
        tvCategoryTitleView = v.findViewById(R.id.tvCategoryTitle);

        brandStripView = v.findViewById(R.id.brandStripView);
        progressBarCategory = v.findViewById(R.id.progressBarCategory);
        categoryView = v.findViewById(R.id.categoryListView);

        recyclerSearchResult = v.findViewById(R.id.recyclerSearchResult);
        progressSearch = v.findViewById(R.id.progressSearch);
        textEmptySearch = v.findViewById(R.id.textEmptySearch);
        swipeRefreshLayout = v.findViewById(R.id.swipeSearchProducts);

        etSearchProduct = v.findViewById(R.id.etSearchProduct);
        btnFilter = v.findViewById(R.id.btnFilter);

        setupBrandStrip();
        setupCategoryList();
        setupProductRecycler();
        setupSwipeRefresh();

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);

        setupSearch();
        setupFilterButton();

        v.post(this::playTopFallDownEnter);

        fetchHomePage();
        isFirstLoad = false;
    }

    private com.example.onlyfanshop.adapter.BrandChipAdapter brandChipAdapter;
    private void setupBrandStrip() {
        brandChipAdapter = new com.example.onlyfanshop.adapter.BrandChipAdapter(new com.example.onlyfanshop.adapter.BrandChipAdapter.Listener() {
            @Override public void onBrandSelected(Integer brandId) {
                selectedBrandId = brandId;
                fetchHomePage();
            }
            @Override public void onSeeAll() {
                // MỞ POPUP HIỂN THỊ TẤT CẢ THƯƠNG HIỆU
                showAllBrandsDialog();
            }
        });
        brandStripView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        brandStripView.setHasFixedSize(true);
        brandStripView.setItemViewCacheSize(10);
        brandStripView.setItemAnimator(null);
        brandStripView.setAdapter(brandChipAdapter);
    }

    private void showAllBrandsDialog() {
        if (!isAdded()) return;
        if (brandPickerDialog == null) {
            brandPickerDialog = BrandPickerDialogFragment.newInstance();
            brandPickerDialog.setOnBrandSelectedListener(brandId -> {
                selectedBrandId = brandId;
                fetchHomePage();
            });
        }
        // Cập nhật danh sách brand mỗi lần mở
        brandPickerDialog.setBrandList(brandList);
        // Tránh IllegalStateException nếu state đã saved
        if (getParentFragmentManager().isStateSaved()) return;
        brandPickerDialog.show(getParentFragmentManager(), "BrandPickerDialog");
    }

    private void setupCategoryList() {
        categoryAdapter = new CategoryAdapter((id, name) -> {
            selectedCategoryId = id;
            fetchHomePage();
        });
        categoryView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        categoryView.setHasFixedSize(false);
        categoryView.setItemViewCacheSize(15);
        categoryView.setItemAnimator(null);
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
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        recyclerSearchResult.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerSearchResult.setHasFixedSize(true);
        recyclerSearchResult.setItemViewCacheSize(15);
        recyclerSearchResult.setItemAnimator(null);
        recyclerSearchResult.setAdapter(productAdapter);

        recyclerSearchResult.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down)
        );
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) return;
        swipeRefreshLayout.setOnRefreshListener(this::fetchHomePage);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
        );
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
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    showEmptyProducts();
                    return;
                }

                HomePageData data = response.body().getData();

                if (data.brands != null) {
                    brandList.clear();
                    brandList.addAll(data.brands);
                }

                List<CategoryDTO> categories = data.categories != null ? data.categories : new ArrayList<>();
                allCategoryList.clear();
                allCategoryList.addAll(categories);

                if (brandChipAdapter != null) brandChipAdapter.submitList(brandList);

                if (categoryAdapter != null) {
                    categoryAdapter.submitList(allCategoryList);
                    categoryAdapter.setSelectedId(selectedCategoryId);
                }

                List<ProductDTO> products = data.products != null ? data.products : new ArrayList<>();

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

                if ("Price".equals(sortBy)) {
                    Comparator<ProductDTO> cmp = Comparator.comparingDouble(
                            p -> Double.parseDouble(String.valueOf(p.getPrice()))
                    );
                    if ("ASC".equals(sortOrder)) {
                        products.sort(cmp);
                    } else {
                        products.sort(cmp.reversed());
                    }
                }

                for (ProductDTO p : products) {
                    Log.d("SORTED_LIST", p.getProductName() + " - " + p.getPrice());
                }

                productAdapter.submitList(products);
                textEmptySearch.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);

                if (!products.isEmpty()) {
                    playListEnterAnimation();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                setCategoryLoading(false);
                setProductLoading(false);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
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

        String priceSort = sortBy.equals("Price") ? sortOrder : "None";
        filterDialog.setCurrentFilters(priceSort, selectedBrandId, selectedCategoryId);
        filterDialog.setBrandList(brandList);
        filterDialog.setCategoryList(allCategoryList);

        filterDialog.setFilterListener(new FilterBottomSheetDialog.FilterListener() {
            @Override
            public void onFilterApplied(String priceSort, Integer brandId, Integer categoryId, Float priceMin, Float priceMax) {
                if ("None".equals(priceSort)) {
                    sortBy = "ProductID";
                    sortOrder = "DESC";
                } else {
                    sortBy = "Price";
                    sortOrder = priceSort;
                }
                selectedBrandId = brandId;
                selectedCategoryId = categoryId;
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

    private void playTopFallDownEnter() {
        if (!isAdded()) return;
        if (searchBarContainerView != null) {
            searchBarContainerView.clearAnimation();
            searchBarContainerView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down)
            );
        }
        if (tvBrandTitleView != null) {
            tvBrandTitleView.clearAnimation();
            tvBrandTitleView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down)
            );
        }
        if (brandStripView != null) {
            brandStripView.clearAnimation();
            brandStripView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down)
            );
        }
        if (tvCategoryTitleView != null) {
            tvCategoryTitleView.clearAnimation();
            tvCategoryTitleView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down)
            );
        }
        if (categoryView != null) {
            categoryView.clearAnimation();
            categoryView.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fall_down)
            );
        }
    }

    public void playListEnterAnimation() {
        if (recyclerSearchResult == null) return;
        if (recyclerSearchResult.getLayoutAnimation() == null && isAdded()) {
            recyclerSearchResult.setLayoutAnimation(
                    AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down)
            );
        }
        recyclerSearchResult.scheduleLayoutAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            if (!isFirstLoad) {
                resetFilters();
            }
            playTopFallDownEnter();
            playListEnterAnimation();
        }
    }

    private void resetFilters() {
        selectedBrandId = null;
        selectedCategoryId = null;
        sortBy = "ProductID";
        sortOrder = "DESC";
        keyword = null;
        if (etSearchProduct != null) {
            etSearchProduct.setText("");
        }
        if (categoryAdapter != null) {
            categoryAdapter.setSelectedId(null);
        }
        fetchHomePage();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}