package com.example.onlyfanshop.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.ProductManagementAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ProductApi;
import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductManagementActivity extends AppCompatActivity implements ProductManagementAdapter.OnProductActionListener {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private ImageButton btnSearch;
    private ProductManagementAdapter adapter;

    private Spinner spinnerCategory, spinnerBrand;
    private List<ProductDTO> productList = new ArrayList<>();
    private boolean isSpinnerInitialized = false;

    private Button btnAdd, btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_management);

        recyclerView = findViewById(R.id.rcvProductList);
        edtSearch = findViewById(R.id.edtSearchProduct);
        btnSearch = findViewById(R.id.btnSearchProduct);
        adapter = new ProductManagementAdapter(productList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        btnAdd = findViewById(R.id.btnAddProduct);


        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ProductManagementActivity.this, AddProductActivity.class);
            startActivityForResult(intent, 100);
        });
        // Gọi API lần đầu
        fetchProducts(null, null, null);

        // Xử lý nút tìm kiếm
        btnSearch.setOnClickListener(v -> {
            String keyword = edtSearch.getText().toString().trim();
            fetchProducts(keyword, null, null);
        });

    }

    List<String> categoryNames = new ArrayList<>();
    List<String> brandNames = new ArrayList<>();

    private void setupSpinners(HomePageData data) {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerBrand = findViewById(R.id.spinnerBrand);

        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("All");
        if (data.categories != null) {
            for (CategoryDTO c : data.categories) {
                categoryNames.add(c.getCategoryName());
            }
        }

        List<String> brandNames = new ArrayList<>();
        brandNames.add("All");
        if (data.brands != null) {
            for (BrandDTO b : data.brands) {
                brandNames.add(b.getName());
            }
        }

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> brandAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brandNames);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBrand.setAdapter(brandAdapter);


        if (data.filters != null) {
            // --- Category ---
            if (data.filters.selectedCategory != null) {
                int categoryIndex = 0;
                for (int i = 0; i < data.categories.size(); i++) {
                    if (data.categories.get(i).getCategoryName()
                            .equalsIgnoreCase(data.filters.selectedCategory)) {
                        categoryIndex = i + 1;
                        break;
                    }
                }
                spinnerCategory.setSelection(categoryIndex);
            }

            // --- Brand ---
            if (data.filters.selectedBrand != null) {
                int brandIndex = 0;
                for (int i = 0; i < data.brands.size(); i++) {
                    if (data.brands.get(i).getName()
                            .equalsIgnoreCase(data.filters.selectedBrand)) {
                        brandIndex = i + 1;
                        break;
                    }
                }
                spinnerBrand.setSelection(brandIndex);
            }
        }

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            boolean initialized = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized) {
                    initialized = true;
                    return;
                }

                Integer categoryId = getSelectedCategoryId(data);
                Integer brandId = getSelectedBrandId(data);
                String keyword = edtSearch.getText().toString().trim();

                fetchProducts(keyword, categoryId, brandId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCategory.setOnItemSelectedListener(listener);
        spinnerBrand.setOnItemSelectedListener(listener);
    }

    private Integer getSelectedCategoryId(HomePageData data) {
        int pos = spinnerCategory.getSelectedItemPosition();
        if (pos > 0 && data.categories != null && pos - 1 < data.categories.size()) {
            return data.categories.get(pos - 1).getCategoryID();
        }
        return null;
    }

    private Integer getSelectedBrandId(HomePageData data) {
        int pos = spinnerBrand.getSelectedItemPosition();
        if (pos > 0 && data.brands != null && pos - 1 < data.brands.size()) {
            Integer id = data.brands.get(pos - 1).getBrandID();
            return (id != null) ? id.intValue() : null;
        }
        return null;
    }






    private void fetchProducts(String keyword, Integer categoryId, Integer brandId) {
        ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
        api.getProductList(
                1,   // page
                10,  // size
                "ProductID",
                "DESC",
                keyword,
                categoryId,
                brandId
        ).enqueue(new Callback<ApiResponse<HomePageData>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomePageData>> call, Response<ApiResponse<HomePageData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    HomePageData data = response.body().getData();

                    productList = data.products;
                    adapter.setFilteredList(productList);

                    if (!isSpinnerInitialized) {
                        setupSpinners(data);
                        isSpinnerInitialized = true;
                    }
                } else {
                    Toast.makeText(ProductManagementActivity.this, "Could not load products!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HomePageData>> call, Throwable t) {
                    Toast.makeText(ProductManagementActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onEdit(ProductDTO product) {
        Toast.makeText(this, "Edit: " + product.getProductName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UpdateProductActivity.class);
        intent.putExtra("productToEdit", product.getProductID());
        intent.putExtra("productURL", product.getImageURL());
        startActivityForResult(intent, 200);
    }

    @Override
    public  void onView(ProductDTO product){
        Intent intent=new Intent(this,ProductDetailManageActivity.class);
        intent.putExtra("productId",product.getProductID());
        startActivity(intent);
    }

    @Override
    public void onToggleActive(ProductDTO product, boolean isActive) {
        ProductApi api = ApiClient.getPrivateClient(this).create(ProductApi.class);
        Log.d("Update active product", "Product: " + product.getProductID() + "onToggleActive: " + isActive);
        api.toggleActive(product.getProductID(), !isActive).enqueue(new Callback<ApiResponse<Void>>() {

            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                fetchProducts(null, null, null);
                Toast.makeText(ProductManagementActivity.this, response.message().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Toast.makeText(ProductManagementActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDelete(ProductDTO product) {
        Toast.makeText(this, "Delete: " + product.getProductName(), Toast.LENGTH_SHORT).show();
        // TODO: call API to delete product
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            boolean addedProduct = data != null && data.getBooleanExtra("addedProduct", false);
            if (addedProduct) {
                fetchProducts(null, null, null);
            }
        }
        if (requestCode == 200 && resultCode == RESULT_OK) {
            boolean updatedProduct = data != null && data.getBooleanExtra("updatedProduct", false);
            if (updatedProduct) {
                fetchProducts(null, null, null);
            }
        }
    }

}
