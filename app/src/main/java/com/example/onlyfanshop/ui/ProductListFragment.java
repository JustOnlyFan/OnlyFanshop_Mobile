package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.ProductAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;
import com.example.onlyfanshop.ViewModel.ProductFilterViewModel;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerProducts;
    private ProgressBar progressProducts;
    private TextView textEmptyProducts;

    private ProductAdapter productAdapter;
    private ProductApi productApi;
    private ProductFilterViewModel filterVM;

    @Nullable
    private String currentKeyword;
    @Nullable
    private Integer currentCategoryId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Khởi tạo các view
        swipeRefresh = v.findViewById(R.id.swipeRefreshProducts);
        recyclerProducts = v.findViewById(R.id.recyclerProducts);
        progressProducts = v.findViewById(R.id.progressProducts);
        textEmptyProducts = v.findViewById(R.id.textEmptyProducts);

        setupRecycler();
        setupSwipe();

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);
        filterVM = new ViewModelProvider(requireActivity()).get(ProductFilterViewModel.class);

        // Lắng nghe thay đổi từ ViewModel để fetch lại sản phẩm
        filterVM.getKeyword().observe(getViewLifecycleOwner(), kw -> {
            currentKeyword = kw;
            fetchProducts();
        });
        filterVM.getCategoryId().observe(getViewLifecycleOwner(), cid -> {
            currentCategoryId = cid;
            fetchProducts();
        });

        // Tải sản phẩm lần đầu
        fetchProducts();
    }

    private void setupRecycler() {
        productAdapter = new ProductAdapter(item -> {
            Integer pid = item.getProductID();
            if (pid == null || pid <= 0) {
                Toast.makeText(requireContext(), "Product ID không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(ProductDetailActivity.newIntent(requireContext(), pid));
        });
        recyclerProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerProducts.setAdapter(productAdapter);
    }

    private void setupSwipe() {
        swipeRefresh.setOnRefreshListener(() -> {
            fetchProducts();
            // Tắt loading sau 5s nếu callback không về (timeout an toàn)
            swipeRefresh.postDelayed(() -> swipeRefresh.setRefreshing(false), 5000);
        });
    }



    /**
     * Hiện hoặc ẩn progressBar chính khi load dữ liệu (không phải loading xoay của SwipeRefreshLayout)
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressProducts.setVisibility(View.VISIBLE);
            textEmptyProducts.setVisibility(View.GONE);
        } else {
            progressProducts.setVisibility(View.GONE);
        }
    }

    /**
     * Gọi API để lấy danh sách sản phẩm
     * Đảm bảo luôn tắt xoay của SwipeRefreshLayout khi hoàn thành (thành công hoặc lỗi)
     */
    private void fetchProducts() {
        if (productApi == null) return;

        // Chỉ show progressBar nhỏ ở giữa khi gọi từ code, không ảnh hưởng xoay của SwipeRefreshLayout
        setLoading(true);

        // Gọi API lấy sản phẩm (thêm filter khác nếu cần)
        Call<ApiResponse<HomePageData>> call = productApi.getHomePagePost(
                1,          // page
                20,         // size
                "ProductID",// sortBy
                "DESC",     // order
                currentKeyword,
                currentCategoryId,
                null        // brandId nếu có filter brand
        );

        call.enqueue(new Callback<ApiResponse<HomePageData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<HomePageData>> call,
                                   @NonNull Response<ApiResponse<HomePageData>> response) {
                // Luôn tắt xoay của SwipeRefreshLayout ngay khi nhận được callback
                swipeRefresh.setRefreshing(false);
                setLoading(false);

                List<ProductDTO> products = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    if (response.body().getData().products != null) {
                        products = response.body().getData().products;
                    }
                }

                productAdapter.submitList(products);
                textEmptyProducts.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false); // Tắt xoay khi lỗi
                setLoading(false);
                productAdapter.submitList(new ArrayList<>());
                textEmptyProducts.setVisibility(View.VISIBLE);
            }
        });
    }
}