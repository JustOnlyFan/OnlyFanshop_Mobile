package com.example.onlyfanshop.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.BrandAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.BrandApi;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.databinding.ActivityBrandManagementBinding;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.BrandManagementDTO;
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

public class BrandManagementActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private ActivityBrandManagementBinding binding;
    private BrandAdapter brandAdapter;
    private List<BrandManagementDTO> brandList = new ArrayList<>();
    private Uri selectedImageUri;
    private String uploadedImageUrl;
    private boolean isUpdatingBrand = false;
    private BrandManagementDTO updatingBrand;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrandManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.rclViewBrand.setLayoutManager(new LinearLayoutManager(this));
        brandAdapter = new BrandAdapter(this, brandList);
        binding.rclViewBrand.setAdapter(brandAdapter);

        brandAdapter.setOnEditBrandListener(new BrandAdapter.OnEditBrandListener() {
            @Override
            public void onEdit(BrandManagementDTO brand) {
                updateBrand(brand);
            }
            @Override
            public void onSwitchActive(Integer brandID, boolean isActive) {
                switchActive(brandID, isActive);
            }
        });
        getBrands();
        binding.addBrand.setOnClickListener(v->{
            binding.addBrandLayout.setVisibility(View.VISIBLE);
        });
        binding.btnConfirmAdd.setOnClickListener(v->confirmAdd());
        binding.btnChooseImage.setOnClickListener(v -> chooseImage());
        binding.back.setOnClickListener(v->finish());

    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ImageView imgLogo = binding.imgBrandLogo;
            if (imgLogo != null) {
                imgLogo.setImageURI(selectedImageUri);
            }
        }
    }

    private void confirmAdd() {
        BrandManagementDTO brand = new BrandManagementDTO();
        brand.setName(binding.edtName.getText().toString());
        brand.setCountry(binding.edtCountry.getText().toString());
        brand.setDescription(binding.edtDes.getText().toString());
        if(brand.getName().isEmpty() || brand.getCountry().isEmpty() || brand.getDescription().isEmpty()){
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUri != null) {
            uploadImageAndAddBrand(brand);
        } else {
            addBrand(brand);
        }
    }

    private void uploadImageAndAddBrand(BrandManagementDTO brand) {
        try {
            File file = getFileFromUri(selectedImageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
            api.uploadBrandImageToFirebase(body).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedImageUrl = response.body().getData();
                        brand.setImageURL(uploadedImageUrl);
                        addBrand(brand);
                    } else {
                        Toast.makeText(BrandManagementActivity.this, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                    Toast.makeText(BrandManagementActivity.this, "Lỗi upload ảnh: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromUri(Uri uri) throws Exception {
        File file = new File(getCacheDir(), "upload_temp_brand.jpg");
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

    private void addBrand(BrandManagementDTO brand) {
        BrandApi api = ApiClient.getPrivateClient(this).create(BrandApi.class);
        api.createBrand(brand).enqueue(new Callback<BrandManagementDTO>() {
            @Override
            public void onResponse(Call<BrandManagementDTO> call, Response<BrandManagementDTO> response) {
                if(response.isSuccessful()){
                    binding.addBrandLayout.setVisibility(View.GONE);
                    binding.edtCountry.setText("");
                    binding.edtDes.setText("");
                    binding.edtName.setText("");
                    binding.imgBrandLogo.setImageResource(R.drawable.ic_launcher_foreground);
                    selectedImageUri = null;
                    uploadedImageUrl = null;
                    Toast.makeText(BrandManagementActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    getBrands();
                }
            }
            @Override
            public void onFailure(Call<BrandManagementDTO> call, Throwable t) {
                Toast.makeText(BrandManagementActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getBrands() {
        BrandApi api = ApiClient.getPrivateClient(this).create(BrandApi.class);
        api.getAllBrands().enqueue(new Callback<List<BrandManagementDTO>>() {
            @Override
            public void onResponse(Call<List<BrandManagementDTO>> call, Response<List<BrandManagementDTO>> response) {
                if(response.isSuccessful()){
                    brandList = response.body();
                    brandAdapter.setData(brandList);
                }
            }

            @Override
            public void onFailure(Call<List<BrandManagementDTO>> call, Throwable t) {
                Toast.makeText(BrandManagementActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateBrand(BrandManagementDTO brand){
        if (selectedImageUri != null) {
            uploadImageAndUpdateBrand(brand);
        } else {
            updateBrandDirect(brand);
        }
    }

    private void uploadImageAndUpdateBrand(BrandManagementDTO brand) {
        try {
            File file = getFileFromUri(selectedImageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
            api.uploadBrandImageToFirebase(body).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedImageUrl = response.body().getData();
                        brand.setImageURL(uploadedImageUrl);
                        updateBrandDirect(brand);
                    } else {
                        Toast.makeText(BrandManagementActivity.this, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                    Toast.makeText(BrandManagementActivity.this, "Lỗi upload ảnh: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBrandDirect(BrandManagementDTO brand) {
        BrandApi api = ApiClient.getPrivateClient(this).create(BrandApi.class);
        api.updateBrand(brand.getBrandID(), brand).enqueue(new Callback<BrandDTO>() {
            @Override
            public void onResponse(Call<BrandDTO> call, Response<BrandDTO> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(BrandManagementActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    getBrands();
                }
            }
            @Override
            public void onFailure(Call<BrandDTO> call, Throwable t) {
                Toast.makeText(BrandManagementActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void switchActive(int brandID, boolean isActive){
        BrandApi api = ApiClient.getPrivateClient(this).create(BrandApi.class);
        api.toggleActive(brandID, isActive).enqueue(new Callback<BrandDTO>() {

            @Override
            public void onResponse(Call<BrandDTO> call, Response<BrandDTO> response) {

            }

            @Override
            public void onFailure(Call<BrandDTO> call, Throwable t) {
                Toast.makeText(BrandManagementActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}