package com.example.onlyfanshop.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.utils.AppEvents;
import com.example.onlyfanshop.ui.CategoryFragment;
import com.example.onlyfanshop.ui.HomeFragment;
import com.example.onlyfanshop.ui.MapFragment;
import com.example.onlyfanshop.ui.ProfileFragment;
import com.example.onlyfanshop.ui.cart.CartFragment;
import com.example.onlyfanshop.utils.BadgeUtils;
import com.example.onlyfanshop.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final String STATE_SELECTED_ITEM = "state_selected_bottom_item";

    private BottomNavigationView bottomNav;
    private FrameLayout fragmentContainer;
    private View root;
    private int currentSelectedId = R.id.nav_home;
    private final BadgeUtils badgeUtils = new BadgeUtils();

    // Fragment cache
    private Fragment homeFragment;
    private Fragment categoryFragment;
    private Fragment mapFragment;
    private Fragment cartFragment;
    private Fragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        root = findViewById(R.id.dashboardRoot);
        bottomNav = findViewById(R.id.bottomNav);
        fragmentContainer = findViewById(R.id.mainFragmentContainer);

        applyEdgeToEdgeInsets();
        initFirebaseTest();
        initFragmentCache();
        initNavigation(savedInstanceState);
        setupBackHandler();

        AppEvents.get().cartUpdated().observe(this, ts -> updateCartBadgeNow());
        updateCartBadgeNow();
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                if (currentSelectedId != R.id.nav_home) {
                    int fromOrder = getNavOrder(currentSelectedId);
                    int toOrder = getNavOrder(R.id.nav_home);
                    boolean forward = toOrder > fromOrder; // thường là false → trượt sang trái để về Home
                    bottomNav.setSelectedItemId(R.id.nav_home);
                    showFragmentById(R.id.nav_home, forward);
                    currentSelectedId = R.id.nav_home;
                } else {
                    setEnabled(false);
                    DashboardActivity.super.onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void initFragmentCache() {
        homeFragment = new HomeFragment();
        categoryFragment = new CategoryFragment();
        mapFragment = new MapFragment();
        profileFragment = new ProfileFragment();
        cartFragment = null;
    }

    public void updateCartBadgeNow() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            int userId = sharedPreferences.getInt("userId", -1);
            if (userId != -1 && bottomNav != null) {
                badgeUtils.updateCartBadge(this, bottomNav, userId);
            } else if (bottomNav != null) {
                badgeUtils.clearCartBadge(bottomNav);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to update cart badge", e);
        }
    }

    private void applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), sys.bottom);
            v.post(this::adjustContainerBottomPadding);
            return insets;
        });
        bottomNav.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> adjustContainerBottomPadding());
        ViewCompat.requestApplyInsets(root);
    }

    private void adjustContainerBottomPadding() {
        if (fragmentContainer == null || bottomNav == null) return;
        int offset = (bottomNav.getVisibility() == View.VISIBLE) ? bottomNav.getHeight() : 0;
        fragmentContainer.setPadding(
                fragmentContainer.getPaddingLeft(),
                fragmentContainer.getPaddingTop(),
                fragmentContainer.getPaddingRight(),
                offset
        );
    }

    private void initFirebaseTest() {
        FirebaseDatabase.getInstance().getReference("test_connection")
                .setValue("Hello Firebase!")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Kết nối Firebase thành công!");
                    } else {
                        Log.e(TAG, "❌ Lỗi kết nối Firebase", task.getException());
                    }
                });
    }

    private void initNavigation(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentSelectedId = savedInstanceState.getInt(STATE_SELECTED_ITEM, R.id.nav_home);
        } else {
            currentSelectedId = R.id.nav_home;
        }
        // Hiển thị ban đầu không cần animation hướng
        showFragmentById(currentSelectedId, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (currentSelectedId != id) {
                int fromOrder = getNavOrder(currentSelectedId);
                int toOrder = getNavOrder(id);
                boolean forward = toOrder > fromOrder; // sang phải → forward, sang trái → backward
                showFragmentById(id, forward);
                currentSelectedId = id;
            }
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            // Optional: scroll to top or refresh
        });
    }

    private String getUsernameForNav() {
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            username = prefs.getString("username", null);
        }
        return username;
    }

    // Xác định thứ tự tabs trên BottomNavigation để suy ra hướng
    private int getNavOrder(int id) {
        if (id == R.id.nav_home) return 0;
        if (id == R.id.nav_search) return 1;
        if (id == R.id.nav_car) return 2;
        if (id == R.id.nav_shop) return 3;
        if (id == R.id.nav_profile) return 4;
        return Integer.MAX_VALUE; // unknown
    }

    private void showFragmentById(int id, boolean forward) {
        Fragment fragmentToShow;

        if (id == R.id.nav_home) {
            fragmentToShow = homeFragment;
        } else if (id == R.id.nav_search) {
            fragmentToShow = categoryFragment;
        } else if (id == R.id.nav_car) {
            String username = getUsernameForNav();
            if (cartFragment != null && cartFragment.isAdded()) {
                try {
                    getSupportFragmentManager().beginTransaction().remove(cartFragment).commitNow();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to remove previous cartFragment synchronously", e);
                    getSupportFragmentManager().beginTransaction().remove(cartFragment).commit();
                    getSupportFragmentManager().executePendingTransactions();
                }
                cartFragment = null;
            }
            cartFragment = CartFragment.newInstance(username);
            fragmentToShow = cartFragment;
        } else if (id == R.id.nav_shop) {
            fragmentToShow = mapFragment;
        } else if (id == R.id.nav_profile) {
            fragmentToShow = profileFragment;
        } else {
            fragmentToShow = homeFragment;
        }

        showFragment(fragmentToShow, forward);
    }

    private void showFragment(Fragment fragmentToShow, boolean forward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // CHỈ sử dụng slide in/out theo hướng, không dùng fade để tránh chồng hiệu ứng
        if (forward) {
            // Sang phải (target nằm bên phải current)
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // enter
                    R.anim.slide_out_left,  // exit
                    R.anim.slide_in_left,   // popEnter (khi backstack pop)
                    R.anim.slide_out_right  // popExit
            );
        } else {
            // Sang trái (target nằm bên trái current)
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,   // enter
                    R.anim.slide_out_right, // exit
                    R.anim.slide_in_right,  // popEnter
                    R.anim.slide_out_left   // popExit
            );
        }

        // Hide all fragments if they are added
        if (homeFragment.isAdded()) transaction.hide(homeFragment);
        if (categoryFragment.isAdded()) transaction.hide(categoryFragment);
        if (mapFragment.isAdded()) transaction.hide(mapFragment);
        if (cartFragment != null && cartFragment.isAdded() && cartFragment != fragmentToShow) transaction.hide(cartFragment);
        if (profileFragment.isAdded()) transaction.hide(profileFragment);

        // Add if not added, else show
        if (!fragmentToShow.isAdded()) {
            transaction.add(R.id.mainFragmentContainer, fragmentToShow);
        } else {
            transaction.show(fragmentToShow);
        }

        // Control bottom navigation visibility based on current fragment
        if (fragmentToShow instanceof CartFragment) {
            bottomNav.setVisibility(View.GONE);
        } else {
            bottomNav.setVisibility(View.VISIBLE);
        }

        try {
            transaction.commitNow();
        } catch (IllegalStateException e) {
            Log.w(TAG, "commitNow failed, falling back to commit + executePendingTransactions", e);
            transaction.commit();
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_ITEM, currentSelectedId);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = LocaleHelper.getLanguage(newBase);
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    public BottomNavigationView getBottomNav() {
        return bottomNav;
    }
}