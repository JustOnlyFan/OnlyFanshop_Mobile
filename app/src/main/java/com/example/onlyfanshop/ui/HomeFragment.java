package com.example.onlyfanshop.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
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

    private static final int LOOP_COUNT = 10000;
    private List<BannerModel> bannerList = new ArrayList<>();

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

    private ImageView btnNotif;

    // Views for entrance animation
    private View headerContainer;
    private View searchBar;
    private View bannerContainer;
    private View popularHeader;
    private View productsHeader;

    private View ivAvatar;
    private View tvUserNameView;
    private View notifContainer;

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || viewPagerBanner == null || bannerAdapter == null || bannerList == null || bannerList.isEmpty())
                return;
            int next = viewPagerBanner.getCurrentItem() + 1;
            viewPagerBanner.setCurrentItem(next, true);
            sliderHandler.postDelayed(this, SLIDER_INTERVAL_MS);
        }
    };

    // Slide-in/out for the whole fragment
    private void playFragmentSlideIn() {
        if (!isAdded() || getView() == null) return;
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right);
        getView().startAnimation(anim);
    }

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
        View popularHeaderBlock = (View) v.findViewById(R.id.tvPopularTitle).getParent();
        View productsHeaderBlock = (View) v.findViewById(R.id.tvProductsTitle).getParent();
        popularHeader = popularHeaderBlock;
        productsHeader = productsHeaderBlock;

        // Make the entire search bar clickable to open SearchFragment
        ImageView searchIcon = v.findViewById(R.id.searchIcon);
        EditText et = v.findViewById(R.id.editTextText);
        ImageView filterIcon = v.findViewById(R.id.imageViewFilter);

        View.OnClickListener openSearch = view1 -> openSearchFragmentDirect();

        if (searchBar != null) searchBar.setOnClickListener(openSearch);
        if (searchIcon != null) searchIcon.setOnClickListener(openSearch);
        if (filterIcon != null) filterIcon.setOnClickListener(openSearch);
        if (et != null) {
            // prevent keyboard in Home; click EditText also opens SearchFragment
            et.setFocusable(false);
            et.setFocusableInTouchMode(false);
            et.setClickable(true);
            et.setCursorVisible(false);
            et.setOnClickListener(openSearch);
        }

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
        viewPagerBanner.setOffscreenPageLimit(1);
        CompositePageTransformer composite = new CompositePageTransformer();
        composite.addTransformer(new MarginPageTransformer(40));
        viewPagerBanner.setPageTransformer(composite);
        viewPagerBanner.post(() -> {
            RecyclerView rv = (RecyclerView) viewPagerBanner.getChildAt(0);
            if (rv != null) rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        });
        viewPagerBanner.post(() -> viewPagerBanner.setCurrentItem(LOOP_COUNT / 2, false));
        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, SLIDER_INTERVAL_MS);
            }
        });

        // Lazy load
        v.postDelayed(this::loadBannersFromRealtimeDb, 100);

        // Popular
        popularView = v.findViewById(R.id.popularView);
        progressBarPopular = v.findViewById(R.id.progressBarPopular);
        popularView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        popularView.setNestedScrollingEnabled(false);
        popularView.setHasFixedSize(true);
        // Tăng cache size để scroll mượt hơn
        popularView.setItemViewCacheSize(20);
        // Tắt animator để scroll nhanh hơn
        popularView.setItemAnimator(null);
        // Tắt layout animation để load nhanh hơn
        // popularView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down));

        popularAdapter = new PopularAdapter(item -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, item.getProductID());
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        popularView.setAdapter(popularAdapter);

        productApi = ApiClient.getPrivateClient(requireContext()).create(ProductApi.class);
        v.postDelayed(this::loadPopular, 150);

        // See all -> open SearchFragment
        TextView tvSeeAll = v.findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(view -> openSearchFragmentDirect());
        }

        // Products
        productsView = v.findViewById(R.id.productsView);
        progressBarProducts = v.findViewById(R.id.progressBarProducts);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        productsView.setLayoutManager(gridLayoutManager);
        productsView.setNestedScrollingEnabled(false);
        productsView.setHasFixedSize(true);
        // Tăng cache size để scroll mượt hơn với GridLayout
        productsView.setItemViewCacheSize(30);
        // Tắt animator để scroll nhanh hơn
        productsView.setItemAnimator(null);
        // Tắt layout animation để load nhanh hơn
        // productsView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down));
        // Tăng drawing cache để render nhanh hơn
        productsView.setDrawingCacheEnabled(true);
        productsView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

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

        v.postDelayed(this::loadProducts, 200);

        // User name
        fetchUserName();

        // Entrance animation
        View root = v;
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                playFragmentSlideIn();
                root.postDelayed(() -> playEnterSlideOppositeSidesTogether(), 60);
                return true;
            }
        });
    }

    private void openSearchFragmentDirect() {
        if (!isAdded() || getView() == null) return;
        View parent = (View) getView().getParent();
        if (parent == null) return;
        int containerId = parent.getId();

        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(containerId, new SearchFragment())
                .addToBackStack("SearchFragment")
                .commit();
    }

    // -------- Banner --------
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

        bannerAdapter = null;
        popularAdapter = null;
        productAdapter = null;

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
                        // Tắt layout animation để load nhanh hơn
                        // if (popularView != null) {
                        //     popularView.scheduleLayoutAnimation();
                        // }
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
        // Giảm từ 1000 xuống 50 products để tăng performance
        // User có thể scroll để xem thêm nếu cần (pagination)
        productApi.getHomePagePost(1, 50, "ProductID", "ASC", null, null, null)
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
                        // Tắt layout animation cho large lists để tránh lag
                        // if (productsView != null && products.size() <= 20) {
                        //     productsView.scheduleLayoutAnimation();
                        // }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<HomePageData>> call, @NonNull Throwable t) {
                        setProductsLoading(false);
                        productAdapter.submitList(new ArrayList<>());
                    }
                });
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
            playFragmentSlideIn();
            if (getView() != null) {
                getView().postDelayed(this::playEnterSlideOppositeSidesTogether, 60);
            }
        } else {
            playFragmentSlideOut();
        }
    }

    private void playEnterSlideOppositeSidesTogether() {
        if (!isAdded() || getView() == null) return;

        List<View> topGroup = new ArrayList<>();
        if (ivAvatar != null) topGroup.add(ivAvatar);
        if (tvUserNameView != null) topGroup.add(tvUserNameView);
        if (notifContainer != null) topGroup.add(notifContainer);
        if (searchBar != null) topGroup.add(searchBar);
        if (popularHeader != null)  topGroup.add(popularHeader);
        if (popularView != null)   topGroup.add(popularView);

        List<View> bottomGroup = new ArrayList<>();
        if (bannerContainer != null) bottomGroup.add(bannerContainer);
        if (productsHeader != null) bottomGroup.add(productsHeader);
        if (productsView != null)  bottomGroup.add(productsView);

        int distance = getView().getWidth();
        if (distance <= 0) {
            distance = requireContext().getResources().getDisplayMetrics().widthPixels;
        }

        prepareSlideTargets(topGroup, bottomGroup, distance);

        final android.animation.TimeInterpolator interpolator = new FastOutSlowInInterpolator();
        final long duration = 360L;

        for (View v : topGroup) {
            if (v == null) continue;
            v.animate().translationX(0f).setDuration(duration).setInterpolator(interpolator).start();
        }
        for (View v : bottomGroup) {
            if (v == null) continue;
            v.animate().translationX(0f).setDuration(duration).setInterpolator(interpolator).start();
        }
    }

    private void prepareSlideTargets(List<View> topGroup, List<View> bottomGroup, int distance) {
        for (View v : topGroup) {
            if (v == null) continue;
            v.setVisibility(View.VISIBLE);
            v.setTranslationX(-distance);
        }
        for (View v : bottomGroup) {
            if (v == null) continue;
            v.setVisibility(View.VISIBLE);
            v.setTranslationX(distance);
        }
    }

    private int dpToPx(int dp) {
        if (!isAdded()) return dp;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}