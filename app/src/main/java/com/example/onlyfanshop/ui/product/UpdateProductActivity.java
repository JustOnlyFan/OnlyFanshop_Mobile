package com.example.onlyfanshop.ui.product;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.ProductDetailDTO;
import com.example.onlyfanshop.model.Request.ProductRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProductActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;

    private EditText edtName, edtBrief, edtFull, edtSpecs, edtPrice;
    private Spinner spinnerCategory, spinnerBrand;
    private ImageView imgPreview;
    private Button btnChooseImage, btnUpdate, btnBack;

    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private List<CategoryDTO> categoryList;
    private List<BrandDTO> brandList;
    private boolean isBrandLoaded = false;
    private boolean isCategoryLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_product);
        edtName = findViewById(R.id.edtProductName);
        edtBrief = findViewById(R.id.edtBriefDescription);
        edtFull = findViewById(R.id.edtFullDescription);
        edtSpecs = findViewById(R.id.edtTechSpecs);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        imgPreview = findViewById(R.id.imgPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpdate = findViewById(R.id.btnSubmitProduct);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        loadCategories();
        loadBrands();
        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("productToEdit")) {
            int productId = intent.getIntExtra("productToEdit",0);
            fetchProduct(productId);
        }
        btnChooseImage.setOnClickListener(v -> updateImage());
        btnUpdate.setOnClickListener(v -> updateProduct());
    }
    private void fetchProduct(int productId) {
        ProductApi productApi = ApiClient.getPrivateClient(this).create(ProductApi.class);

        productApi.getProductDetail(productId).enqueue(new Callback<ApiResponse<ProductDetailDTO>>() {

            @Override
            public void onResponse(Call<ApiResponse<ProductDetailDTO>> call, Response<ApiResponse<ProductDetailDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductDetailDTO product = response.body().getData();

                    // Assign data to interface
                    edtName.setText(product.getProductName());
                    edtBrief.setText(product.getBriefDescription());
                    edtFull.setText(product.getFullDescription());
                    edtSpecs.setText(product.getTechnicalSpecifications());
                    edtPrice.setText(String.valueOf(product.getPrice()));

                    // Nếu có ảnh
                    if (product.getImageURL() != null && !product.getImageURL().isEmpty()) {
                        Glide.with(UpdateProductActivity.this)
                                .load(product.getImageURL())
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(imgPreview);

                        uploadedImageUrl = product.getImageURL();
                    }
                    selectSpinnerWhenReady(product);

                } else {
                    Toast.makeText(UpdateProductActivity.this, "Product not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProductDetailDTO>> call, Throwable t) {
                Toast.makeText(UpdateProductActivity.this, "Error loading product: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateProduct() {
        int productId = getIntent().getIntExtra("productToEdit", 0);

        ProductRequest request = new ProductRequest();
        request.setProductName(edtName.getText().toString());
        request.setBriefDescription(edtBrief.getText().toString());
        request.setFullDescription(edtFull.getText().toString());
        request.setTechnicalSpecifications(edtSpecs.getText().toString());
        request.setPrice(Double.parseDouble(edtPrice.getText().toString()));
//        request.setImageUrl(uploadedImageUrl);

        int selectedCategoryPos = spinnerCategory.getSelectedItemPosition();
        int selectedBrandPos = spinnerBrand.getSelectedItemPosition();

        if (selectedCategoryPos >= 0 && selectedCategoryPos < categoryList.size()) {
            request.setCategoryID(categoryList.get(selectedCategoryPos).getCategoryID());
        }

        if (selectedBrandPos >= 0 && selectedBrandPos < brandList.size()) {
            request.setBrandID(brandList.get(selectedBrandPos).getBrandID());
        }


        ProductApi productApi = ApiClient.getPrivateClient(this).create(ProductApi.class);

        productApi.updateProduct(productId, request).enqueue(new Callback<ProductDetailDTO>() {
            @Override
            public void onResponse(Call<ProductDetailDTO> call, Response<ProductDetailDTO> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UpdateProductActivity.this, "Update successful!", Toast.LENGTH_SHORT).show();
                    // When update successful (image or information)
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updatedProduct", true);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Return to previous screen
                } else {
                    Toast.makeText(UpdateProductActivity.this, "Update failed! Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductDetailDTO> call, Throwable t) {
                    Toast.makeText(UpdateProductActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadBrands() {
        ProductApi api = ApiClient.getPublicClient().create(ProductApi.class);
        api.getAllBrands().enqueue(new Callback<List<BrandDTO>>() {
            @Override
            public void onResponse(Call<List<BrandDTO>> call, Response<List<BrandDTO>> response) {
                if (response.isSuccessful()) {
                    brandList = response.body();
                    List<String> names = new ArrayList<>();
                    for (BrandDTO b : brandList) {
                        names.add(b.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(UpdateProductActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBrand.setAdapter(adapter);
                    Log.d("loadBrand", "da load brand");
                    isBrandLoaded = true;
                }
            }

            @Override
            public void onFailure(Call<List<BrandDTO>> call, Throwable t) {
                Toast.makeText(UpdateProductActivity.this, "Could not load brands!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategories() {
        ProductApi api = ApiClient.getPublicClient().create(ProductApi.class);
        api.getAllCategories().enqueue(new Callback<List<CategoryDTO>>() {
            @Override
            public void onResponse(Call<List<CategoryDTO>> call, Response<List<CategoryDTO>> response) {
                if (response.isSuccessful()) {
                    categoryList = response.body();
                    List<String> names = new ArrayList<>();
                    for (CategoryDTO c : categoryList) {
                        names.add(c.getCategoryName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(UpdateProductActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                    isCategoryLoaded = true;
                }
            }

            @Override
            public void onFailure(Call<List<CategoryDTO>> call, Throwable t) {
                Toast.makeText(UpdateProductActivity.this, "Could not load categories!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void selectSpinnerWhenReady(ProductDetailDTO product) {
        new Thread(() -> {
            // Wait until brand/category list is loaded
            while (!isBrandLoaded || !isCategoryLoaded) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            runOnUiThread(() -> {
                if (brandList != null) {
                    for (int i = 0; i < brandList.size(); i++) {
                        if (brandList.get(i).getName().equalsIgnoreCase(product.getBrand().getName())) {
                            spinnerBrand.setSelection(i);
                            break;
                        }
                    }
                }

                if (categoryList != null) {
                    for (int i = 0; i < categoryList.size(); i++) {
                        if (categoryList.get(i).getCategoryName().equalsIgnoreCase(product.getCategory().getCategoryName())) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }
    public void updateImage() {
        // Open gallery to select image
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgPreview.setImageURI(selectedImageUri); // Display new image selected by user

            // After selecting image, call upload function
            uploadNewImage();
        }
    }

    private void uploadNewImage() {
        int productId = getIntent().getIntExtra("productToEdit", 0);
        String oldUrl = uploadedImageUrl; // Old image saved when fetchProduct()

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a new image", Toast.LENGTH_SHORT).show();
            return;
        }

        File file;
        try {
            file = getFileFromUri(selectedImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Could not get image path: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody oldUrlPart = RequestBody.create(MediaType.parse("text/plain"), oldUrl);

        ProductApi apiService = ApiClient.getPrivateClient(this).create(ProductApi.class);

        // 🧩 1. Call Firebase image change API
        apiService.changeImage(filePart, oldUrlPart).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newImageUrl = response.body().getData();

                    // 🧩 2. Update image URL in database
                    apiService.updateImage(productId, newImageUrl).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response2) {
                            if (response2.isSuccessful()) {
                                uploadedImageUrl = newImageUrl;
                                // When update successful (image or information)
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("updatedProduct", true);
                                setResult(RESULT_OK, resultIntent);
                                Toast.makeText(UpdateProductActivity.this, "Image has been updated!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UpdateProductActivity.this, "Error updating image in database", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(UpdateProductActivity.this, "Database error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(UpdateProductActivity.this, "Error uploading new image", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(UpdateProductActivity.this, "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private File getFileFromUri(Uri uri) throws Exception {
        File file = new File(getCacheDir(), "upload_temp.jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        }
        return file;
    }
}

