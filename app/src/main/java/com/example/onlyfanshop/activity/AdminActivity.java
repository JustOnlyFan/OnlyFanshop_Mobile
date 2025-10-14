package com.example.onlyfanshop.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;


import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ui.ProfileFragment;
import com.example.onlyfanshop.ui.admin.ManagerFragment;
import com.example.onlyfanshop.ui.admin.OrderFragment;
import com.example.onlyfanshop.ui.admin.StoreFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {
    BottomNavigationView bottomNav;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        bottomNav = findViewById(R.id.bottomNav);

// Mặc định hiển thị trang Manager
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, new ManagerFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_manager) {
                selectedFragment = new ManagerFragment();
            } else if (id == R.id.nav_store) {
                selectedFragment = new StoreFragment();
            } else if (id == R.id.nav_order) {
                selectedFragment = new OrderFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mainFragmentContainer, selectedFragment)
                        .commit();
            }

            return true;
        });

    }
}