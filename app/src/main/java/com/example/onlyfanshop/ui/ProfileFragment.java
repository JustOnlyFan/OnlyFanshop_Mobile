package com.example.onlyfanshop.ui;

import android.content.Intent;
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
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.response.UserResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private CardView btnEditProfile;
    private View btnMyStores, btnSupport, btnChat, btnPinCode, btnLogout;
    private SwitchCompat switchPushNotif, switchFaceId;

    private TextView tvProfileName, tvProfileEmail;

    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupClickListeners();

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
        btnMyStores = view.findViewById(R.id.btnMyStores);
        btnSupport = view.findViewById(R.id.btnSupport);
        btnChat = view.findViewById(R.id.btnChat);
        btnPinCode = view.findViewById(R.id.btnPinCode);
        btnLogout = view.findViewById(R.id.btnLogout);
        switchPushNotif = view.findViewById(R.id.switchPushNotif);
        switchFaceId = view.findViewById(R.id.switchFaceId);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            // Điều hướng sang EditProfileFragment và giữ nguyên bottom nav (vì replace trong container)
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
        btnMyStores.setOnClickListener(v -> Toast.makeText(requireContext(), "My Stores clicked", Toast.LENGTH_SHORT).show());
        btnSupport.setOnClickListener(v -> Toast.makeText(requireContext(), "Support clicked", Toast.LENGTH_SHORT).show());

        // Thay link phần chat: dùng cùng logic với MainActivity (mở ChatRoom cho CUSTOMER, ChatList cho ADMIN/khác)
        btnChat.setOnClickListener(v -> openChatEntry());

        btnPinCode.setOnClickListener(v -> Toast.makeText(requireContext(), "PIN Code clicked", Toast.LENGTH_SHORT).show());
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        switchPushNotif.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Push notifications: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchFaceId.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Face ID: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
    }

    // Mở chat theo cùng logic với MainActivity.btnOpenTestChat
    private void openChatEntry() {
        // Cố gắng đọc role từ currentUser (đã fetch từ API)
        String role = null;
        Integer userId = null;
        if (currentUser != null) {
            try {
                role = currentUser.getRole(); // yêu cầu model User có getRole()
            } catch (Exception ignored) {}
            try {
                userId = currentUser.getUserID(); // yêu cầu model User có getUserID()
            } catch (Exception ignored) {}
        }

        if ("CUSTOMER".equals(role)) {
            // Khách: mở ChatRoomActivity để chat với admin, giống MainActivity
            String customerId = FirebaseAuth.getInstance().getUid();
            if (customerId == null) {
                if (userId != null) {
                    customerId = "customer_" + userId;
                } else if (currentUser != null && currentUser.getUsername() != null) {
                    customerId = "customer_" + currentUser.getUsername();
                } else {
                    customerId = "customer_" + System.currentTimeMillis();
                }
            }
            // Lưu ý: thay "admin_uid" bằng UID admin thật của bạn (có thể lưu trong strings.xml)
            String adminId = "admin_uid";
            String conversationId = customerId.compareTo(adminId) < 0
                    ? customerId + "_" + adminId
                    : adminId + "_" + customerId;

            Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.chat.ChatRoomActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("customerName", "Admin");
            startActivity(intent);
        } else if ("ADMIN".equals(role)) {
            // Admin: mở danh sách hội thoại
            Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.chat.ChatListActivity.class);
            startActivity(intent);
        } else {
            // Không rõ role: fallback mở danh sách
            Intent intent = new Intent(requireContext(), com.example.onlyfanshop.ui.chat.ChatListActivity.class);
            startActivity(intent);
        }
    }

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
    }

    private void showLogoutDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    android.content.SharedPreferences prefs = requireContext().getApplicationContext()
                            .getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE);
                    prefs.edit().remove("jwt_token").apply();
                    com.example.onlyfanshop.api.ApiClient.clearAuthToken();
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}