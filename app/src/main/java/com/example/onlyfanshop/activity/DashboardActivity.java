package com.example.onlyfanshop.activity;

import static com.example.onlyfanshop.ultils.BadgeUtils.updateCartBadge;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ui.CategoryFragment;
import com.example.onlyfanshop.ui.HomeFragment;
import com.example.onlyfanshop.ui.MapFragment;
import com.example.onlyfanshop.ui.ProfileFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final String STATE_SELECTED_ITEM = "state_selected_bottom_item";

    private ChipNavigationBar bottomNav;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        bottomNav = findViewById(R.id.bottomNav);

        initFirebaseTest();
        initNavigation(savedInstanceState);
        initSearchActions();

//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
//        updateCartBadge(this, bottomNavigationView, userId);
    }

    private void initFirebaseTest() {
        FirebaseDatabase.getInstance().getReference("test_connection")
                .setValue("Hello Firebase!")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FIREBASE", "✅ Kết nối Firebase thành công!");
                    } else {
                        Log.e("FIREBASE", "❌ Lỗi kết nối Firebase", task.getException());
                    }
                });
    }

    private void initNavigation(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            bottomNav.setItemSelected(R.id.nav_home, true);
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(id -> {
            Fragment fragment;
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_category) {
                fragment = new CategoryFragment();
            } else if (id == R.id.nav_fav) {
                fragment = new MapFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
            }
            loadFragment(fragment);
        });
    }

    private void initSearchActions() {
        // Bạn có thể gắn sự kiện search ở đây nếu muốn chuyển vào HomeFragment thông qua interface
    }

    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }
}