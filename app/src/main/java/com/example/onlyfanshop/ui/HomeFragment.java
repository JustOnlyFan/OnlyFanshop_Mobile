package com.example.onlyfanshop.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.BannerAdapter;
import com.example.onlyfanshop.adapter.PopularAdapter;
import com.example.onlyfanshop.adapter.SearchSuggestionAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.BannerModel;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;
import com.example.onlyfanshop.model.response.UserResponse;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private static final String DB_URL = "https://onlyfan-f9406-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String BANNER_NODE = "Banner";

    // Banner
    private ViewPager2 viewPagerBanner;
    private ProgressBar progressBarBanner;
    private BannerAdapter bannerAdapter;
    private final Handler sliderHandler = new Handler();
    private static final long SLIDER_INTERVAL_MS = 3000L;

    // Popular
    private RecyclerView popularView;
    private ProgressBar progressBarPopular;
    private PopularAdapter popularAdapter;
    private ProductApi productApi;

    // Welcome
    private TextView tvUserName;

    // Search suggestions
    private EditText etSearch;
    private RecyclerView recyclerSuggest;
    private ProgressBar progressSearch;
    private SearchSuggestionAdapter suggestAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    // Số dòng tối đa hiển thị đồng thời
    private static final int SUGGEST_MAX_ROWS = 5;
    // Chiều cao mỗi dòng (khớp với row_search_suggestion.xml: 68dp)
    private static final int SUGGEST_ROW_DP = 68;

    private final Runnable sliderRunnable = new Runnable() {
        @Override public void run() {
            if (!isAdded() || viewPagerBanner == null || bannerAdapter == null) return;
            int count = bannerAdapter.getItemCount();
            if (count <= 1) return;
            int next = viewPagerBanner.getCurrentItem() + 1;
            if (next >= count) next = 0;
            viewPagerBanner.setCurrentItem(next, true);
            sliderHandler.postDelayed(this, SLIDER_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);


        // Welcome
        tvUserName = v.findViewById(R.id.tvUserName);
        tvUserName.setOnClickListener(view -> {
            if ("Sign in".equals(tvUserName.getText().toString())) {
                Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.login.LoginActivity.class);
                startActivity(intent);
                requireActivity().finish(); // Nếu muốn khi đăng nhập xong không quay lại Home cũ
            }
        });
        // Banner
        viewPagerBanner = v.findViewById(R.id.viewPagerBanner);
        progressBarBanner = v.findViewById(R.id.progressBarBanner);

        bannerAdapter = new BannerAdapter(new ArrayList<>(), viewPagerBanner);
        viewPagerBanner.setAdapter(bannerAdapter);

        viewPagerBanner.setClipToPadding(false);
        viewPagerBanner.setClipChildren(false);
        viewPagerBanner.setOffscreenPageLimit(3);
        CompositePageTransformer composite = new CompositePageTransformer();
        composite.addTransformer(new MarginPageTransformer(40));
        viewPagerBanner.setPageTransformer(composite);

        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, SLIDER_INTERVAL_MS);
            }
        });

        // Load banner từ Realtime Database
        loadBannersFromRealtimeDb();

        // Popular
        popularView = v.findViewById(R.id.popularView);
        progressBarPopular = v.findViewById(R.id.progressBarPopular);

        popularView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        popularView.setNestedScrollingEnabled(false);

        popularAdapter = new PopularAdapter(item -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProductID());
            startActivity(intent);
        });
        popularView.setAdapter(popularAdapter);

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);
        loadPopular();

        // Search suggestions
        etSearch = v.findViewById(R.id.editTextText);
        recyclerSuggest = v.findViewById(R.id.recyclerSearchSuggest);
        progressSearch = v.findViewById(R.id.progressSearch);

        recyclerSuggest.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Cho phép cuộn bên trong khi giới hạn chiều cao
        recyclerSuggest.setNestedScrollingEnabled(true);
        recyclerSuggest.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        // Tránh ScrollView chặn cử chỉ cuộn của RecyclerView
        recyclerSuggest.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        suggestAdapter = new SearchSuggestionAdapter(item -> {
            Integer pid = item.getProductID();
            if (pid != null && pid > 0) {
                startActivity(ProductDetailActivity.newIntent(requireContext(), pid));
            }
        });
        recyclerSuggest.setAdapter(suggestAdapter);

        setupSearch();

        // Lấy tên user cho phần Welcome
        fetchUserName();
    }

    // -------- Banner từ Realtime Database --------
    private void setBannerLoading(boolean loading) {
        if (progressBarBanner != null) progressBarBanner.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void loadBannersFromRealtimeDb() {
        setBannerLoading(true);

        FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child(BANNER_NODE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    ArrayList<BannerModel> banners = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String url;
                        if (child.hasChild("url")) {
                            url = child.child("url").getValue(String.class);
                        } else {
                            url = child.getValue(String.class);
                        }
                        if (url != null && !url.isEmpty()) {
                            BannerModel m = new BannerModel();
                            m.setUrl(url);
                            banners.add(m);
                        }
                    }
                    bannerAdapter.submit(banners);
                    setBannerLoading(false);
                    if (!banners.isEmpty()) startAutoSlide();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Realtime DB load failed at node '" + BANNER_NODE + "'", e);
                    bannerAdapter.submit(new ArrayList<>());
                    setBannerLoading(false);
                });
    }

    private void startAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, SLIDER_INTERVAL_MS);
    }

    private void stopAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override public void onResume() {
        super.onResume();
        startAutoSlide();
    }

    @Override public void onPause() {
        stopAutoSlide();
        super.onPause();
    }

    @Override public void onDestroyView() {
        stopAutoSlide();
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        super.onDestroyView();
    }

    // ---------------- Popular ----------------
    private void setPopularLoading(boolean loading) {
        if (progressBarPopular != null) {
            progressBarPopular.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void loadPopular() {
        setPopularLoading(true);
        productApi.getHomePagePost(1, 10, "ProductID", "DESC", null, null, null)
                .enqueue(new Callback<ApiResponse<HomePageData>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<HomePageData>> call,
                                           @NonNull Response<ApiResponse<HomePageData>> response) {
                        setPopularLoading(false);
                        List<ProductDTO> products = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            if (response.body().getData().products != null) {
                                products = response.body().getData().products;
                            }
                        }
                        popularAdapter.submitList(products);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                        setPopularLoading(false);
                        popularAdapter.submitList(new ArrayList<>());
                    }
                });
    }

    // ---------------- Search suggestion ----------------
    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                final String key = s.toString().trim();

                // Nếu rỗng thì ẩn gợi ý
                if (key.isEmpty()) {
                    suggestAdapter.submitList(new ArrayList<>());
                    recyclerSuggest.setVisibility(View.GONE);
                    progressSearch.setVisibility(View.GONE);
                    // Không cần giữ chiều cao cũ khi ẩn
                    return;
                }

                pendingSearch = () -> fetchSuggestions(key);
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void setSuggestLoading(boolean loading) {
        if (progressSearch != null) progressSearch.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) recyclerSuggest.setVisibility(View.GONE);
    }

    private void fetchSuggestions(String keyword) {
        if (productApi == null) return;
        setSuggestLoading(true);

        productApi.getHomePagePost(
                        1, 20, // có thể trả về >5, ta sẽ giới hạn chiều cao hiển thị
                        "ProductID", "DESC",
                        keyword,
                        null,
                        null)
                .enqueue(new Callback<ApiResponse<HomePageData>>() {
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
                    public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call,
                                          @NonNull Throwable t) {
                        setSuggestLoading(false);
                        if (!isAdded()) return;
                        suggestAdapter.submitList(new ArrayList<>());
                        recyclerSuggest.setVisibility(View.GONE);
                    }
                });
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
        if (!isAdded()) return dp; // fallback
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ---------------- Welcome username ----------------
    private void fetchUserName() {
        String token = ApiClient.getToken(requireContext());
        if (token == null || token.trim().isEmpty()) {
            tvUserName.setText("Sign in");
            return;
        }
        ProfileApi profileApi = ApiClient.getPrivateClient(requireContext()).create(ProfileApi.class);
        profileApi.getUser().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    String name = user.getUsername();
                    if (name == null || name.trim().isEmpty()) name = "Guest";
                    tvUserName.setText(name);
                } else if (response.code() == 401) {
                    Log.w(TAG, "Unauthorized. Token may be invalid/expired.");
                } else {
                    Log.w(TAG, "getUser failed: code=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "getUser error", t);
            }
        });
    }
}