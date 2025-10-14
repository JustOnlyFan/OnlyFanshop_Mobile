package com.example.onlyfanshop.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer; // optional nếu dùng anonymous class ở Cách 2

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.utils.AppEvents; // ĐẢM BẢO import đúng class AppEvents bạn đã tạo
import com.example.onlyfanshop.ui.CategoryFragment;
import com.example.onlyfanshop.ui.HomeFragment;
import com.example.onlyfanshop.ui.MapFragment;
import com.example.onlyfanshop.ui.ProfileFragment;
import com.example.onlyfanshop.ui.cart.CartFragment;
import com.example.onlyfanshop.utils.BadgeUtils;
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
        initNavigation(savedInstanceState);

        // Đặt observe BÊN TRONG phương thức onCreate
        AppEvents.get().cartUpdated().observe(this, ts -> {
            updateCartBadgeNow();
        });

        // Cập nhật badge lần đầu
        updateCartBadgeNow();
    }

    // Cho phép Fragment/Activity khác gọi cập nhật badge
    public void updateCartBadgeNow() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            int userId = sharedPreferences.getInt("userId", -1);
            if (userId != -1 && bottomNav != null) {
                badgeUtils.updateCartBadge(this, bottomNav, userId);
            } else if (bottomNav != null) {
                // Nếu userId không tồn tại, xóa badge
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
        int offset = bottomNav.getHeight();
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
            bottomNav.setSelectedItemId(currentSelectedId);
            loadFragmentById(currentSelectedId);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_home);
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (currentSelectedId != id) {
                currentSelectedId = id;
                loadFragmentById(id);
            }
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            // Optional
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

    private void loadFragmentById(int id) {
        Fragment fragment;
        String tag;

        if (id == R.id.nav_home) {
            fragment = new HomeFragment();
            tag = "HOME";
        } else if (id == R.id.nav_search) {
            fragment = new CategoryFragment();
            tag = "SEARCH";
        } else if (id == R.id.nav_car) {
            String username = getUsernameForNav();
            fragment = CartFragment.newInstance(username);
            tag = "CART";
        } else if (id == R.id.nav_shop) {
            fragment = new MapFragment();
            tag = "MAP";
        } else if (id == R.id.nav_profile) {
            fragment = new ProfileFragment();
            tag = "PROFILE";
        } else {
            fragment = new HomeFragment();
            tag = "HOME";
        }

        loadFragment(fragment, tag);
    }

    private void loadFragment(@NonNull Fragment fragment) {
        loadFragment(fragment, fragment.getClass().getSimpleName());
    }

    private void loadFragment(@NonNull Fragment fragment, String tag) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.mainFragmentContainer, fragment, tag)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading fragment: " + tag, e);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_ITEM, currentSelectedId);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        if (currentSelectedId != R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            currentSelectedId = R.id.nav_home;
            loadFragmentById(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}