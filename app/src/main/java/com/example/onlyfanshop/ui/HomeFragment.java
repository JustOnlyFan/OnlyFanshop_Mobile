package com.example.onlyfanshop.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.BannerAdapter;
import com.example.onlyfanshop.adapter.PopularAdapter;
import com.example.onlyfanshop.adapter.ProductAdapter;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    private static final int LOOP_COUNT = 10000; // Số lượng lớn để giả infinite loop
    private List<BannerModel> bannerList = new ArrayList<>(); // List banner thực

    // Popular
    private RecyclerView popularView;
    private ProgressBar progressBarPopular;
    private PopularAdapter popularAdapter;
    private ProductApi productApi;

    // Products
    private RecyclerView productsView;
    private ProgressBar progressBarProducts;
    private ProductAdapter productAdapter;

    // Welcome
    private TextView tvUserName;

    // Search suggestions
    private EditText etSearch;
    private RecyclerView recyclerSuggest;
    private ProgressBar progressSearch;
    private SearchSuggestionAdapter suggestAdapter;

    private ImageView btnNotif;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private static final int SUGGEST_MAX_ROWS = 5;
    private static final int SUGGEST_ROW_DP = 68;

    // Views for entrance animation
    private View headerContainer;      // R.id.homeHeader
    private View searchBar;            // R.id.legacySearchBar
    private View bannerContainer;      // R.id.bannerContainer
    private View popularHeader;        // R.id.tvPopularTitle (we will animate its parent block)
    private View productsHeader;       // R.id.tvProductsTitle (we will animate its parent block)

    private View ivAvatar;
    private View tvUserNameView;
    private View notifContainer;

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || viewPagerBanner == null || bannerAdapter == null || bannerList == null || bannerList.isEmpty())
                return;
            int next = viewPagerBanner.getCurrentItem() + 1;
            viewPagerBanner.setCurrentItem(next, true); // luôn trượt phải, không giật về đầu
            sliderHandler.postDelayed(this, SLIDER_INTERVAL_MS);
        }
    };

    // Chạy slide-in từ phải vào cho toàn fragment (root)
    private void playFragmentSlideIn() {
        if (!isAdded() || getView() == null) return;
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right);
        getView().startAnimation(anim);
    }

    // Chạy slide-out sang trái khi fragment bị ẩn
    private void playFragmentSlideOut() {
        if (!isAdded() || getView() == null) return;
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_left);
        getView().startAnimation(anim);
    }

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
        btnNotif = v.findViewById(R.id.btnNotif);
        TextView tvNotifBadge = v.findViewById(R.id.tvNotifBadge);
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId != -1) {
            fetchUnreadNotificationCount(userId, tvNotifBadge);
        }

        btnNotif.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

            if (userId == -1) {
                Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.login.LoginActivity.class);
                startActivity(intent);
                return;
            }

            Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.notification.NotificationListActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // Welcome
        tvUserName = v.findViewById(R.id.tvUserName);
        tvUserName.setOnClickListener(view -> {
            if ("Sign in".equals(tvUserName.getText().toString())) {
                Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.login.LoginActivity.class);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        // Cache views for entrance animation
        headerContainer = v.findViewById(R.id.homeHeader);
        searchBar = v.findViewById(R.id.legacySearchBar);
        bannerContainer = v.findViewById(R.id.bannerContainer);
        // Popular/Products header containers are RelativeLayouts around those titles
        View popularHeaderBlock = (View) v.findViewById(R.id.tvPopularTitle).getParent();
        View productsHeaderBlock = (View) v.findViewById(R.id.tvProductsTitle).getParent();
        popularHeader = popularHeaderBlock;
        productsHeader = productsHeaderBlock;

        // Banner
        viewPagerBanner = v.findViewById(R.id.viewPagerBanner);
        progressBarBanner = v.findViewById(R.id.progressBarBanner);

        bannerAdapter = new BannerAdapter(new ArrayList<>(), viewPagerBanner) {
            @Override
            public int getItemCount() {
                return bannerList == null || bannerList.isEmpty() ? 0 : LOOP_COUNT;
            }

            @Override
            public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
                if (bannerList == null || bannerList.isEmpty()) return;
                int realPos = position % bannerList.size();
                holder.bind(bannerList.get(realPos));
            }
        };

        viewPagerBanner.setAdapter(bannerAdapter);
        viewPagerBanner.setClipToPadding(false);
        viewPagerBanner.setClipChildren(false);
        viewPagerBanner.setOffscreenPageLimit(1); // Giảm từ 3 xuống 1 để nhanh hơn
        CompositePageTransformer composite = new CompositePageTransformer();
        composite.addTransformer(new MarginPageTransformer(40));
        viewPagerBanner.setPageTransformer(composite);
        // Tối ưu ViewPager2 scroll
        viewPagerBanner.post(() -> {
            RecyclerView rv = (RecyclerView) viewPagerBanner.getChildAt(0);
            if (rv != null) {
                rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
        });

        viewPagerBanner.post(() -> {
            int mid = LOOP_COUNT / 2;
            viewPagerBanner.setCurrentItem(mid, false);
        });

        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, SLIDER_INTERVAL_MS);
            }
        });

        // Lazy load: Chỉ load banner và popular sau khi UI đã render
        // Banner và Popular sẽ load sau một chút để UI hiển thị nhanh hơn
        v.postDelayed(() -> loadBannersFromRealtimeDb(), 100);

        // Popular
        popularView = v.findViewById(R.id.popularView);
        progressBarPopular = v.findViewById(R.id.progressBarPopular);

        popularView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        popularView.setNestedScrollingEnabled(false);
        // Tối ưu scroll performance
        popularView.setHasFixedSize(true);
        popularView.setItemViewCacheSize(10);
        popularView.setItemAnimator(null); // Tắt animation khi scroll để mượt hơn
        // Layout animation chỉ khi load lần đầu
        popularView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down));

        popularAdapter = new PopularAdapter(item -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProductID());
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        popularView.setAdapter(popularAdapter);

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);
        // Lazy load: Popular sẽ load sau khi UI đã render
        v.postDelayed(() -> loadPopular(), 150);

        // See all button - navigate to products tab
        TextView tvSeeAll = v.findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(view -> {
                if (getActivity() != null) {
                    View bottomNavView = getActivity().findViewById(R.id.bottomNav);
                    if (bottomNavView instanceof BottomNavigationView) {
                        BottomNavigationView bottomNav = (BottomNavigationView) bottomNavView;
                        bottomNav.setSelectedItemId(R.id.nav_search);
                    }
                }
            });
        }

        // Products
        productsView = v.findViewById(R.id.productsView);
        progressBarProducts = v.findViewById(R.id.progressBarProducts);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        productsView.setLayoutManager(gridLayoutManager);
        productsView.setNestedScrollingEnabled(false);
        // Tối ưu scroll performance
        productsView.setHasFixedSize(true);
        productsView.setItemViewCacheSize(15);
        productsView.setItemAnimator(null); // Tắt animation khi scroll để mượt hơn
        // Layout animation chỉ khi load lần đầu
        productsView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down));
        ivAvatar = v.findViewById(R.id.imageViewProfile);
        tvUserNameView = v.findViewById(R.id.tvUserName);
        notifContainer = v.findViewById(R.id.notifContainer);
        productAdapter = new ProductAdapter(item -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProductID());
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        productsView.setAdapter(productAdapter);

        // Lazy load: Products sẽ load sau khi UI đã render
        v.postDelayed(() -> loadProducts(), 200);

        // Search suggestions
        etSearch = v.findViewById(R.id.editTextText);
        recyclerSuggest = v.findViewById(R.id.recyclerSearchSuggest);
        progressSearch = v.findViewById(R.id.progressSearch);

        recyclerSuggest.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSuggest.setNestedScrollingEnabled(true);
        recyclerSuggest.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        // Tối ưu scroll performance
        recyclerSuggest.setHasFixedSize(true);
        recyclerSuggest.setItemViewCacheSize(8);
        recyclerSuggest.setItemAnimator(null); // Tắt animation để mượt hơn
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
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        recyclerSuggest.setAdapter(suggestAdapter);

        setupSearch();

        // Lấy tên user cho phần Welcome
        fetchUserName();

