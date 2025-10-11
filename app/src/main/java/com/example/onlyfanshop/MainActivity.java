package com.example.onlyfanshop;

import static com.example.onlyfanshop.ultils.BadgeUtils.updateCartBadge;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.widget.Toast;

import com.example.onlyfanshop.ui.cart.CartActivity;
import com.example.onlyfanshop.ultils.AppPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.api.UserApi;
import com.example.onlyfanshop.model.UserDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.product.ProductDetailActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private UserApi userApi;
    private TextView textView;
    private EditText editProductId;
    private Button btnViewProduct;
    private Button btnViewCart;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Button btnOpenDashboard;
    private Button btnOpenTestChat;

    // Chat test views
    private RecyclerView rvCustomer, rvAdmin;
    private EditText edtCustomer, edtAdmin;
    private Button btnSendCustomer, btnSendAdmin;
    private final java.util.List<com.example.onlyfanshop.model.chat.Message> msgsCustomer = new java.util.ArrayList<>();
    private final java.util.List<com.example.onlyfanshop.model.chat.Message> msgsAdmin = new java.util.ArrayList<>();
    private RecyclerView.Adapter<?> adapterCustomer, adapterAdmin;
    private com.google.firebase.database.DatabaseReference messagesRef;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        initializeGoogleSignIn();

        UserDTO user = (UserDTO) getIntent().getSerializableExtra("user");
        textView = findViewById(R.id.textView);
        if (user != null && user.getUsername() != null) {
            textView.setText("Welcome " + user.getUsername());
        } else {
            textView.setText("Welcome to OnlyFanshop!");
        }

        userApi = ApiClient.getPublicClient().create(UserApi.class);

        editProductId = findViewById(R.id.editProductId);
        btnViewProduct = findViewById(R.id.btnViewProduct);
        btnViewCart = findViewById(R.id.btnViewCart);
        btnLogout = findViewById(R.id.btnLogout);
        btnOpenDashboard = findViewById(R.id.btnOpenDashboard);
        btnOpenTestChat = findViewById(R.id.btnOpenTestChat);

        btnViewProduct.setOnClickListener(v -> openProductDetail());
        btnViewCart.setOnClickListener(v -> {
            if (user != null) {
                viewCart(user.getUsername());
            }
        });
        btnLogout.setOnClickListener(v -> logout());

        btnOpenDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.activity.DashboardActivity.class);
            if (user != null) intent.putExtra("user", user);
            startActivity(intent);
        });

        // Update button text based on user role
        updateChatButtonText(user);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        btnOpenTestChat.setOnClickListener(v -> {
            // Check user role and navigate accordingly
            if (user != null && user.getRole() != null) {
                String userRole = user.getRole();
                if ("CUSTOMER".equals(userRole)) {
                    // Customer: Go to ChatRoomActivity to chat with admin
                    Log.d("MainActivity", "Customer accessing chat with admin");
                    Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.chat.ChatRoomActivity.class);

                    // Create conversation ID between customer and admin
                    String customerId = FirebaseAuth.getInstance().getUid();
                    if (customerId == null) {
                        customerId = "customer_" + user.getUserID();
                    }
                    String adminId = "admin_uid"; // This should match your admin UID in strings.xml
                    String conversationId = customerId.compareTo(adminId) < 0 ?
                            customerId + "_" + adminId : adminId + "_" + customerId;

                    intent.putExtra("conversationId", conversationId);
                    intent.putExtra("customerName", "Admin");
                    Log.d("MainActivity", "Opening chat room with conversationId: " + conversationId);
                    startActivity(intent);
                } else if ("ADMIN".equals(userRole)) {
                    // Admin: Go to ChatListActivity to see all conversations
                    Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.chat.ChatListActivity.class);
                    startActivity(intent);
                } else {
                    // Default fallback
                    Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.chat.ChatListActivity.class);
                    startActivity(intent);
                }
            } else {
                // No user info, default to ChatListActivity
                Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.chat.ChatListActivity.class);
                startActivity(intent);
            }
        });

        int cartCount = AppPreferences.getCartCount(this);
        updateCartBadge(MainActivity.this,cartCount);

    }


    private void openProductDetail() {
        String productIdText = editProductId.getText().toString().trim();

        if (productIdText.isEmpty()) {
            Toast.makeText(this, "Please enter a Product ID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int productId = Integer.parseInt(productIdText);
            Log.d("MainActivity", "Opening product detail for ID: " + productId);

            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
            startActivity(intent);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    private void testApiConnection() {
        Log.d("MainActivity", "Testing API connection...");
        Log.d("MainActivity", "Base URL: " + ApiClient.getPublicClient().baseUrl());

        ProductApi api = ApiClient.getPublicClient().create(ProductApi.class);

        // First test basic connectivity
        Call<ApiResponse<String>> testCall = api.testConnection();
        testCall.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                Log.d("MainActivity", "Response code: " + response.code());
                Log.d("MainActivity", "Response body: " + response.body());
                Log.d("MainActivity", "Response headers: " + response.headers());

                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getData();
                    Toast.makeText(MainActivity.this, "API Connected: " + message, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, 1);
                    startActivity(intent);
                } else {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        errorBody = "Could not read error body";
                    }
                    Log.e("MainActivity", "Error response: " + errorBody);
                    Toast.makeText(MainActivity.this, "API Error: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e("MainActivity", "API connection failed: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Cannot connect to server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void viewCart(String username) {
        Log.d("MainActivity", "Testing API only...");
        Intent intent = new Intent(MainActivity.this, CartActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("username", username);
        intent.putExtras(bundle);
        startActivity(intent);

        //testRawHttp();
    }

    private void testRawHttp() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://10.0.2.2:8080/product/test");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                Log.d("MainActivity", "Raw HTTP Response Code: " + responseCode);

                String responseBody;
                if (responseCode >= 200 && responseCode < 300) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    responseBody = response.toString();
                } else {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    responseBody = response.toString();
                }

                Log.d("MainActivity", "Raw HTTP Response Body: " + responseBody);

                runOnUiThread(() -> {
                    if (responseCode >= 200 && responseCode < 300) {
                        Toast.makeText(MainActivity.this, "Raw HTTP Success: " + responseBody, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Raw HTTP Error " + responseCode + ": " + responseBody, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e("MainActivity", "Raw HTTP failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Raw HTTP Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("328191492825-8iket64hs1nr651gn0jnb19js7aimj10.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // Logout method
//    private void logout() {
//        Log.d("Logout", "Starting logout process");
//
//        // Sign out from Firebase
//        mAuth.signOut();
//        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
//        String token = prefs.getString("jwt_token", null);
//        // Sign out from Google
//        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
//            Log.d("Logout", "Google sign out completed");
//
//            // Show logout success message
//            Toast.makeText(MainActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
//            // Navigate back to LoginActivity
//            Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.login.LoginActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(intent);
//            finish();
//        });
//    }
    private void logout() {
        Log.d("Logout", "Starting logout process");

        // Lấy token từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);

        if (token == null) {
            Log.w("Logout", "Không có token để logout, chuyển về LoginActivity");
            navigateToLogin();
            return;
        }

        // Gọi API logout
        UserApi apiService = ApiClient.getPrivateClient(this).create(UserApi.class);
        Call<ApiResponse<Void>> call = apiService.logout();

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d("Logout", "Logout thành công từ server");
                } else {
                    Log.w("Logout", "Server trả về lỗi: " + response.code());
                }

                completeLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("Logout", "Lỗi khi gọi API logout", t);
                completeLogout();
            }
        });
    }

    private void completeLogout() {
        Log.d("Logout", "Hoàn tất đăng xuất cục bộ");

        // Xóa token khỏi SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().remove("jwt_token").apply();

        // Đăng xuất Firebase
        mAuth.signOut();

        // Đăng xuất Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d("Logout", "Google sign out completed");
            Toast.makeText(MainActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, com.example.onlyfanshop.ui.login.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateChatButtonText(UserDTO user) {
        if (user != null && user.getRole() != null) {
            String userRole = user.getRole();
            if ("CUSTOMER".equals(userRole)) {
                btnOpenTestChat.setText("Chat with Admin");
            } else if ("ADMIN".equals(userRole)) {
                btnOpenTestChat.setText("Chat List");
            } else {
                btnOpenTestChat.setText("Open Test Chat");
            }
        } else {
            btnOpenTestChat.setText("Open Test Chat");
        }
    }

}
