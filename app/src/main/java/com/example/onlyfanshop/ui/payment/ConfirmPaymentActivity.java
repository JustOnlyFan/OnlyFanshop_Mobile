package com.example.onlyfanshop.ui.payment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.CartAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.PaymentApi;
import com.example.onlyfanshop.api.ProfileApi;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.api.VietnamAddressApi;
import com.example.onlyfanshop.api.VietnamAddressApiClient;
import com.example.onlyfanshop.databinding.ActivityConfirmPaymentBinding;
import com.example.onlyfanshop.ui.order.OrderDetailsActivity;
import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.PaymentDTO;
import com.example.onlyfanshop.model.UserDTO;
import com.example.onlyfanshop.model.VietnamDistrict;
import com.example.onlyfanshop.model.VietnamProvince;
import com.example.onlyfanshop.model.VietnamWard;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.UserResponse;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmPaymentActivity extends AppCompatActivity {
    private static final String TAG = "ConfirmPayment";
    private ActivityConfirmPaymentBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItemDTO> cartItems;
    List<CartItemDTO> subList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private VietnamAddressApi vietnamAddressApi;
    private List<VietnamProvince> provinces;
    private List<VietnamDistrict> districts;
    private List<VietnamWard> wards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityConfirmPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get cart items and total price from intent
        cartItems = (List<CartItemDTO>) getIntent().getSerializableExtra("cartItems");
        for (CartItemDTO item : cartItems) {
            if(item.isChecked()){
                subList.add(item);
            }
        }
        double totalPrice = getIntent().getDoubleExtra("totalPrice", 0.0);
        
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        
        // Initialize Vietnam Address API
        vietnamAddressApi = VietnamAddressApiClient.getInstance().create(VietnamAddressApi.class);
        provinces = new ArrayList<>();
        districts = new ArrayList<>();
        wards = new ArrayList<>();
        
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // Setup edge-to-edge insets to prevent content from going under status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Setup UI
        setupUI(totalPrice);
        
        // Fetch user information
        fetchUserInfo();
        
        // Load provinces
        loadProvinces();
    }

    private void setupUI(double totalPrice) {
        // Setup cart recycler view with animation
        binding.rclViewCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, subList, false);
        binding.rclViewCart.setAdapter(cartAdapter);
        
        // Animate cart items appearance
        animateCartItems();
        
        // Format and display total price
        String totalPriceText = formatCurrencyVND(totalPrice);
        binding.totalPrice.setText(totalPriceText);
        binding.tvSubtotal.setText(totalPriceText);
        
        // Setup delivery type tabs
        setupDeliveryTabs();
        
        // Setup checkout button with dynamic text based on payment method
        binding.checkoutBtn.setOnClickListener(v -> {
            // Add click animation
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .setInterpolator(new OvershootInterpolator())
                                .start();
                        processPayment(totalPrice);
                    })
                    .start();
        });
        
        // Add listener to update button text based on payment method
        binding.radioBtnCOD.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.checkoutBtn.setText("Xác nhận đơn hàng");
                // Show COD description with animation
                binding.tvVNPayDescription.setVisibility(View.GONE);
                animateDescriptionShow(binding.tvCODDescription);
                
                // Add bounce animation to COD radio button
                animateRadioButtonSelection(binding.radioBtnCOD);
            } else {
                binding.tvCODDescription.setVisibility(View.GONE);
            }
        });
        
        binding.radioBtnVnPay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.checkoutBtn.setText("Thanh toán");
                // Show VNPay description with animation
                binding.tvCODDescription.setVisibility(View.GONE);
                animateDescriptionShow(binding.tvVNPayDescription);
                
                // Add bounce animation to VNPay radio button
                animateRadioButtonSelection(binding.radioBtnVnPay);
            } else {
                binding.tvVNPayDescription.setVisibility(View.GONE);
            }
        });
        
        // Setup "Enter new address" click listener
        binding.tvEnterNewAddress.setOnClickListener(v -> {
            if (binding.layoutNewAddress.getVisibility() == View.VISIBLE) {
                binding.layoutNewAddress.setVisibility(View.GONE);
                binding.tvEnterNewAddress.setText("Nhập địa chỉ mới");
            } else {
                binding.layoutNewAddress.setVisibility(View.VISIBLE);
                binding.tvEnterNewAddress.setText("Ẩn địa chỉ mới");
            }
        });
        
        // Load default address
        loadDefaultAddress();
    }
    
    private void setupDeliveryTabs() {
        // Add tabs programmatically
        TabLayout.Tab tabPickup = binding.tabDeliveryType.newTab();
        tabPickup.setText("Nhận tại cửa hàng");
        binding.tabDeliveryType.addTab(tabPickup);
        
        TabLayout.Tab tabHomeDelivery = binding.tabDeliveryType.newTab();
        tabHomeDelivery.setText("Giao hàng tận nơi");
        binding.tabDeliveryType.addTab(tabHomeDelivery);
        
        // Setup tab listener
        binding.tabDeliveryType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Nhận tại cửa hàng
                    showStorePickupLayout();
                } else if (position == 1) {
                    // Giao hàng tận nơi
                    showHomeDeliveryLayout();
                }
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
        
        // Chọn tab "Nhận tại cửa hàng" mặc định sau khi setup listener
        TabLayout.Tab defaultTab = binding.tabDeliveryType.getTabAt(0);
        if (defaultTab != null) {
            defaultTab.select();
        }
        
        // Đảm bảo hiển thị form "Nhận tại cửa hàng" ngay từ đầu
        binding.getRoot().post(() -> {
            showStorePickupLayout();
        });
    }
    
    private void showStorePickupLayout() {
        animateLayoutTransition(binding.layoutStorePickup, binding.layoutHomeDelivery);
    }
    
    private void showHomeDeliveryLayout() {
        animateLayoutTransition(binding.layoutHomeDelivery, binding.layoutStorePickup);
    }
    
    private void animateLayoutTransition(View showView, View hideView) {
        // Fade out and hide the current view
        if (hideView.getVisibility() == View.VISIBLE) {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(hideView, "alpha", 1f, 0f);
            fadeOut.setDuration(200);
            fadeOut.setInterpolator(new DecelerateInterpolator());
            fadeOut.start();
            fadeOut.addUpdateListener(animation -> {
                if ((float) animation.getAnimatedValue() == 0f) {
                    hideView.setVisibility(View.GONE);
                }
            });
        }
        
        // Fade in and show the new view
        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(showView, "alpha", 0f, 1f);
        fadeIn.setDuration(200);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.start();
    }
    
    private void hideBothDeliveryLayouts() {
        binding.layoutStorePickup.setVisibility(View.GONE);
        binding.layoutHomeDelivery.setVisibility(View.GONE);
    }
    
    private void loadDefaultAddress() {
        String defaultAddress = sharedPreferences.getString("address", "");
        if (!defaultAddress.isEmpty()) {
            binding.tvDefaultAddress.setText(defaultAddress);
            binding.layoutDefaultAddress.setVisibility(View.VISIBLE);
        } else {
            binding.layoutDefaultAddress.setVisibility(View.GONE);
        }
    }

    private void fetchUserInfo() {
        String token = ApiClient.getToken(this);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "No token found");
            loadUserInfoFromPreferences();
            return;
        }

        ProfileApi profileApi = ApiClient.getPrivateClient(this).create(ProfileApi.class);
        profileApi.getUser().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    com.example.onlyfanshop.model.User user = response.body().getData();
                    
                    // Save user info to SharedPreferences for fallback
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", user.getUsername());
                    editor.putString("email", user.getEmail());
                    if (user.getPhoneNumber() != null) {
                        editor.putString("phoneNumber", user.getPhoneNumber());
                    }
                    editor.apply();
                    
                    displayUserInfo(user);
                } else {
                    Log.w(TAG, "getUser failed: code=" + response.code());
                    loadUserInfoFromPreferences();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "getUser error", t);
                loadUserInfoFromPreferences();
            }
        });
    }

    private void loadUserInfoFromPreferences() {
        String username = sharedPreferences.getString("username", "");
        String email = sharedPreferences.getString("email", "");
        String phoneNumber = sharedPreferences.getString("phoneNumber", "");
        
        com.example.onlyfanshop.model.UserDTO user = new UserDTO();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        
        displayUserInfo(user);
    }

    private void displayUserInfo(Object userObj) {
        String username = null, email = null, phoneNumber = null;
        
        if (userObj instanceof com.example.onlyfanshop.model.User) {
            com.example.onlyfanshop.model.User user = (com.example.onlyfanshop.model.User) userObj;
            username = user.getUsername();
            email = user.getEmail();
            phoneNumber = user.getPhoneNumber();
        } else if (userObj instanceof UserDTO) {
            UserDTO user = (UserDTO) userObj;
            username = user.getUsername();
            email = user.getEmail();
            phoneNumber = user.getPhoneNumber();
        }
        
        if (username != null && !username.isEmpty()) {
            binding.tvUsername.setText(username);
            // Auto-fill recipient name field
            binding.edtRecipientName.setText(username);
        }
        
        if (email != null && !email.isEmpty()) {
            binding.tvEmail.setText(email);
        }
        
        // Show phone number if available
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            binding.layoutPhone.setVisibility(View.VISIBLE);
            binding.tvPhoneNumber.setText(phoneNumber);
            // Auto-fill recipient phone field
            binding.edtRecipientPhone.setText(phoneNumber);
        } else {
            binding.layoutPhone.setVisibility(View.GONE);
        }
    }


    private void processPayment(double totalPrice) {
        // Validate delivery type - check which tab is selected
        int selectedTab = binding.tabDeliveryType.getSelectedTabPosition();
        boolean isPickupStore = (selectedTab == 0);
        boolean isHomeDelivery = (selectedTab == 1);
        
        if (selectedTab == -1) {
            showError("Vui lòng chọn phương thức nhận hàng");
            return;
        }
        
        // Validate payment method
        boolean isCOD = binding.radioBtnCOD.isChecked();
        boolean isVNPay = binding.radioBtnVnPay.isChecked();
        
        if (!isCOD && !isVNPay) {
            showError("Vui lòng chọn phương thức thanh toán");
            return;
        }
        
        binding.tvError.setVisibility(View.GONE);
        
        // Build address string based on delivery type
        String deliveryAddress = buildDeliveryAddress(isPickupStore, isHomeDelivery);
        
        if (deliveryAddress == null) {
            showError("Vui lòng nhập đầy đủ thông tin địa chỉ");
            return;
        }
        
        if (isCOD) {
            showCODConfirmationDialog(totalPrice, deliveryAddress);
        } else {
            handleVNPayPayment(totalPrice, deliveryAddress);
        }
    }
    
    private void showCODConfirmationDialog(double totalPrice, String address) {
        // Create custom dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cod_confirmation, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        
        // Setup buttons
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            handleCODPayment(totalPrice, address);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.setOnShowListener(d -> {
            // Animate dialog appearance
            View v = dialog.getWindow().getDecorView();
            v.setAlpha(0f);
            v.setScaleX(0.9f);
            v.setScaleY(0.9f);
            v.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        });
        
        dialog.show();
    }
    
    private void showCODSuccessDialog(Integer orderId) {
        // Create custom success dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cod_success, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        
        // Setup order ID display
        android.widget.TextView tvOrderId = dialogView.findViewById(R.id.tvOrderId);
        tvOrderId.setText("#" + orderId);
        
        // Setup buttons
        android.widget.Button btnViewOrder = dialogView.findViewById(R.id.btnViewOrder);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btnClose);
        
        btnViewOrder.setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to order details with orderId
            Intent intent = new Intent(ConfirmPaymentActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
            finish(); // Close payment activity after navigating
        });
        
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        
        dialog.setOnShowListener(d -> {
            // Animate success dialog with bounce
            View v = dialog.getWindow().getDecorView();
            v.setAlpha(0f);
            v.setScaleX(0.5f);
            v.setScaleY(0.5f);
            v.animate()
                    .alpha(1f)
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(300)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(1.5f))
                            .start())
                    .setInterpolator(new OvershootInterpolator())
                    .start();
            
            // Animate success icon
            View iconView = dialogView.findViewById(R.id.ivSuccessIcon);
            if (iconView != null) {
                iconView.setScaleX(0f);
                iconView.setScaleY(0f);
                iconView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setStartDelay(200)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start();
            }
        });
        
        dialog.show();
    }
    
    private String buildDeliveryAddress(boolean isPickupStore, boolean isHomeDelivery) {
        if (isPickupStore) {
            // Store pickup address
            String province = binding.spinnerStoreProvince.getText().toString().trim();
            String district = binding.spinnerStoreDistrict.getText().toString().trim();
            String store = binding.spinnerStore.getText().toString().trim();
            
            if (province.isEmpty() || district.isEmpty() || store.isEmpty()) {
                return null;
            }
            
            return String.format("%s, %s, %s", store, district, province);
        } else if (isHomeDelivery) {
            // Home delivery address
            // Check if using default address or new address
            boolean isNewAddress = binding.layoutNewAddress.getVisibility() == View.VISIBLE;
            
            if (isNewAddress) {
                String province = binding.spinnerHomeProvince.getText().toString().trim();
                String district = binding.spinnerHomeDistrict.getText().toString().trim(); // This is actually ward in API v2
                String street = binding.edtHomeStreet.getText().toString().trim();
                
                if (province.isEmpty() || district.isEmpty() || street.isEmpty()) {
                    return null;
                }
                
                // Format: street, district (ward), province
                return String.format("%s, %s, %s", street, district, province);
            } else {
                // Use default address
                String defaultAddress = binding.tvDefaultAddress.getText().toString().trim();
                if (defaultAddress.isEmpty()) {
                    return null;
                }
                return defaultAddress;
            }
        }
        return null;
    }
    
    private void handleCODPayment(double totalPrice, String address) {
        showLoading(true);
        
        // Get recipient phone number from input
        String recipientPhoneNumber = "";
        if (binding.edtRecipientPhone != null) {
            recipientPhoneNumber = binding.edtRecipientPhone.getText().toString().trim();
        }
        // If empty, use default phone number from user
        if (recipientPhoneNumber.isEmpty()) {
            recipientPhoneNumber = binding.tvPhoneNumber.getText().toString().trim();
        }
        
        // Determine delivery type and storeId
        int selectedTab = binding.tabDeliveryType.getSelectedTabPosition();
        String deliveryType = (selectedTab == 0) ? "IN_STORE_PICKUP" : "HOME_DELIVERY";
        Integer storeId = null;
        
        // For now, buyMethod defaults to "ByCart" since it's not passed in intent
        // This should be updated to pass buyMethod from CartFragment/BuyNowBottomSheet
        String buyMethod = "ByCart";
        
        PaymentApi api = ApiClient.getPrivateClient(this).create(PaymentApi.class);
        api.createCODOrder(totalPrice, address, buyMethod, recipientPhoneNumber, deliveryType, storeId)
                .enqueue(new Callback<ApiResponse<Integer>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Integer orderId = response.body().getData();
                            Log.d(TAG, "COD Order created: " + orderId);
                            
                            // Show success dialog
                            showCODSuccessDialog(orderId);
                        } else {
                            String errorMsg = "Không thể tạo đơn hàng COD. Vui lòng thử lại.";
                            if (response.errorBody() != null) {
                                try {
                                    Log.e(TAG, "COD Order error: " + response.errorBody().string());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error reading error body", e);
                                }
                            }
                            showError(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "Network error: " + t.getMessage(), t);
                        showError("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }
    
    private void handleVNPayPayment(double totalPrice, String address) {
        showLoading(true);
        
        // Get recipient phone number from input
        String recipientPhoneNumber = "";
        if (binding.edtRecipientPhone != null) {
            recipientPhoneNumber = binding.edtRecipientPhone.getText().toString().trim();
        }
        // If empty, use default phone number from user
        if (recipientPhoneNumber.isEmpty()) {
            recipientPhoneNumber = binding.tvPhoneNumber.getText().toString().trim();
        }
        
        String bankCode = "NCB";
        PaymentApi api = ApiClient.getPrivateClient(this).create(PaymentApi.class);
        api.createPayment(totalPrice, bankCode, address, recipientPhoneNumber).enqueue(new Callback<ApiResponse<PaymentDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentDTO>> call, Response<ApiResponse<PaymentDTO>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String paymentUrl = response.body().getData().getPaymentUrl();
                    Log.d(TAG, "Payment URL: " + paymentUrl);
                    Toast.makeText(ConfirmPaymentActivity.this, "Đang chuyển hướng đến trang thanh toán...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ConfirmPaymentActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra(PaymentWebViewActivity.EXTRA_URL, paymentUrl);
                    startActivity(intent);
                } else {
                    String errorMsg = "Không thể tạo thanh toán. Vui lòng thử lại.";
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Payment error: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentDTO>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            // Animate loading in
            binding.progressBar.setAlpha(0f);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            
            // Animate button out
            binding.checkoutBtn.animate()
                    .alpha(0.5f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(150)
                    .start();
        } else {
            // Animate loading out
            binding.progressBar.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    })
                    .start();
            
            // Animate button back in
            binding.checkoutBtn.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
        binding.checkoutBtn.setEnabled(!show);
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        
        // Animate error message
        if (binding.tvError.getVisibility() != View.VISIBLE) {
            binding.tvError.setAlpha(0f);
            binding.tvError.setTranslationY(-20f);
            binding.tvError.setVisibility(View.VISIBLE);
            
            binding.tvError.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        } else {
            // Shake animation for repeated errors
            binding.tvError.animate()
                    .translationX(-10f)
                    .setDuration(50)
                    .withEndAction(() -> binding.tvError.animate()
                            .translationX(10f)
                            .setDuration(50)
                            .withEndAction(() -> binding.tvError.animate()
                                    .translationX(-10f)
                                    .setDuration(50)
                                    .withEndAction(() -> binding.tvError.animate()
                                            .translationX(0f)
                                            .setDuration(50)
                                            .start())
                                    .start())
                            .start())
                    .start();
        }
    }

    private String formatCurrencyVND(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value).replace("₫", "₫");
    }
    
    private void animateDescriptionShow(View descriptionView) {
        descriptionView.setAlpha(0f);
        descriptionView.setTranslationY(-10f);
        descriptionView.setVisibility(View.VISIBLE);
        
        descriptionView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    
    private void animateRadioButtonSelection(View radioButton) {
        radioButton.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction(() -> radioButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator())
                        .start())
                .start();
    }
    
    private void animateCartItems() {
        // Animate cart items appearance with stagger
        for (int i = 0; i < binding.rclViewCart.getChildCount(); i++) {
            View child = binding.rclViewCart.getChildAt(i);
            if (child != null) {
                child.setAlpha(0f);
                child.setTranslationY(30f);
                
                child.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(i * 80)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }
    
    private void loadProvinces() {
        vietnamAddressApi.getProvinces().enqueue(new Callback<List<VietnamProvince>>() {
            @Override
            public void onResponse(Call<List<VietnamProvince>> call, Response<List<VietnamProvince>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinces = response.body();
                    setupProvinceDropdowns();
                    Log.d(TAG, "Loaded " + provinces.size() + " provinces");
                } else {
                    Log.e(TAG, "Failed to load provinces: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<List<VietnamProvince>> call, Throwable t) {
                Log.e(TAG, "Error loading provinces: " + t.getMessage(), t);
            }
        });
    }
    
    private void setupProvinceDropdowns() {
        // Get province names for dropdowns
        ArrayList<String> provinceNames = new ArrayList<>();
        for (VietnamProvince province : provinces) {
            provinceNames.add(province.getName());
        }
        
        // Setup adapters for all province dropdowns
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, provinceNames);
        
        // Store pickup province
        binding.spinnerStoreProvince.setAdapter(provinceAdapter);
        binding.spinnerStoreProvince.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvinceName = provinceNames.get(position);
            VietnamProvince selectedProvince = provinces.get(position);
            Log.d(TAG, "Selected province: " + selectedProvinceName + ", code: " + selectedProvince.getCode());
            loadDistricts(selectedProvince.getCode(), true); // true for store pickup
        });
        
        // Home delivery province
        binding.spinnerHomeProvince.setAdapter(provinceAdapter);
        binding.spinnerHomeProvince.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvinceName = provinceNames.get(position);
            VietnamProvince selectedProvince = provinces.get(position);
            Log.d(TAG, "Selected province: " + selectedProvinceName + ", code: " + selectedProvince.getCode());
            loadDistricts(selectedProvince.getCode(), false); // false for home delivery
        });
    }
    
    private void loadDistricts(int provinceCode, boolean isStorePickup) {
        // API v2: Load wards directly from province (no districts)
        vietnamAddressApi.getProvinceWithWards(provinceCode).enqueue(new Callback<VietnamProvince>() {
            @Override
            public void onResponse(Call<VietnamProvince> call, Response<VietnamProvince> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VietnamProvince province = response.body();
                    // API v2: provinces return wards directly, no districts
                    wards = province.getWards();
                    // Load wards into district dropdowns (for backward compatibility)
                    loadWards(isStorePickup);
                    Log.d(TAG, "Loaded " + (wards != null ? wards.size() : 0) + " wards for province: " + provinceCode);
                } else {
                    Log.e(TAG, "Failed to load province data: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<VietnamProvince> call, Throwable t) {
                Log.e(TAG, "Error loading province data: " + t.getMessage(), t);
            }
        });
    }
    
    private void loadWards(boolean isStorePickup) {
        // API v2: Wards already loaded from province, setup dropdowns
        ArrayList<String> wardNames = new ArrayList<>();
        if (wards != null) {
            for (VietnamWard ward : wards) {
                wardNames.add(ward.getName());
            }
        }
        
        // Setup adapter
        ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wardNames);
        
        if (isStorePickup) {
            // Store pickup: wards go to district dropdown
            binding.spinnerStoreDistrict.setAdapter(wardAdapter);
            binding.spinnerStoreDistrict.setOnItemClickListener((parent, view, position, id) -> {
                String selectedWardName = wardNames.get(position);
                VietnamWard selectedWard = wards.get(position);
                Log.d(TAG, "Selected ward: " + selectedWardName + ", code: " + selectedWard.getCode());
            });
        } else {
            // Home delivery: wards go to district dropdown (API v2 uses wards directly, no separate district/ward)
            binding.spinnerHomeDistrict.setAdapter(wardAdapter);
            binding.spinnerHomeDistrict.setOnItemClickListener((parent, view, position, id) -> {
                String selectedWardName = wardNames.get(position);
                VietnamWard selectedWard = wards.get(position);
                Log.d(TAG, "Selected ward: " + selectedWardName + ", code: " + selectedWard.getCode());
            });
        }
    }
}
