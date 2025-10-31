package com.example.onlyfanshop.ui.product;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.Request.ProductRequest;
import com.example.onlyfanshop.model.response.ApiResponse;

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

public class AddProductActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    private EditText edtName, edtBrief, edtFull, edtSpecs, edtPrice;
    private Spinner spinnerCategory, spinnerBrand;
    private ImageView imgPreview;
    private Button btnChooseImage, btnSubmit;

    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private List<CategoryDTO> categoryList;
    private List<BrandDTO> brandList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Map view
        edtName = findViewById(R.id.edtProductName);
        edtBrief = findViewById(R.id.edtBriefDescription);
        edtFull = findViewById(R.id.edtFullDescription);
        edtSpecs = findViewById(R.id.edtTechSpecs);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        imgPreview = findViewById(R.id.imgPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSubmit = findViewById(R.id.btnSubmitProduct);

        // Load categories & brands
        loadCategories();
        loadBrands();

        // Select image
        btnChooseImage.setOnClickListener(v -> chooseImage());

        // Add product button (upload image + send data)
        btnSubmit.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select product image!", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadImageAndAddProduct();
        });

    }

    // ==================== Load Category & Brand ====================
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<CategoryDTO>> call, Throwable t) {
                Toast.makeText(AddProductActivity.this, "Could not load categories!", Toast.LENGTH_SHORT).show();
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this,
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBrand.setAdapter(adapter);
                    Log.d("loadBrand", "da load brand");
                }
            }

            @Override
            public void onFailure(Call<List<BrandDTO>> call, Throwable t) {
                Toast.makeText(AddProductActivity.this, "Could not load brands!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== Select image from gallery ====================
    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgPreview.setImageURI(selectedImageUri);
        }
    }

    // ==================== Upload image + Add product ====================
    private void uploadImageAndAddProduct() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = getFileFromUri(selectedImageUri);  // Copy Uri to temp file
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            Log.d("AddProduct", "File path: " + file.getAbsolutePath() + ", size: " + file.length());
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
            ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
            api.uploadImageToFirebase(body).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                    Log.d("Add product", "upload image" + response.body().getData());
                    Log.d("Add product", "upload image" + response.isSuccessful());
                    Log.d("Add product", "upload image" + response.code());
                    Log.d("Add product", "upload image" + response.message());
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedImageUrl = response.body().getData();
                        addProduct();

                    } else {
                        Toast.makeText(AddProductActivity.this, "Image upload failed!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                    Toast.makeText(AddProductActivity.this, "Error uploading image: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not read image file!", Toast.LENGTH_SHORT).show();
        }
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
    private void addProduct() {
        try {
            // 1️⃣ Tạo request DTO đơn giản (đồng bộ với backend)
            ProductRequest request = new ProductRequest();
            request.setProductName(edtName.getText().toString().trim());
            request.setBriefDescription(edtBrief.getText().toString().trim());
            request.setFullDescription(edtFull.getText().toString().trim());
            request.setTechnicalSpecifications(edtSpecs.getText().toString().trim());
            request.setPrice(Double.parseDouble(edtPrice.getText().toString().trim()));
            request.setImageURL(uploadedImageUrl);

            int categoryPos = spinnerCategory.getSelectedItemPosition();
            int brandPos = spinnerBrand.getSelectedItemPosition();

            if (categoryPos >= 0 && categoryPos < categoryList.size()) {
                request.setCategoryID(categoryList.get(categoryPos).getCategoryID());
            }

            if (brandPos >= 0 && brandPos < brandList.size()) {
                request.setBrandID(brandList.get(brandPos).getBrandID());
            }

            ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
            Call<ApiResponse<ProductDTO>> call = api.addProduct(request);

            call.enqueue(new Callback<ApiResponse<ProductDTO>>() {
                @Override
                public void onResponse(Call<ApiResponse<ProductDTO>> call, Response<ApiResponse<ProductDTO>> response) {
                    Log.d("AddProduct", "Response code: " + response.code());
                    Log.d("AddProduct", "Response body: " + response.body());

                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(AddProductActivity.this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("addedProduct", true);
                        setResult(RESULT_OK, resultIntent);

                        finish();
                    } else {
                        String errorMsg = "Error adding product! (code " + response.code() + ")";
                        Toast.makeText(AddProductActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<ProductDTO>> call, Throwable t) {
                    Log.e("AddProduct", "Connection error: " + t.getMessage());
                    Toast.makeText(AddProductActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AddProduct", "Invalid data: " + e.getMessage());
            Toast.makeText(this, "Invalid data!", Toast.LENGTH_SHORT).show();
        }
    }

}
