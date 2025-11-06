package com.example.onlyfanshop.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.SearchSuggestionAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 300L;
    private static final int SUGGEST_MAX_ROWS = 10;
    private static final int SUGGEST_ROW_DP = 68;

    private EditText etSearch;
    private androidx.recyclerview.widget.RecyclerView recyclerSuggest;
    private ProgressBar progressSearch;

    private SearchSuggestionAdapter suggestAdapter;
    private ProductApi productApi;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ImageView btnBack = v.findViewById(R.id.btnBack);
        etSearch = v.findViewById(R.id.etSearch);
        recyclerSuggest = v.findViewById(R.id.recyclerSearchSuggest);
        progressSearch = v.findViewById(R.id.progressSearch);

        recyclerSuggest.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSuggest.setHasFixedSize(true);
        recyclerSuggest.setItemViewCacheSize(8);
        recyclerSuggest.setItemAnimator(null);

        suggestAdapter = new SearchSuggestionAdapter(item -> {
            Integer pid = item.getProductID();
            if (pid != null && pid > 0) {
                startActivity(ProductDetailActivity.newIntent(requireContext(), pid));
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        recyclerSuggest.setAdapter(suggestAdapter);

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);

        btnBack.setOnClickListener(view -> {
            if (isAdded()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // Focus vào ô tìm kiếm và mở bàn phím
        etSearch.requestFocus();
        showKeyboard();

        setupSearch();
    }

    // Ẩn BottomNavigationView khi vào SearchFragment, hiện lại khi rời đi
    @Override
    public void onResume() {
        super.onResume();
        View bottomNav = requireActivity().findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        View bottomNav = requireActivity().findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        super.onDestroyView();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                final String key = s.toString().trim();
                if (key.isEmpty()) {
                    suggestAdapter.submitList(new ArrayList<>());
                    recyclerSuggest.setVisibility(View.GONE);
                    progressSearch.setVisibility(View.GONE);
                    return;
                }
                pendingSearch = () -> fetchSuggestions(key);
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void fetchSuggestions(String keyword) {
        setSuggestLoading(true);
        productApi.getHomePagePost(
                1, 20,
                "ProductID", "DESC",
                keyword,
                null,
                null
        ).enqueue(new Callback<ApiResponse<HomePageData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<HomePageData>> call,
                                   @NonNull Response<ApiResponse<HomePageData>> response) {
                setSuggestLoading(false);
                if (!isAdded()) return;

                List<ProductDTO> products = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    if (response.body().getData().products != null) {
                        products = response.body().getData().products;
                    }
                }
                suggestAdapter.submitList(products);
                adjustSuggestionHeight(products.size());
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                setSuggestLoading(false);
                if (!isAdded()) return;
                suggestAdapter.submitList(new ArrayList<>());
                recyclerSuggest.setVisibility(View.GONE);
            }
        });
    }

    private void setSuggestLoading(boolean loading) {
        if (progressSearch != null) progressSearch.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading && recyclerSuggest != null) recyclerSuggest.setVisibility(View.GONE);
    }

    private void adjustSuggestionHeight(int count) {
        if (recyclerSuggest == null) return;
        if (count <= 0) {
            recyclerSuggest.setVisibility(View.GONE);
            return;
        }
        int rows = Math.min(SUGGEST_MAX_ROWS, count);
        int itemHeightPx = dpToPx(SUGGEST_ROW_DP);
        ViewGroup.LayoutParams lp = recyclerSuggest.getLayoutParams();
        lp.height = itemHeightPx * rows;
        recyclerSuggest.setLayoutParams(lp);
        recyclerSuggest.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp) {
        if (!isAdded()) return dp;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showKeyboard() {
        etSearch.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}