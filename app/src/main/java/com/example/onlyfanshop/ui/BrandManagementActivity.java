package com.example.onlyfanshop.ui;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.BrandAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.BrandApi;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.databinding.ActivityBrandManagementBinding;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.BrandManagementDTO;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BrandManagementActivity extends AppCompatActivity {

    private ActivityBrandManagementBinding binding;
    private BrandAdapter brandAdapter;
    private List<BrandManagementDTO> brandList = new ArrayList<>();


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
        binding.back.setOnClickListener(v->finish());

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
        BrandApi api = ApiClient.getPrivateClient(this).create(BrandApi.class);
        api.createBrand(brand).enqueue(new Callback<BrandManagementDTO>() {

            @Override
            public void onResponse(Call<BrandManagementDTO> call, Response<BrandManagementDTO> response) {
                if(response.isSuccessful()){
                    binding.addBrandLayout.setVisibility(View.GONE);
                    binding.edtCountry.setText("");
                    binding.edtDes.setText("");
                    binding.edtName.setText("");
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