//         Staggered entrance ngay lần đầu hiển thị
//        playEnterAnimationSequential();
        View root = v;
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                // Slide toàn fragment (nếu bạn vẫn muốn cảm giác vào màn)
                playFragmentSlideIn();
                // Sau 60ms, slide từng attribute theo 2 nửa (giảm delay)
                root.postDelayed(() -> playEnterSlideOppositeSidesTogether(), 60);
                return true;
            }
        });
    }


    // -------- Banner từ Realtime Database --------
    private void setBannerLoading(boolean loading) {
        if (progressBarBanner != null)
            progressBarBanner.setVisibility(loading ? View.VISIBLE : View.GONE);
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
                    bannerList = banners;
                    bannerAdapter.notifyDataSetChanged();

                    setBannerLoading(false);
                    if (!banners.isEmpty()) {
                        int mid = LOOP_COUNT / 2;
                        viewPagerBanner.setCurrentItem(mid, false);
                        startAutoSlide();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Realtime DB load failed at node '" + BANNER_NODE + "'", e);
                    loadFallbackBanners();
                    setBannerLoading(false);
                });
    }

    private void loadFallbackBanners() {
        bannerList = new ArrayList<>();
        bannerAdapter.notifyDataSetChanged();
    }

    private void startAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, SLIDER_INTERVAL_MS);
    }

    private void stopAutoSlide() {
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoSlide();
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId != -1 && getView() != null) {
            TextView tvNotifBadge = getView().findViewById(R.id.tvNotifBadge);
            if (tvNotifBadge != null) {
                fetchUnreadNotificationCount(userId, tvNotifBadge);
            }
        }
        if (isVisible() && getView() != null) {
            // Nếu muốn slide cả fragment mỗi lần quay lại, giữ dòng dưới; nếu không, có thể bỏ:
            playFragmentSlideIn();
            getView().postDelayed(this::playEnterSlideOppositeSidesTogether, 60);
        }
    }

    @Override
    public void onPause() {
        stopAutoSlide();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopAutoSlide();
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        
        // Giải phóng resources
        if (viewPagerBanner != null) {
            viewPagerBanner.setAdapter(null);
            viewPagerBanner = null;
        }
        if (popularView != null) {
            popularView.setAdapter(null);
            popularView = null;
        }
        if (productsView != null) {
            productsView.setAdapter(null);
            productsView = null;
        }
        if (recyclerSuggest != null) {
            recyclerSuggest.setAdapter(null);
            recyclerSuggest = null;
        }
        
        bannerAdapter = null;
        popularAdapter = null;
        productAdapter = null;
        suggestAdapter = null;
        
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
        productApi.getHomePagePost(1, 50, "ProductID", "DESC", null, null, null)
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
                        List<ProductDTO> randomProducts = getRandomProducts(products, 15);
                        popularAdapter.submitList(randomProducts);
                        // chạy layout animation cho list
                        if (popularView != null) {
                            popularView.scheduleLayoutAnimation();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                        setPopularLoading(false);
                        popularAdapter.submitList(new ArrayList<>());
                    }
                });
    }

    private List<ProductDTO> getRandomProducts(List<ProductDTO> products, int count) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductDTO> shuffled = new ArrayList<>(products);
        Collections.shuffle(shuffled, new Random());
        int size = Math.min(count, shuffled.size());
        return shuffled.subList(0, size);
    }

    // ---------------- Products ----------------
    private void setProductsLoading(boolean loading) {
        if (progressBarProducts != null) {
            progressBarProducts.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void loadProducts() {
        setProductsLoading(true);
        productApi.getHomePagePost(1, 1000, "ProductID", "ASC", null, null, null)
                .enqueue(new Callback<ApiResponse<HomePageData>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<HomePageData>> call,
                                           @NonNull Response<ApiResponse<HomePageData>> response) {
                        setProductsLoading(false);
                        List<ProductDTO> products = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            if (response.body().getData().products != null) {
                                products = response.body().getData().products;
                            }
                        }
                        productAdapter.submitList(products);
                        // chạy layout animation cho list
                        if (productsView != null) {
                            productsView.scheduleLayoutAnimation();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                        setProductsLoading(false);
                        productAdapter.submitList(new ArrayList<>());
                    }
                });
    }

    // ---------------- Search suggestion ----------------
    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }

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

    private void setSuggestLoading(boolean loading) {
        if (progressSearch != null)
            progressSearch.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) recyclerSuggest.setVisibility(View.GONE);
    }

    private void fetchSuggestions(String keyword) {
        if (productApi == null) return;
        setSuggestLoading(true);

        productApi.getHomePagePost(
                        1, 20,
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

    private void fetchUnreadNotificationCount(int userId, TextView badgeView) {
        com.example.onlyfanshop.api.NotificationApi api =
                ApiClient.getPrivateClient(requireContext()).create(com.example.onlyfanshop.api.NotificationApi.class);

        api.getUnreadCount(userId).enqueue(new retrofit2.Callback<Long>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Long> call,
                                   @NonNull retrofit2.Response<Long> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    int unreadCount = response.body().intValue();
                    if (unreadCount > 0) {
                        badgeView.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                        badgeView.setVisibility(View.VISIBLE);
                    } else {
                        badgeView.setVisibility(View.GONE);
                    }
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<Long> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e("HomeFragment", "Lỗi khi lấy số thông báo chưa đọc", t);
                badgeView.setVisibility(View.GONE);
            }
        });
    }

    // ---------- Entrance animation helpers ----------
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Nếu muốn, slide cả fragment rồi slide nội dung:
            playFragmentSlideIn();
            if (getView() != null) {
                getView().postDelayed(this::playEnterSlideOppositeSidesTogether, 60);
            }
        } else {
            playFragmentSlideOut();
        }
    }


    private void prepareEnterTargets(List<View> order) {
        int dy = dpToPx(12);
        for (View target : order) {
            if (target == null) continue;
            target.setVisibility(View.VISIBLE); // đảm bảo visible để animate
            if (target == bannerContainer) {
                target.setAlpha(0f);
                target.setScaleX(0.98f);
                target.setScaleY(0.98f);
            } else {
                target.setAlpha(0f);
                target.setTranslationY(dy);
            }
        }
    }

    // Chạy tuần tự từng view trong "order"


    // GỌI HÀM NÀY thay cho bản sequential
    // 3) Hàm mới: slide 2 nửa theo 2 hướng, KHÔNG fade
    private void playEnterSlideOppositeSidesTogether() {
        if (!isAdded() || getView() == null) return;

        // Nhóm NỬA TRÊN: header + search + banner
        List<View> topGroup = new ArrayList<>();
        if (ivAvatar != null) topGroup.add(ivAvatar);
        if (tvUserNameView != null) topGroup.add(tvUserNameView);
        if (notifContainer != null) topGroup.add(notifContainer);
        if (searchBar != null) topGroup.add(searchBar);
        if (popularHeader != null)  topGroup.add(popularHeader);
        if (popularView != null)   topGroup.add(popularView);

        // Nhóm NỬA DƯỚI: popular + products
        List<View> bottomGroup = new ArrayList<>();
        if (bannerContainer != null) bottomGroup.add(bannerContainer);
        if (productsHeader != null) bottomGroup.add(productsHeader);
        if (productsView != null)  bottomGroup.add(productsView);

        // Slide distance: toàn chiều rộng màn (off-screen)
        int distance = getView().getWidth();
        if (distance <= 0) {
            distance = requireContext().getResources().getDisplayMetrics().widthPixels;
        }

        // Đặt trạng thái ẩn trước khi animate (không dùng alpha)
        prepareSlideTargets(topGroup, bottomGroup, distance);

        // Animate tất cả CÙNG LÚC, mượt
        final android.animation.TimeInterpolator interpolator = new FastOutSlowInInterpolator();
        final long duration = 360L; // rất nhanh, mượt mà

        for (View v : topGroup) {
            if (v == null) continue;
            v.animate()
                    .translationX(0f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start();
        }
        for (View v : bottomGroup) {
            if (v == null) continue;
            v.animate()
                    .translationX(0f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start();
        }
    }

    // 4) Chuẩn bị trạng thái ban đầu: nửa trên ở ngoài màn bên trái, nửa dưới ở ngoài màn bên phải
    private void prepareSlideTargets(List<View> topGroup, List<View> bottomGroup, int distance) {
        for (View v : topGroup) {
            if (v == null) continue;
            v.setVisibility(View.VISIBLE);
            v.setTranslationX(-distance); // từ trái sang
            // Không đổi alpha (tránh fade)
        }
        for (View v : bottomGroup) {
            if (v == null) continue;
            v.setVisibility(View.VISIBLE);
            v.setTranslationX(distance); // từ phải sang
        }
    }

// Hàm này bạn đã có sẵn; giữ nguyên để pre-hide tất cả targets
// private void prepareEnterTargets(List<View> order) { ... }
}