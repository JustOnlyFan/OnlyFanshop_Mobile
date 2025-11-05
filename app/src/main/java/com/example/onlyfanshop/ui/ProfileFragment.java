package com.example.onlyfanshop.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.ChangePasswordActivity;
import com.example.onlyfanshop.activity.DashboardActivity;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.OrderApi;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.model.User;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.UserResponse;
import com.example.onlyfanshop.service.NotificationListenerService;
import com.example.onlyfanshop.ui.order.OrderHistoryActivity;
import com.example.onlyfanshop.ui.order.UserOrderFragment;
import com.example.onlyfanshop.ui.chat.ChatRoomActivity;
import com.example.onlyfanshop.api.ChatApi;
import com.example.onlyfanshop.service.ChatService;
import com.example.onlyfanshop.utils.AppPreferences;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.RemoteModelManager; // Dùng để xóa mô hình
import com.google.mlkit.nl.translate.TranslateRemoteModel; // Dùng để xác định mô hình xóa

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private CardView btnEditProfile;
    private View btnSupport, btnResetPassword, btnLogout, btnChatWithAdmin, btnLanguage;
    private TextView tvProfileName, tvProfileEmail, tvSeeAllOrders, tvBadgePending, tvBadgeShipping, tvBadgeDelivered;
    private User currentUser;
    private String currentSourceLangCode;
    private String currentTargetLangCode;
    private com.google.mlkit.nl.translate.Translator mlKitTranslator;
    private TranslatorOptions currentTranslatorOptions;

    private LinearLayout btnPendingConfirm, btnReadyToShip, btnShipping;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String token = ApiClient.getToken(requireContext());
        if (token == null || token.trim().isEmpty()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new PleaseSignInFragment(), "PLEASE_SIGN_IN")
                    .commit();
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupClickListeners();
        loadOrderStatusCount();
        // Áp dụng padding đáy động theo system insets + chiều cao BottomNavigationView
        applySystemInsetsPadding(view);

        // 1. DISPLAY IMMEDIATELY FROM SHARED PREFERENCES TO AVOID DELAY
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Guest");
        String email = prefs.getString("email", "");
        tvProfileName.setText(username);
        tvProfileEmail.setText(email);

        // 2. CALL API TO UPDATE LATEST INFORMATION
        fetchUser();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUser();
    }

    private void initViews(View view) {
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSupport = view.findViewById(R.id.btnSupport);
        btnChatWithAdmin = view.findViewById(R.id.btnChatWithAdmin);
        tvSeeAllOrders = view.findViewById(R.id.tvSeeAllOrders);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);

        btnLanguage = view.findViewById(R.id.btnLanguage);

        btnPendingConfirm = view.findViewById(R.id.btnPendingConfirm);
        btnReadyToShip = view.findViewById(R.id.btnReadyToShip);
        btnShipping = view.findViewById(R.id.btnShipping);
        tvBadgePending = view.findViewById(R.id.badgePending);
        tvBadgeShipping = view.findViewById(R.id.badgeShipping);
        tvBadgeDelivered = view.findViewById(R.id.badgeDelivered);
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
        btnChatWithAdmin.setOnClickListener(v -> openChatWithAdmin());
        btnResetPassword.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        tvSeeAllOrders.setOnClickListener(v -> startActivity(new Intent(requireContext(), OrderHistoryActivity.class)));
        


        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageSelectionDialog();
            }
        });
        btnPendingConfirm.setOnClickListener(v -> openOrderHistory("PENDING"));
        btnReadyToShip.setOnClickListener(v -> openOrderHistory("APPROVED"));
        btnShipping.setOnClickListener(v -> openOrderHistory("SHIPPED"));

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
                    Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUser(User user) {
        tvProfileName.setText(user.getUsername() != null ? user.getUsername() : "Guest");
        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc là muốn đăng xuất?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    SharedPreferences prefs = requireContext().getApplicationContext()
                            .getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                    prefs.edit().remove("jwt_token").remove("userId").remove("username").remove("email").remove("role").apply();
                    ApiClient.clearAuthToken();
                    Toast.makeText(requireContext(), "Đăng xuất", Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof DashboardActivity) {
                        DashboardActivity dashboard = (DashboardActivity) requireActivity();
                        dashboard.updateCartBadgeNow();
                        BottomNavigationView bottomNav = dashboard.findViewById(R.id.bottomNav);
                        Intent serviceIntent = new Intent(requireContext(), NotificationListenerService.class);
                        requireContext().stopService(serviceIntent);
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

    private void openChatWithAdmin() {
        // ✅ Immediate response - navigate to chat room instantly
        String currentUserId = AppPreferences.getUserId(requireContext());
        String currentUsername = AppPreferences.getUsername(requireContext());

        // Generate room ID immediately (same logic as backend)
        String roomId = "chatRoom_" + currentUsername + "_" + currentUserId;

        // Navigate to chat room immediately
        Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);

        // ✅ Background task - ensure room exists in Firebase (non-blocking)
        ChatApi chatApi = ApiClient.getPrivateClient(requireContext()).create(ChatApi.class);
        ChatService chatService = new ChatService(chatApi, requireContext());

        // This runs in background, doesn't block UI
        chatService.getOrCreateCustomerRoom(new ChatService.RoomCallback() {
            @Override
            public void onSuccess(String roomId) {
                // Room created/verified in background
                Log.d("ProfileFragment", "Chat room verified: " + roomId);
            }

            @Override
            public void onError(String error) {
                // Log error but don't show to user (already in chat room)
                Log.e("ProfileFragment", "Background room creation failed: " + error);
            }
        });
    }

    //---------------------------------------------------------
    // ML KIT TRANSLATION LOGIC
    //---------------------------------------------------------

    private void showLanguageSelectionDialog() {
        // Target language list
        final String[] languages = {"Vietnamese (vi)", "English (en)", "Japanese (ja)", "Spanish (es)"};
        final String[] languageCodes = {"vi", "en", "ja", "es"};

        // Default source language (assumed input is English)
        final String sourceLangCode = TranslateLanguage.ENGLISH;

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Translation Language")
                .setItems(languages, (dialog, which) -> {
                    String targetLangCode = languageCodes[which];

                    // 1. Initialize Translator with selected source/target language
                    initializeTranslator(sourceLangCode, targetLangCode);

                    // 2. Call translation function (user name)
                    translateUserName(tvProfileName.getText().toString(), languages[which]);

                    dialog.dismiss();
                })
                .show();
    }

    private void initializeTranslator(String sourceLangCode, String targetLangCode) {
        if (mlKitTranslator != null) {
            mlKitTranslator.close();
        }

        // ✅ Save language codes manually
        currentSourceLangCode = sourceLangCode;
        currentTargetLangCode = targetLangCode;

        currentTranslatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build();

        mlKitTranslator = Translation.getClient(currentTranslatorOptions);

        mlKitTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(v -> Log.d("MLKit", "Model has been downloaded/ready."))
                .addOnFailureListener(e -> handleModelDownloadFailure(mlKitTranslator, e));
    }

    private void handleModelDownloadFailure(com.google.mlkit.nl.translate.Translator mlKitTranslator, Exception e) {
        if (currentTargetLangCode == null || requireContext() == null) {
            Toast.makeText(requireContext(), "Target language could not be determined.", Toast.LENGTH_LONG).show();
            return;
        }

        TranslateRemoteModel modelToDelete = new TranslateRemoteModel.Builder(currentTargetLangCode).build();

        RemoteModelManager.getInstance()
                .deleteDownloadedModel(modelToDelete)
                .addOnSuccessListener(aVoid -> {
                    Log.w("MLKit", "Old model has been deleted. Retrying download...");
                    mlKitTranslator.downloadModelIfNeeded()
                            .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Model reloaded successfully.", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(re -> Toast.makeText(requireContext(), "Error reloading model: " + re.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(delE -> {
                    Toast.makeText(requireContext(), "Error deleting model: " + delE.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("MLKit", "Error deleting model: " + delE.getMessage());
                });
    }

    private void translateUserName(String textToTranslate, String targetLangName) {
        if (mlKitTranslator == null) {
            Toast.makeText(requireContext(), "Error: Translator not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Translating to " + targetLangName + "...", Toast.LENGTH_SHORT).show();

        mlKitTranslator.translate(textToTranslate)
                .addOnSuccessListener(translatedText -> {
                    // Success: Update TextView with translation
                    tvProfileName.setText(translatedText);
                    Toast.makeText(requireContext(), "Translation successful: " + translatedText, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failure: Handle error
                        Log.e("MLKit", "Translation error: " + e.getMessage());
                        Toast.makeText(requireContext(), "Translation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close translator to free memory
        if (mlKitTranslator != null) {
            mlKitTranslator.close();
        }
    }
    private void openOrderHistory(String status) {
        // Navigate to UserOrderFragment with status filter
        UserOrderFragment fragment = UserOrderFragment.newInstance(status);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.mainFragmentContainer, fragment, "USER_ORDER_FRAGMENT")
                .addToBackStack("USER_ORDER_FRAGMENT")
                .commit();
    }

    // Handle status bar (top) and navigation/bottom bar + BottomNavigationView (bottom)
    private void applySystemInsetsPadding(View root) {
        // Ensure ScrollView can scroll into padding
        if (root instanceof ScrollView) {
            ((ScrollView) root).setClipToPadding(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int systemTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            int bottomNavHeight = 0;
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNavHeight = bottomNav.getHeight();

                // If height is not ready yet, post an update after layout
                if (bottomNavHeight == 0) {
                    bottomNav.post(() -> {
                        int h = bottomNav.getHeight();
                        int desiredBottom = systemBottom + h;
                        v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), desiredBottom);
                    });
                }
            }

            int desiredBottom = systemBottom + bottomNavHeight;
            // Apply top inset to push content below status bar, preserve existing L/R paddings
            v.setPadding(v.getPaddingLeft(), systemTop, v.getPaddingRight(), desiredBottom);

            return insets;
        });

        // Request to apply insets immediately
        ViewCompat.requestApplyInsets(root);
    }
    private void loadOrderStatusCount() {
        Log.d("EditProfileFragment", "loadOrderStatusCount");
        OrderApi apiOrder = ApiClient.getPrivateClient(requireContext()).create(OrderApi.class);

        apiOrder.getBadgeCount().enqueue(new Callback<ApiResponse<Map<String, Long>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Long>>> call, Response<ApiResponse<Map<String, Long>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Map<String, Long> counts = response.body().getData();

                    setBadge(tvBadgePending, counts.get("pending"));
                    setBadge(tvBadgeShipping, counts.get("shipping"));
                    setBadge(tvBadgeDelivered, counts.get("delivered"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Long>>> call, Throwable t) {
                Toast.makeText(requireContext(), "Không thể tải trạng thái đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setBadge(TextView badge, Long count) {
        if (count != null && count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }
}
