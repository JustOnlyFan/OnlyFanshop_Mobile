package com.example.onlyfanshop.ui;

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
        btnChat.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), com.example.onlyfanshop.ui.chat.ChatListActivity.class)));
        btnPinCode.setOnClickListener(v -> Toast.makeText(requireContext(), "PIN Code clicked", Toast.LENGTH_SHORT).show());
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        switchPushNotif.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Push notifications: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());

        switchFaceId.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(requireContext(), "Face ID: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
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