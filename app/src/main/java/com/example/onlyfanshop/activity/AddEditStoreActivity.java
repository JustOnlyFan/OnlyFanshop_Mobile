package com.example.onlyfanshop.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.StoreLocationApi;
import com.example.onlyfanshop.model.StoreLocation;
import com.example.onlyfanshop.model.response.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditStoreActivity extends AppCompatActivity {

    public static final String EXTRA_STORE_LOCATION = "store_location";
    private static final int REQUEST_PICK_LOCATION = 100;

    private EditText etStoreName, etStoreDescription, etStorePhone, etStoreHours, etStoreImageUrl;
    private TextView tvSelectedAddress, tvLatLng;
    private Button btnPickLocation, btnSaveStore;
    private LinearLayout locationInfoLayout;
    private ProgressBar progressBar;

    private Double selectedLatitude;
    private Double selectedLongitude;
    private String selectedAddress;

    private StoreLocation storeToEdit;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_store);

        initViews();
        checkEditMode();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etStoreName = findViewById(R.id.etStoreName);
        etStoreDescription = findViewById(R.id.etStoreDescription);
        etStorePhone = findViewById(R.id.etStorePhone);
        etStoreHours = findViewById(R.id.etStoreHours);
        etStoreImageUrl = findViewById(R.id.etStoreImageUrl);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvLatLng = findViewById(R.id.tvLatLng);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnSaveStore = findViewById(R.id.btnSaveStore);
        locationInfoLayout = findViewById(R.id.locationInfoLayout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void checkEditMode() {
        if (getIntent().hasExtra(EXTRA_STORE_LOCATION)) {
            isEditMode = true;
            storeToEdit = (StoreLocation) getIntent().getSerializableExtra(EXTRA_STORE_LOCATION);
            if (storeToEdit != null) {
                populateFields(storeToEdit);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Edit Store");
                }
            }
        }
    }

    private void populateFields(StoreLocation store) {
        etStoreName.setText(store.getName());
        etStoreDescription.setText(store.getDescription());
        etStorePhone.setText(store.getPhone());
        etStoreHours.setText(store.getOpeningHours());
        etStoreImageUrl.setText(store.getImageUrl());

        selectedLatitude = store.getLatitude();
        selectedLongitude = store.getLongitude();
        selectedAddress = store.getAddress();

        updateLocationDisplay();
    }

    private void setupListeners() {
        btnPickLocation.setOnClickListener(v -> openLocationPicker());
        btnSaveStore.setOnClickListener(v -> saveStore());
    }

    private void openLocationPicker() {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        if (selectedLatitude != null && selectedLongitude != null) {
            intent.putExtra(LocationPickerActivity.EXTRA_LATITUDE, selectedLatitude);
            intent.putExtra(LocationPickerActivity.EXTRA_LONGITUDE, selectedLongitude);
            intent.putExtra(LocationPickerActivity.EXTRA_ADDRESS, selectedAddress);
        }
        startActivityForResult(intent, REQUEST_PICK_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, 0);
            selectedLongitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, 0);
            selectedAddress = data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS);
            updateLocationDisplay();
        }
    }

    private void updateLocationDisplay() {
        if (selectedLatitude != null && selectedLongitude != null) {
            tvSelectedAddress.setText(selectedAddress != null ? selectedAddress : "Location selected");
            tvLatLng.setText(String.format("Lat: %.6f, Lng: %.6f", selectedLatitude, selectedLongitude));
            locationInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void saveStore() {
        // Validate inputs
        String name = etStoreName.getText().toString().trim();
        String description = etStoreDescription.getText().toString().trim();
        String phone = etStorePhone.getText().toString().trim();
        String hours = etStoreHours.getText().toString().trim();
        String imageUrl = etStoreImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etStoreName.setError("Store name is required");
            etStoreName.requestFocus();
            return;
        }

        if (selectedLatitude == null || selectedLongitude == null || TextUtils.isEmpty(selectedAddress)) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create StoreLocation object
        StoreLocation storeLocation = new StoreLocation();
        if (isEditMode && storeToEdit != null) {
            storeLocation.setLocationID(storeToEdit.getLocationID());
        }
        storeLocation.setName(name);
        storeLocation.setDescription(description);
        storeLocation.setPhone(phone);
        storeLocation.setOpeningHours(hours);
        storeLocation.setImageUrl(imageUrl);
        storeLocation.setLatitude(selectedLatitude);
        storeLocation.setLongitude(selectedLongitude);
        storeLocation.setAddress(selectedAddress);

        // Save to server
        progressBar.setVisibility(View.VISIBLE);
        btnSaveStore.setEnabled(false);

        StoreLocationApi api = ApiClient.getPrivateClient(this).create(StoreLocationApi.class);
        Call<ApiResponse<StoreLocation>> call;

        if (isEditMode && storeToEdit != null) {
            call = api.updateStoreLocation(storeToEdit.getLocationID(), storeLocation);
        } else {
            call = api.createStoreLocation(storeLocation);
        }

        call.enqueue(new Callback<ApiResponse<StoreLocation>>() {
            @Override
            public void onResponse(Call<ApiResponse<StoreLocation>> call, Response<ApiResponse<StoreLocation>> response) {
                progressBar.setVisibility(View.GONE);
                btnSaveStore.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AddEditStoreActivity.this, 
                        isEditMode ? "Store updated successfully" : "Store created successfully", 
                        Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(AddEditStoreActivity.this, "Failed to save store", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StoreLocation>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSaveStore.setEnabled(true);
                Toast.makeText(AddEditStoreActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

