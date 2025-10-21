package com.example.onlyfanshop.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.ProductDetailDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailManageActivity extends AppCompatActivity {
    private ImageView imgProduct;
    private TextView tvProductName, tvPrice, tvCategory, tvBrand, tvBriefDescription, tvFullDescription, tvTechSpecs;
    private Button btnBack, btnUpdate;
    private int productId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_detail_manage);
        imgProduct = findViewById(R.id.imgProduct);
        tvProductName = findViewById(R.id.tvProductName);
        tvPrice = findViewById(R.id.tvPrice);
        tvCategory = findViewById(R.id.tvCategory);
        tvBrand = findViewById(R.id.tvBrand);
        tvBriefDescription = findViewById(R.id.tvBriefDescription);
        tvFullDescription = findViewById(R.id.tvFullDescription);
        tvTechSpecs = findViewById(R.id.tvTechSpecs);
        btnBack = findViewById(R.id.btnBack);
        btnUpdate = findViewById(R.id.btnUpdate);

        // Lấy ID sản phẩm từ Intent
        productId = getIntent().getIntExtra("productId", -1);

        if (productId == -1) {
            Toast.makeText(this, "Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Gọi API để lấy thông tin sản phẩm
        fetchProductDetail(productId);

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút sửa sản phẩm
        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailManageActivity.this, UpdateProductActivity.class);
            intent.putExtra("productToEdit", productId);
            startActivity(intent);
        });
    }

    private void fetchProductDetail(int productId) {
        ProductApi productApi = ApiClient.getPublicClient().create(ProductApi.class);

        productApi.getProductDetail(productId).enqueue(new Callback<ApiResponse<ProductDetailDTO>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProductDetailDTO>> call, Response<ApiResponse<ProductDetailDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductDetailDTO product = response.body().getData();

                    // Hiển thị thông tin sản phẩm
                    tvProductName.setText(product.getProductName());
                    tvPrice.setText("₫" + product.getPrice());
                    tvCategory.setText("Danh mục: " + product.getCategory().getCategoryName());
                    tvBrand.setText("Thương hiệu: " + product.getBrand().getName());
                    tvBriefDescription.setText(product.getBriefDescription());
                    tvFullDescription.setText(product.getFullDescription());
                    tvTechSpecs.setText(product.getTechnicalSpecifications());

                    // Hiển thị ảnh sản phẩm
                    if (product.getImageURL() != null && !product.getImageURL().isEmpty()) {
                        Glide.with(ProductDetailManageActivity.this)
                                .load(product.getImageURL())
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(imgProduct);
                    }

                } else {
                    Toast.makeText(ProductDetailManageActivity.this, "Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProductDetailDTO>> call, Throwable t) {
                Toast.makeText(ProductDetailManageActivity.this, "Lỗi khi tải sản phẩm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


