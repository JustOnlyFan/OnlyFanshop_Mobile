package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.adapter.CategoryManagementAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.CategoryApi;
import com.example.onlyfanshop.databinding.ActivityCategoryManagementBinding;
import com.example.onlyfanshop.model.CategoryManagementDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryManagementActivity extends AppCompatActivity {

    private ActivityCategoryManagementBinding binding;
    private List<CategoryManagementDTO> listCategory = new ArrayList<>();
    private CategoryManagementAdapter categoryAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCategoryManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.rclViewCategory.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryManagementAdapter(this, listCategory);
        binding.rclViewCategory.setAdapter(categoryAdapter);

        categoryAdapter.setOnEditCategoryListener(new CategoryManagementAdapter.OnEditCategoryListener() {
            @Override
            public void onEdit(CategoryManagementDTO category) {
                updateCategory(category);
            }

            @Override
            public void onDelete(Integer categoryID) {
                deleteCategory(categoryID);
            }
            @Override
            public void onSwitchActive(Integer categoryID, boolean isActive) {
                switchActive(categoryID, isActive);
            }
        });
        getCategories();
        binding.addCategory.setOnClickListener(v -> {
            binding.addCategoryLayout.setVisibility(View.VISIBLE);
            binding.confirmAdd.setOnClickListener(v1 -> {
                CategoryManagementDTO category = new CategoryManagementDTO();
                category.setCategoryName(binding.edtAddName.getText().toString());
                category.setActive(true);
                if(category.getCategoryName().isEmpty()){
                    Toast.makeText(CategoryManagementActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                }else{
                    createCategory(category);
                }

            });
        });
        binding.back.setOnClickListener(v -> finish());
    }
    private void createCategory(CategoryManagementDTO category){
        CategoryApi api = ApiClient.getPrivateClient(this).create(CategoryApi.class);
        api.createCategory(category).enqueue(new Callback<CategoryManagementDTO>() {
            @Override
            public void onResponse(Call<CategoryManagementDTO> call, Response<CategoryManagementDTO> response) {
                if(response.isSuccessful()){
                    binding.addCategoryLayout.setVisibility(View.GONE);
                    binding.edtAddName.setText("");
                    Toast.makeText(CategoryManagementActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    getCategories();
                }
            }

            @Override
            public void onFailure(Call<CategoryManagementDTO> call, Throwable throwable) {
                Toast.makeText(CategoryManagementActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getCategories() {
        CategoryApi api = ApiClient.getPrivateClient(this).create(CategoryApi.class);
        api.getAllCategories().enqueue(new Callback<List<CategoryManagementDTO>>() {
            @Override
            public void onResponse(Call<List<CategoryManagementDTO>> call, Response<List<CategoryManagementDTO>> response) {
                if (response.isSuccessful()){
                    listCategory = response.body();
                    categoryAdapter.setData(listCategory);
                }
            }

            @Override
            public void onFailure(Call<List<CategoryManagementDTO>> call, Throwable throwable) {
                Toast.makeText(CategoryManagementActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();

            }
        });

    }
    private void updateCategory(CategoryManagementDTO category) {
        CategoryApi api = ApiClient.getPrivateClient(this).create(CategoryApi.class);
        api.updateCategory(category.getCategoryID(), category).enqueue(new Callback<CategoryManagementDTO>() {
            @Override
            public void onResponse(Call<CategoryManagementDTO> call, Response<CategoryManagementDTO> response) {
                if(response.isSuccessful()){
                    Toast.makeText(CategoryManagementActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    getCategories();
                }
            }

            @Override
            public void onFailure(Call<CategoryManagementDTO> call, Throwable throwable) {
                Toast.makeText(CategoryManagementActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void deleteCategory(Integer categoryID) {
        CategoryApi api = ApiClient.getPrivateClient(this).create(CategoryApi.class);
        api.deleteCategory(categoryID).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if(response.isSuccessful()){
                    Toast.makeText(CategoryManagementActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                    getCategories();
                }else{
                    Toast.makeText(CategoryManagementActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Toast.makeText(CategoryManagementActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void switchActive(Integer categoryID, boolean isActive) {
        CategoryApi api = ApiClient.getPrivateClient(this).create(CategoryApi.class);
        api.toggleActive(categoryID, isActive).enqueue(new Callback<CategoryManagementDTO>() {
            @Override
            public void onResponse(Call<CategoryManagementDTO> call, Response<CategoryManagementDTO> response) {
                if(response.isSuccessful()){
                    Toast.makeText(CategoryManagementActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    getCategories();
                }
            }
            @Override
            public void onFailure(Call<CategoryManagementDTO> call, Throwable throwable) {
                    Toast.makeText(CategoryManagementActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}