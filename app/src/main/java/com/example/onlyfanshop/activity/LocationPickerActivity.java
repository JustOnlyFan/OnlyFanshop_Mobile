package com.example.onlyfanshop.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.map.core.interfaces.MapProvider;
import com.example.onlyfanshop.map.impl.map.OsmMapProvider;
import com.example.onlyfanshop.map.core.facade.MapServiceFacade;
import com.example.onlyfanshop.map.models.PlaceSuggestion;
import com.example.onlyfanshop.map.models.GeocodeResult;
import com.example.onlyfanshop.map.config.KeyStorage;

import java.util.List;

public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private MapProvider mapProvider;
    private MapServiceFacade mapService;

    private EditText etSearchLocation;
    private Button btnSearchLocation, btnConfirmLocation;
    private TextView tvSelectedLocationAddress, tvSelectedLocationCoords;
    private ProgressBar progressSearch;
    private FrameLayout mapContainer;

    private double currentLat = 16.0544;
    private double currentLng = 108.2022;
    private String currentAddress = "";

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable reverseGeocodeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        KeyStorage.loadIntoConfig(this);

        initViews();
        initMap();
        setupListeners();

        // Set initial position
        if (getIntent().hasExtra(EXTRA_LATITUDE) && getIntent().hasExtra(EXTRA_LONGITUDE)) {
            currentLat = getIntent().getDoubleExtra(EXTRA_LATITUDE, currentLat);
            currentLng = getIntent().getDoubleExtra(EXTRA_LONGITUDE, currentLng);
            currentAddress = getIntent().getStringExtra(EXTRA_ADDRESS);
            if (currentAddress == null) currentAddress = "";
        }

        mapProvider.moveCamera(currentLat, currentLng, 15.0);
        updateLocationInfo();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnSearchLocation = findViewById(R.id.btnSearchLocation);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvSelectedLocationAddress = findViewById(R.id.tvSelectedLocationAddress);
        tvSelectedLocationCoords = findViewById(R.id.tvSelectedLocationCoords);
        progressSearch = findViewById(R.id.progressSearch);
        mapContainer = findViewById(R.id.mapContainer);
    }

    private void initMap() {
        mapProvider = new OsmMapProvider();
        View mapView = mapProvider.createMapView(this);
        mapContainer.addView(mapView, 0);

        mapService = new MapServiceFacade();

        // Listen for map idle events to update location
        mapProvider.setOnMapClickListener((lat, lng) -> {
            currentLat = lat;
            currentLng = lng;
            scheduleReverseGeocode();
        });
    }

    private void setupListeners() {
        btnSearchLocation.setOnClickListener(v -> searchLocation());
        btnConfirmLocation.setOnClickListener(v -> confirmLocation());

        // Listen for map movements
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Get center of map
                currentLat = mapProvider.getCenterLat();
                currentLng = mapProvider.getCenterLng();
                updateLocationInfo();
                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void searchLocation() {
        String query = etSearchLocation.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        progressSearch.setVisibility(View.VISIBLE);
        btnSearchLocation.setEnabled(false);

        mapService.autocomplete().suggest(query, new com.example.onlyfanshop.map.core.interfaces.AutocompleteProvider.Callback() {
            @Override
            public void onSuccess(List<PlaceSuggestion> results) {
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    btnSearchLocation.setEnabled(true);

                    if (results.isEmpty()) {
                        Toast.makeText(LocationPickerActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    } else {
                        PlaceSuggestion first = results.get(0);
                        currentLat = first.lat;
                        currentLng = first.lng;
                        currentAddress = first.primaryText;

                        mapProvider.moveCamera(currentLat, currentLng, 15.0);
                        updateLocationInfo();
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    btnSearchLocation.setEnabled(true);
                    Toast.makeText(LocationPickerActivity.this, "Search failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scheduleReverseGeocode() {
        if (reverseGeocodeRunnable != null) {
            handler.removeCallbacks(reverseGeocodeRunnable);
        }

        reverseGeocodeRunnable = () -> {
            mapService.geocoding().reverseGeocode(currentLat, currentLng, new com.example.onlyfanshop.map.core.interfaces.GeocodingProvider.Callback() {
                @Override
                public void onSuccess(List<GeocodeResult> results) {
                    runOnUiThread(() -> {
                        if (results != null && !results.isEmpty()) {
                            currentAddress = results.get(0).formattedAddress;
                        } else {
                            currentAddress = "Unknown location";
                        }
                        updateLocationInfo();
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        currentAddress = "Unknown location";
                        updateLocationInfo();
                    });
                }
            });
        };

        handler.postDelayed(reverseGeocodeRunnable, 1000);
    }

    private void updateLocationInfo() {
        tvSelectedLocationAddress.setText(TextUtils.isEmpty(currentAddress) ? "Move map to select location" : currentAddress);
        tvSelectedLocationCoords.setText(String.format("Lat: %.6f, Lng: %.6f", currentLat, currentLng));
    }

    private void confirmLocation() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_LATITUDE, currentLat);
        resultIntent.putExtra(EXTRA_LONGITUDE, currentLng);
        resultIntent.putExtra(EXTRA_ADDRESS, currentAddress);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapProvider != null) mapProvider.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapProvider != null) mapProvider.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapProvider != null) mapProvider.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}

