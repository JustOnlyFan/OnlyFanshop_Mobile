package com.example.onlyfanshop.ui.store;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.onlyfanshop.R;
import com.example.onlyfanshop.activity.LocationPickerActivity;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.StoreLocationApi;
import com.example.onlyfanshop.model.StoreLocation;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditStoreActivity extends AppCompatActivity {

    public static final String EXTRA_STORE_LOCATION = "store_location";
    private static final int REQUEST_PICK_LOCATION = 100;
    private static final int REQUEST_PICK_IMAGE = 200;

    private EditText etStoreName, etStoreDescription, etStorePhone, etStoreHours, etStoreImageUrl;
    private TextView tvSelectedAddress, tvLatLng, tvImageSelected;
    private Button btnPickLocation, btnSaveStore, btnChooseImage;
    private LinearLayout locationInfoLayout;
    private ProgressBar progressBar;
    private ImageView imgPreview;

    private Double selectedLatitude;
    private Double selectedLongitude;
    private String selectedAddress;
    private Uri selectedImageUri;

    private StoreLocation storeToEdit;
    private boolean isEditMode = false;
    private boolean isImageUploaded = false;

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
        btnChooseImage = findViewById(R.id.btnChooseImage);
        tvImageSelected = findViewById(R.id.tvImageSelected);
        imgPreview = findViewById(R.id.imgPreview);
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

        if (!TextUtils.isEmpty(store.getImageUrl())) {
            Glide.with(this).load(store.getImageUrl()).into(imgPreview);
            tvImageSelected.setText("Image loaded from server");
        }

        updateLocationDisplay();
    }

    private void setupListeners() {
        btnPickLocation.setOnClickListener(v -> openLocationPicker());
        btnSaveStore.setOnClickListener(v -> {
            if (selectedImageUri != null && !isImageUploaded) {
                uploadImageToFirebase();
            } else {
                saveStore();
            }
        });

        btnChooseImage.setOnClickListener(v -> openImagePicker());
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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
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

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                imgPreview.setImageURI(selectedImageUri);
                tvImageSelected.setText("Image selected");
                isImageUploaded = false;
            }
        }
    }

    private void updateLocationDisplay() {
        if (selectedLatitude != null && selectedLongitude != null) {
            tvSelectedAddress.setText(selectedAddress != null ? selectedAddress : "Location selected");
            tvLatLng.setText(String.format("Lat: %.6f, Lng: %.6f", selectedLatitude, selectedLongitude));
            locationInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void uploadImageToFirebase() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveStore.setEnabled(false);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("stores/" + UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String firebaseUrl = uri.toString();
                    etStoreImageUrl.setText(firebaseUrl);
                    isImageUploaded = true;
                    progressBar.setVisibility(View.GONE);
                    btnSaveStore.setEnabled(true);
                    Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show();
                    saveStore();
                }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveStore.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveStore() {
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
            public void onResponse(@NonNull Call<ApiResponse<StoreLocation>> call, @NonNull Response<ApiResponse<StoreLocation>> response) {
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
            public void onFailure(@NonNull Call<ApiResponse<StoreLocation>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSaveStore.setEnabled(true);
                Toast.makeText(AddEditStoreActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
