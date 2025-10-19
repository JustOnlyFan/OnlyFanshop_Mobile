package com.example.onlyfanshop.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.ChangePasswordActivity;
import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.response.UserResponse;
import com.example.onlyfanshop.ui.order.OrderHistoryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private CardView btnEditProfile;
    private View btnSupport, btnChat, btnResetPassword, btnLogout, btnHistory;
    private SwitchCompat switchPushNotif, switchFaceId;

    private TextView tvProfileName, tvProfileEmail;

    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String token = ApiClient.getToken(requireContext());
        if (token == null || token.trim().isEmpty()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new PleaseSignInFragment(), "PLEASE_SIGN_IN")
                    .commit();
            return new View(requireContext()); // Trả về view rỗng, tránh null pointer
        }

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupClickListeners();

        // 1. HIỂN THỊ NGAY TỪ SHARED PREFERENCES ĐỂ TRÁNH TRỄ
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Guest");
        String email = prefs.getString("email", "");
        String role = prefs.getString("role", "");
        tvProfileName.setText(username);
        tvProfileEmail.setText(email);
        if ("ADMIN".equals(role)) btnChat.setVisibility(View.GONE);
        else btnChat.setVisibility(View.VISIBLE);

        // 2. GỌI API ĐỂ CẬP NHẬT THÔNG TIN MỚI NHẤT
        fetchUser();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Khi quay lại từ EditProfileFragment, tự refresh user
        fetchUser();
    }

    private void initViews(View view) {
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSupport = view.findViewById(R.id.btnSupport);
        btnChat = view.findViewById(R.id.btnChat);
        btnHistory = view.findViewById(R.id.History);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        btnLogout = view.findViewById(R.id.btnLogout);
        switchPushNotif = view.findViewById(R.id.switchPushNotif);
        switchFaceId = view.findViewById(R.id.switchFaceId);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.mainFragmentContainer, new EditProfileFragment(), "EDIT_PROFILE")
                    .addToBackStack("EDIT_PROFILE")
                    .commit();
        });
        btnSupport.setOnClickListener(v -> Toast.makeText(requireContext(), "Support clicked", Toast.LENGTH_SHORT).show());
        btnChat.setOnClickListener(v -> openChatEntry());
        btnResetPassword.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), OrderHistoryActivity.class)));
        switchPushNotif.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Push notifications: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchFaceId.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Face ID: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
    }

    private void openChatEntry() {
        String role = null;
        Integer userId = null;
        if (currentUser != null) {
            try { role = currentUser.getRole(); } catch (Exception ignored) {}
            try { userId = currentUser.getUserID(); } catch (Exception ignored) {}
        }

        if ("CUSTOMER".equals(role)) {
            String customerId = FirebaseAuth.getInstance().getUid();
            if (customerId == null) {
                if (userId != null) customerId = "customer_" + userId;
                else if (currentUser != null && currentUser.getUsername() != null) customerId = "customer_" + currentUser.getUsername();
                else customerId = "customer_" + System.currentTimeMillis();
            }
            String adminId = "admin_uid";
            String conversationId = customerId.compareTo(adminId) < 0
                    ? customerId + "_" + adminId
                    : adminId + "_" + customerId;

            Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.chat.ChatRoomActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("customerName", "Admin");
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "Chat feature is only available for customers", Toast.LENGTH_SHORT).show();
        }
    }

    // GỌI API VÀ LƯU VÀO SHARED PREFERENCES ĐỂ LẦN SAU LOAD NHANH
    private void fetchUser() {
        ProfileApi api = ApiClient.getPrivateClient(requireContext()).create(ProfileApi.class);
        api.getUser().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse body = response.body();
                    if (body.getStatusCode() == 200 && body.getData() != null) {
                        currentUser = body.getData();
                        bindUser(currentUser);

                        // CẬP NHẬT SHARED PREFERENCES CHO LẦN SAU
                        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("username", currentUser.getUsername())
                                .putString("email", currentUser.getEmail())
                                .putString("role", currentUser.getRole())
                                .apply();
                    } else {
                        Toast.makeText(requireContext(), body.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                    // TODO: Điều hướng Login
                } else {
                    Toast.makeText(requireContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUser(User user) {
        tvProfileName.setText(user.getUsername() != null ? user.getUsername() : "Guest");
        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        if (user.getRole() != null && "ADMIN".equals(user.getRole())) {
            btnChat.setVisibility(View.GONE);
        } else {
            btnChat.setVisibility(View.VISIBLE);
        }
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    SharedPreferences prefs = requireContext().getApplicationContext()
                            .getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                    prefs.edit().remove("jwt_token").remove("userId").remove("username").remove("email").remove("role").apply();
                    ApiClient.clearAuthToken();
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof DashboardActivity) {
                        DashboardActivity dashboard = (DashboardActivity) requireActivity();
                        dashboard.updateCartBadgeNow();
                        BottomNavigationView bottomNav = dashboard.findViewById(R.id.bottomNav);
                        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
                        dashboard.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragmentContainer, new HomeFragment(), "HOME_FRAGMENT")
                                .commit();
                    } else {
                        Intent intent = new Intent(requireContext(), DashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}