package com.example.onlyfanshop.ui.product;

import com.example.onlyfanshop.utils.BadgeUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.MainActivity;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CartItemApi;
import com.example.onlyfanshop.api.PaymentApi;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.utils.AppEvents; // THÊM: import AppEvents
import com.example.onlyfanshop.model.PaymentDTO;
import com.example.onlyfanshop.model.ProductDetailDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.ui.payment.PaymentWebViewActivity;
import com.example.onlyfanshop.utils.AppPreferences;
import com.example.onlyfanshop.ultils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "product_id";

    private ImageView imageProduct;
    private TextView textBrand, textProductName, textBottomPrice, textBrief, textFull, textSpecs;
    private MaterialButton btnAddToCart;
    private ProgressBar progressBar;
    private boolean isFavorite = false;

    // THÊM: Factory method tạo Intent mở màn chi tiết
    public static Intent newIntent(Context context, int productId) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        imageProduct = findViewById(R.id.imageProduct);
        textBrand = findViewById(R.id.textBrand);
        textProductName = findViewById(R.id.textProductName);
        textBottomPrice = findViewById(R.id.textBottomPrice);
        textBrief = findViewById(R.id.textBrief);
        textFull = findViewById(R.id.textFull);
        textSpecs = findViewById(R.id.textSpecs);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnFavorite).setOnClickListener(v -> toggleFavorite());

        int id = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
        btnAddToCart.setOnClickListener(v -> addTocart(id));
        if (id > 0) {
            fetchDetail(id);
        } else {
            Toast.makeText(this, "Product ID không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void testPayment() {
        String priceString = textBottomPrice.getText().toString().replace("$", "");
        double amount;
        try {
            amount = Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid product price", Toast.LENGTH_SHORT).show();
            return;
        }

        String bankCode = "NCB";
        Log.d("Payment", "Creating payment with amount: " + amount + " and bankCode: " + bankCode);
        showLoading(true);

        PaymentApi api = ApiClient.getPrivateClient(this).create(PaymentApi.class);
        api.createPayment(amount, bankCode).enqueue(new Callback<ApiResponse<PaymentDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentDTO>> call, Response<ApiResponse<PaymentDTO>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String paymentUrl = response.body().getData().getPaymentUrl();
                    Log.d("Payment", "Payment URL: " + paymentUrl);
                    Toast.makeText(ProductDetailActivity.this, "Redirecting to payment...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProductDetailActivity.this, PaymentWebViewActivity.class);
                    intent.putExtra(PaymentWebViewActivity.EXTRA_URL, paymentUrl);
                    startActivity(intent);
                } else {
                    Log.e("Payment", "API call failed with response code: " + response.code());
                    Toast.makeText(ProductDetailActivity.this, "Failed to create payment.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentDTO>> call, Throwable t) {
                showLoading(false);
                Log.e("Payment", "Network error: " + t.getMessage(), t);
                Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTocart(int productID) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        String token = sharedPreferences.getString("jwt_token", "");

        if (username == null || username.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            // Tạo dialog giống PleaseSignInFragment, nền trắng
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                    .setTitle("Please sign in")
                    .setMessage("You need to sign in to continue.")
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        Intent intent = new Intent(this, com.example.onlyfanshop.ui.login.LoginActivity.class);
                        startActivity(intent);
                        finish(); // Nếu muốn đóng màn hình hiện tại sau khi sang LoginActivity
                    })
                    .setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white); // Nền trắng
            dialog.show();
            return;
        }

        CartItemApi cartItemApi = ApiClient.getPrivateClient(this).create(CartItemApi.class);
        cartItemApi.addToCart(productID, username).enqueue(new Callback<ApiResponse<Void>>() {

            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int currentCount = AppPreferences.getCartCount(ProductDetailActivity.this);
                    AppPreferences.setCartCount(ProductDetailActivity.this, currentCount + 1);

                    int cartCount = AppPreferences.getCartCount(ProductDetailActivity.this);
                    NotificationHelper.showNotification(
                            ProductDetailActivity.this,
                            "Giỏ hàng",
                            "Bạn đang có " + cartCount + " sản phẩm trong giỏ hàng!"
                    );

                    Toast.makeText(ProductDetailActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();

                    AppEvents.get().notifyCartUpdated();
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Thêm thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDetail(int id) {
        Log.d("ProductDetail", "Fetching product detail for ID: " + id);
        showLoading(true);
        ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
        api.getProductDetail(id).enqueue(new Callback<ApiResponse<ProductDetailDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProductDetailDTO>> call, Response<ApiResponse<ProductDetailDTO>> response) {
                showLoading(false);
                Log.d("ProductDetail", "Response code: " + response.code());
                Log.d("ProductDetail", "Response body: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    ProductDetailDTO d = response.body().getData();
                    Log.d("ProductDetail", "Product data: " + d);
                    if (d == null) {
                        Log.e("ProductDetail", "Product data is null");
                        Toast.makeText(ProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bindProductData(d);
                } else {
                    Log.e("ProductDetail", "Response not successful or body is null");
                    Toast.makeText(ProductDetailActivity.this, "Failed to load product details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProductDetailDTO>> call, Throwable t) {
                showLoading(false);
                Log.e("ProductDetail", "Network error: " + t.getMessage(), t);
                Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProductData(ProductDetailDTO product) {
        textBrand.setText(product.getBrand() != null ? product.getBrand().getName() : "");
        textProductName.setText(product.getProductName());
        textBottomPrice.setText(String.format("$%.2f", product.getPrice() != null ? product.getPrice() : 0));
        textBrief.setText(product.getBriefDescription() != null ? product.getBriefDescription() : "");
        textFull.setText(product.getFullDescription() != null ? product.getFullDescription() : "");
        textSpecs.setText(product.getTechnicalSpecifications() != null ? product.getTechnicalSpecifications() : "");

        if (product.getImageURL() != null && !product.getImageURL().isEmpty()) {
            Glide.with(ProductDetailActivity.this)
                    .load(product.getImageURL())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageProduct);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        ImageView fav = findViewById(R.id.btnFavorite);
        fav.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        // TODO: Optionally call backend to persist favorite state
    }
}