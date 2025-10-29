package com.example.onlyfanshop.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.StoreLocationAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.StoreLocationApi;
import com.example.onlyfanshop.model.StoreLocation;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreManagementActivity extends AppCompatActivity implements StoreLocationAdapter.OnStoreActionListener {

    private static final int REQUEST_ADD_STORE = 101;
    private static final int REQUEST_EDIT_STORE = 102;

    private RecyclerView rvStores;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearchStore;
    private FloatingActionButton fabAddStore;

    private StoreLocationAdapter adapter;
    private StoreLocationApi storeLocationApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_management);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadStores();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvStores = findViewById(R.id.rvStores);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        etSearchStore = findViewById(R.id.etSearchStore);
        fabAddStore = findViewById(R.id.fabAddStore);

        storeLocationApi = ApiClient.getPrivateClient(this).create(StoreLocationApi.class);
    }

    private void setupRecyclerView() {
        adapter = new StoreLocationAdapter(this);
        rvStores.setLayoutManager(new LinearLayoutManager(this));
        rvStores.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAddStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditStoreActivity.class);
            startActivityForResult(intent, REQUEST_ADD_STORE);
        });

        etSearchStore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStores() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        rvStores.setVisibility(View.GONE);

        storeLocationApi.getAllStoreLocations().enqueue(new Callback<ApiResponse<List<StoreLocation>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<StoreLocation>>> call, Response<ApiResponse<List<StoreLocation>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<StoreLocation> stores = response.body().getData();
                    if (stores.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvStores.setVisibility(View.VISIBLE);
                        adapter.setStores(stores);
                    }
                } else {
                    emptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(StoreManagementActivity.this, "Failed to load stores", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<StoreLocation>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                Toast.makeText(StoreManagementActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditStore(StoreLocation store) {
        Intent intent = new Intent(this, AddEditStoreActivity.class);
        intent.putExtra(AddEditStoreActivity.EXTRA_STORE_LOCATION, store);
        startActivityForResult(intent, REQUEST_EDIT_STORE);
    }

    @Override
    public void onDeleteStore(StoreLocation store) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Store")
                .setMessage("Are you sure you want to delete " + store.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteStore(store))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStoreClick(StoreLocation store) {
        // Optional: Show store details
        Toast.makeText(this, store.getName(), Toast.LENGTH_SHORT).show();
    }

    private void deleteStore(StoreLocation store) {
        progressBar.setVisibility(View.VISIBLE);

        storeLocationApi.deleteStoreLocation(store.getLocationID()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(StoreManagementActivity.this, "Store deleted successfully", Toast.LENGTH_SHORT).show();
                    loadStores();
                } else {
                    Toast.makeText(StoreManagementActivity.this, "Failed to delete store", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StoreManagementActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD_STORE || requestCode == REQUEST_EDIT_STORE) && resultCode == RESULT_OK) {
            loadStores();
        }
    }
}

