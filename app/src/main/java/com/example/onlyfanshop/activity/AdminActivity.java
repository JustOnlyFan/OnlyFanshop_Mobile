package com.example.onlyfanshop.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
            
            // Xác định hướng chuyển (forward = sang phải)
            boolean forward = false;
            int currentId = bottomNav.getSelectedItemId();
            
            // Map order: Manager < Store < Order < Profile
            int currentPos = getFragmentPosition(currentId);
            int targetPos = getFragmentPosition(id);
            forward = targetPos > currentPos;

            if (id == R.id.nav_manager) {
                selectedFragment = new ManagerFragment();
            } else if (id == R.id.nav_order) {
                selectedFragment = new OrderFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                androidx.fragment.app.FragmentTransaction transaction = 
                        getSupportFragmentManager().beginTransaction();
                
                // Thêm smooth transitions
                if (forward) {
                    transaction.setCustomAnimations(
                            R.anim.slide_in_right,  // enter
                            R.anim.slide_out_left,  // exit
                            R.anim.slide_in_left,   // popEnter
                            R.anim.slide_out_right  // popExit
                    );
                } else {
                    transaction.setCustomAnimations(
                            R.anim.slide_in_left,   // enter
                            R.anim.slide_out_right, // exit
                            R.anim.slide_in_right,  // popEnter
                            R.anim.slide_out_left   // popExit
                    );
                }
                
                transaction.setReorderingAllowed(true)
                        .replace(R.id.mainFragmentContainer, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
    
    private int getFragmentPosition(int itemId) {
        if (itemId == R.id.nav_manager) return 0;
        if (itemId == R.id.nav_order) return 1;
        if (itemId == R.id.nav_profile) return 2;
        return 0;

    }
}