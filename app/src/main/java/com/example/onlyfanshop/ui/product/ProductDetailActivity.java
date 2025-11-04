package com.example.onlyfanshop.ui.product;

import com.example.onlyfanshop.model.Request.AddToCartRequest;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bumptech.glide.Glide;
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
import com.example.onlyfanshop.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "product_id";

    private ImageView imageProduct, btnChat, btnCart, btnExpandDescription;
    private TextView textBrand, textProductName, textBottomPrice, textBrief, textFull, numberItem, addQuantity, minusQuantity, textRating;
    private TextView textSpecType, textSpecBlade, textSpecAirflow, textSpecSpeed, textSpecWeight, textSpecColor;
    private MaterialButton btnBuyNow;
    private boolean isDescriptionExpanded = false;
    //private Integer quantity = 1;
    private ProgressBar progressBar;
    private String imageURL;
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
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        imageProduct = findViewById(R.id.imageProduct);
        textBrand = findViewById(R.id.textBrand);
        textProductName = findViewById(R.id.textProductName);
        textBottomPrice = findViewById(R.id.textBottomPrice);
        textBrief = findViewById(R.id.textBrief);
        textFull = findViewById(R.id.textFull);
//        numberItem = findViewById(R.id.numberItem);
//        addQuantity = findViewById(R.id.addQuantity);
//        minusQuantity = findViewById(R.id.minusQuantity);
        btnChat = findViewById(R.id.btnChat);
        btnCart = findViewById(R.id.btnCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnExpandDescription = findViewById(R.id.btnExpandDescription);
        textRating = findViewById(R.id.textRating);
        textSpecType = findViewById(R.id.textSpecType);
        textSpecBlade = findViewById(R.id.textSpecBlade);
        textSpecAirflow = findViewById(R.id.textSpecAirflow);
        textSpecSpeed = findViewById(R.id.textSpecSpeed);
        textSpecWeight = findViewById(R.id.textSpecWeight);
        textSpecColor = findViewById(R.id.textSpecColor);
        progressBar = findViewById(R.id.progressBar);

        //numberItem.setText(quantity.toString());




        int id = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
        btnCart.setOnClickListener(v -> addTocart(id, 1));
        btnChat.setOnClickListener(v -> {
            // TODO: Implement chat functionality
            Toast.makeText(this, "Chat feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnExpandDescription.setOnClickListener(v -> toggleDescription());

        btnBuyNow.setOnClickListener(v -> {
            String name = textProductName.getText().toString();
            String price = textBottomPrice.getText().toString();
            ImageView imageView = imageProduct;
            imageView.buildDrawingCache();
            // 👉 Nếu bạn dùng ảnh từ Glide, tốt nhất truyền URL, còn không thì có thể dùng resource.

            BuyNowBottomSheet bottomSheet = BuyNowBottomSheet.newInstance(name, price, imageURL,id);

            bottomSheet.show(getSupportFragmentManager(), "BuyNowBottomSheet");
        });

        if (id > 0) {
            fetchDetail(id);
        } else {
            Toast.makeText(this, "Product ID không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void showBuyNowBottomSheet(int id) {
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_buy_now, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        bottomSheetDialog.setContentView(view);
        //Objects.requireNonNull(bottomSheetDialog.getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        AtomicInteger quantity = new AtomicInteger(1);
        ImageView imgProductThumb = view.findViewById(R.id.imgProductThumb);
        TextView tvProductNameBottom = view.findViewById(R.id.tvProductNameBottom);
        TextView tvProductPriceBottom = view.findViewById(R.id.tvProductPriceBottom);
        TextView tvQuantity = view.findViewById(R.id.tvQuantity);
        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        MaterialButton btnMinus = view.findViewById(R.id.btnMinus);
        Button btnConfirmBuy = view.findViewById(R.id.btnConfirmBuy);

        imgProductThumb.setImageDrawable(imageProduct.getDrawable());
        tvProductNameBottom.setText(textProductName.getText());
        tvProductPriceBottom.setText(textBottomPrice.getText());
        tvQuantity.setText(String.valueOf(quantity.get()));
        btnAdd.setOnClickListener(v -> {
            quantity.incrementAndGet();
            tvQuantity.setText(String.valueOf(quantity.get()));
        });
        btnMinus.setOnClickListener(v -> {
            if (quantity.get() > 1) {
                quantity.decrementAndGet();
                tvQuantity.setText(String.valueOf(quantity.get()));}
        });

        bottomSheetDialog.show();
    }


    private void addTocart(int productID, int quantiy) {
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
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    })
                    .setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white); // Nền trắng
            dialog.show();
            return;
        }
        AddToCartRequest request = new AddToCartRequest(productID, quantiy, username);
        CartItemApi cartItemApi = ApiClient.getPrivateClient(this).create(CartItemApi.class);
        cartItemApi.addToCart(request).enqueue(new Callback<ApiResponse<Void>>() {

            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ProductDetailActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();

                    AppEvents.get().notifyCartUpdated();

                    animateCartAdd();
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
        textBottomPrice.setText(formatCurrencyVND(product.getPrice() != null ? product.getPrice() : 0));
        textBrief.setText(product.getBriefDescription() != null ? product.getBriefDescription() : "");
        textFull.setText(product.getFullDescription() != null ? product.getFullDescription() : "");

        // Set technical specifications
        if (product.getTechnicalSpecifications() != null && !product.getTechnicalSpecifications().isEmpty()) {
            // Parse technical specifications and set individual fields
            String specs = product.getTechnicalSpecifications();
            // You can parse the specs string and set individual TextViews
            // For now, we'll set default values
            textSpecType.setText("Loại: " + (product.getProductName() != null ? product.getProductName() : "N/A"));
            textSpecBlade.setText("Sải cánh: 40 cm");
            textSpecAirflow.setText("Lưu lượng gió: 50.2 m³/phút");
            textSpecSpeed.setText("Tốc độ xoay: 1200 vòng/phút");
            textSpecWeight.setText("Trọng lượng: 4.6 kg");
            textSpecColor.setText("Màu sắc: Xanh, lá mạ");
        }

        // Set rating text (you can get this from product data if available)
        textRating.setText("4.5 (128 reviews)");

        imageURL = product.getImageURL();
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



    private void toggleDescription() {
        isDescriptionExpanded = !isDescriptionExpanded;
        textFull.setVisibility(isDescriptionExpanded ? View.VISIBLE : View.GONE);
        btnExpandDescription.setRotation(isDescriptionExpanded ? 180 : 0);
    }

    private void animateCartAdd() {
        CoordinatorLayout rootLayout = findViewById(R.id.coordinatorLayout);
        int size = 140; // Hình to hơn

        final ImageView flyingImage = new ImageView(this);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(size, size);
        flyingImage.setLayoutParams(params);
        flyingImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this)
                .load(imageURL)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(flyingImage);

        rootLayout.addView(flyingImage);

        int[] startLocation = new int[2];
        btnCart.getLocationInWindow(startLocation);

        int[] endLocation = new int[2];
        ImageView btnCartDetail = findViewById(R.id.btnCartDetail);
        btnCartDetail.getLocationInWindow(endLocation);

        flyingImage.setTranslationX(startLocation[0]);
        flyingImage.setTranslationY(startLocation[1]);

        // Tính vị trí trung tâm icon đích
        int iconWidth = btnCartDetail.getWidth();
        int iconHeight = btnCartDetail.getHeight();
        int imageWidth = params.width;
        int imageHeight = params.height;
        float targetX = endLocation[0] + iconWidth / 2f - imageWidth / 2f;
        float targetY = endLocation[1] + iconHeight / 2f - imageHeight / 2f - 12; // -12 để nhích lên xíu

        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(flyingImage, "rotation", 0f, 720f);

        ObjectAnimator translateX = ObjectAnimator.ofFloat(flyingImage, "translationX", startLocation[0], targetX);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(flyingImage, "translationY", startLocation[1], targetY);

        AnimatorSet moveSet = new AnimatorSet();
        moveSet.playTogether(rotateAnim, translateX, translateY);
        moveSet.setDuration(1200);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(flyingImage, "scaleX", 1f, 0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(flyingImage, "scaleY", 1f, 0f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(flyingImage, "alpha", 1f, 0f);

        AnimatorSet shrinkSet = new AnimatorSet();
        shrinkSet.playTogether(scaleX, scaleY, alphaAnim);
        shrinkSet.setDuration(400);

        AnimatorSet totalSet = new AnimatorSet();
        totalSet.playSequentially(moveSet, shrinkSet);
        totalSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rootLayout.removeView(flyingImage);
            }
        });
        totalSet.start();
    }
    
    private String formatCurrencyVND(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value).replace("₫", "₫");
    }
}