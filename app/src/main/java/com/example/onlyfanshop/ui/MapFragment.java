package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ViewModel.MapViewModel;
import com.example.onlyfanshop.adapter.AttractionAdapter;
import com.example.onlyfanshop.adapter.SuggestionAdapter;
import com.example.onlyfanshop.map.config.KeyStorage;
import com.example.onlyfanshop.map.config.MapConfig;
import com.example.onlyfanshop.map.core.interfaces.MapProvider;
import com.example.onlyfanshop.map.impl.map.OsmMapProvider;
import com.example.onlyfanshop.map.models.GeocodeResult;
import com.example.onlyfanshop.map.models.PlaceSuggestion;
import com.example.onlyfanshop.map.models.RouteResult;
import com.example.onlyfanshop.map.shop.Shop;
import com.example.onlyfanshop.map.shop.ShopDetailBottomSheet;
import com.example.onlyfanshop.map.shop.ShopMarkerManager;
import com.example.onlyfanshop.map.shop.ShopRepository;
import com.example.onlyfanshop.map.shop.ShopUiMapper;
import com.example.onlyfanshop.model.Attraction;
import com.example.onlyfanshop.map.shop.AttractionCarouselController;
import com.example.onlyfanshop.map.shop.DatabaseShopDataSource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    // Default location: Việt Nam (center)
    private static final double DEFAULT_LAT_VN = 16.0;
    private static final double DEFAULT_LNG_VN = 108.0;
    private static final float DEFAULT_ZOOM_VN = 6.0f;

    private MapViewModel vm;
    private MapProvider mapProvider;

    private ImageButton btnZoomIn, btnZoomOut;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView tvLocation;
    private RecyclerView rvSuggestions, rvAttractions;
//    private TextView tvRouteInfo;
    private LinearLayout routePanel;

    private final List<PlaceSuggestion> currentSuggestions = new ArrayList<>();
    private SuggestionAdapter suggestionAdapter;

    private ShopRepository shopRepository;
    private ShopMarkerManager markerManager;
    private AttractionCarouselController carouselController;

    private List<Shop> shops = new ArrayList<>();
    private List<Attraction> attractions = new ArrayList<>();
    private AttractionAdapter attractionAdapter;

    private Double routeStartLat = null;
    private Double routeStartLng = null;
    private String routeStartAddress = null;
    private boolean isGeocodingInProgress = false;

    private String currentUserAddress = null;

    private Attraction aPendingRoutingAttraction = null;


    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        vm = new ViewModelProvider(this).get(MapViewModel.class);

        etSearch = v.findViewById(R.id.etSearch);
        btnClearSearch = v.findViewById(R.id.btnClearSearch);
        tvLocation = v.findViewById(R.id.tvLocation);
        rvSuggestions = v.findViewById(R.id.rvSuggestions);
        rvAttractions = v.findViewById(R.id.rvAttractions);
//        tvRouteInfo = v.findViewById(R.id.tvRouteInfo);
        routePanel = v.findViewById(R.id.routePanel);
        btnZoomIn = v.findViewById(R.id.btnZoomIn);
        btnZoomOut = v.findViewById(R.id.btnZoomOut);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        tvLocation.setText("Việt Nam");
        KeyStorage.loadIntoConfig(requireContext());

        initSuggestionAdapter();
        initMap(v);
        initCarousel(); // Init carousel trước
        initData(); // Load data sau khi carousel đã ready
        bindViewModel();
        bindEvents();

        btnZoomIn.setOnClickListener(view -> {
            float currentZoom = mapProvider.getZoomLevel();
            mapProvider.moveCamera(mapProvider.getCenterLat(), mapProvider.getCenterLng(), currentZoom + 1);
        });
        btnZoomOut.setOnClickListener(view -> {
            float currentZoom = mapProvider.getZoomLevel();
            mapProvider.moveCamera(mapProvider.getCenterLat(), mapProvider.getCenterLng(), currentZoom - 1);
        });
    }

    private void getCurrentUserLocation(OnLocationReadyCallback callback) {
        // Kiểm tra permission
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            // Callback vẫn chưa thể gọi, sẽ gọi lại sau khi user cấp quyền, cần lưu callback lại nếu muốn gọi tiếp
            return;
        }

        // Lấy vị trí hiện tại
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        Log.d("MapFragment", "User location: " + lat + ", " + lng);

                        // Di chuyển camera đến vị trí người dùng
                        mapProvider.moveCamera(lat, lng, 13.0);

                        // Hiển thị marker vị trí người dùng
                        showUserLocationMarker(lat, lng);

                        tvLocation.setText("Your Location");

                        if (callback != null) {
                            callback.onLocationReady(lat, lng);
                        }
                    } else {
                        Log.w("MapFragment", "Location is null, using default Vietnam location");
                        // Nếu không lấy được vị trí, hiển thị Việt Nam
                        mapProvider.moveCamera(DEFAULT_LAT_VN, DEFAULT_LNG_VN, DEFAULT_ZOOM_VN);
                        tvLocation.setText("Việt Nam");
                        if (callback != null) {
                            callback.onLocationFailed();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MapFragment", "Failed to get location: " + e.getMessage());
                    Toast.makeText(getContext(), "Cannot get your location", Toast.LENGTH_SHORT).show();
                    // Fallback to default Vietnam view
                    mapProvider.moveCamera(DEFAULT_LAT_VN, DEFAULT_LNG_VN, DEFAULT_ZOOM_VN);
                    if (callback != null) {
                        callback.onLocationFailed();
                    }
                });
    }

    private void showUserLocationMarker(double lat, double lng) {
        try {
            mapProvider.removeMarker("user_location");
            Drawable userIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_my_location);
            if (userIcon != null) {
                userIcon.setBounds(0, 0, 40, 40);
            }
            mapProvider.addMarker("user_location", lat, lng, "Your Location", "", userIcon);
        } catch (Exception e) {
            Log.e("MapFragment", "Error showing user location marker: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location again
                getCurrentUserLocation(null);
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showStartPin(double lat, double lng, String address) {
        // Xóa marker "start" cũ nếu có
        mapProvider.removeMarker("start");
        // Thêm marker "start" mới với icon riêng biệt, chỉnh size icon nếu cần
        Drawable startIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pin_start);
        if (startIcon != null) {
            startIcon.setBounds(0, 0, 48, 48); // chỉnh size nhỏ hơn nếu muốn
        }
        mapProvider.addMarker("start", lat, lng, address, "", startIcon);
    }

    // Hiển thị từng shop một pin riêng biệt
    private void showShopPins(List<Shop> shops) {
        for (Shop shop : shops) {
            String markerId = "shop_" + shop.getId();
            double lat = shop.getLatitude();
            double lng = shop.getLongitude();
            String title = shop.getName();
            String snippet = shop.getAddress(); // hoặc ""
            mapProvider.addMarker(markerId, lat, lng, title, snippet);
            // Nếu muốn icon riêng cho shop thì dùng overload với Drawable icon
        }
    }

    private void initSuggestionAdapter() {
        suggestionAdapter = new SuggestionAdapter(currentSuggestions, suggestion -> {
            etSearch.setText(suggestion.primaryText);
            rvSuggestions.setVisibility(View.GONE);
            etSearch.setEnabled(false);

            if (!Double.isNaN(suggestion.lat) && !Double.isNaN(suggestion.lng)) {
                showStartPin(suggestion.lat, suggestion.lng, suggestion.primaryText);
                mapProvider.moveCamera(suggestion.lat, suggestion.lng, 15f);

                routeStartLat = suggestion.lat;
                routeStartLng = suggestion.lng;
                routeStartAddress = suggestion.primaryText;
                isGeocodingInProgress = false;

                // Cập nhật lại số km cho directions ngay khi chọn suggestion
                if (attractionAdapter != null)
                    attractionAdapter.setCurrentLocation(routeStartLat, routeStartLng);
            } else {
                isGeocodingInProgress = true;
                vm.search(suggestion.primaryText);
                routeStartAddress = suggestion.primaryText;
            }
        });
        rvSuggestions.setAdapter(suggestionAdapter);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    private void initMap(View root) {
        mapProvider = new OsmMapProvider();
        FrameLayout container = root.findViewById(R.id.mapContainer);
        View mapView = mapProvider.createMapView(requireContext());
        container.addView(mapView);

        markerManager = new ShopMarkerManager(mapProvider);

        mapProvider.moveCamera(DEFAULT_LAT_VN, DEFAULT_LNG_VN, DEFAULT_ZOOM_VN);

        mapProvider.setOnMapClickListener((lat, lng) -> rvSuggestions.setVisibility(View.GONE));
        mapProvider.setOnMapLongClickListener((lat, lng) -> {
            routeStartLat = lat;
            routeStartLng = lng;
            routeStartAddress = String.format("Lat: %.5f, Lng: %.5f", lat, lng);
            showStartPin(lat, lng, routeStartAddress);
            Toast.makeText(getContext(), "Start point selected", Toast.LENGTH_SHORT).show();
            isGeocodingInProgress = false;
        });
    }

    private void initData() {
        shopRepository = ShopRepository.getInstance();

        // Load stores from database
        loadStoresFromDatabase();
    }

    private void loadStoresFromDatabase() {
        shopRepository.loadFromDatabase(new DatabaseShopDataSource.OnDataLoadedListener() {
            @Override
            public void onDataLoaded(List<Shop> loadedShops) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Log.d("MapFragment", "onDataLoaded callback - Received " + loadedShops.size() + " stores");

                    // Update shops list
                    shops.clear();
                    shops.addAll(loadedShops);
                    Log.d("MapFragment", "shops list size after update: " + shops.size());

                    // Convert to attractions
                    List<Attraction> newAttractions = ShopUiMapper.toAttractions(shops);
                    Log.d("MapFragment", "Converted to " + newAttractions.size() + " attractions");

                    attractions.clear();
                    attractions.addAll(newAttractions);
                    Log.d("MapFragment", "attractions list size: " + attractions.size());

                    // Show all store pins on map
                    if (!shops.isEmpty()) {
                        showShopPins(shops);
                        Log.d("MapFragment", "Called showShopPins with " + shops.size() + " shops");
                    }

                    // Update adapter với data mới
                    Log.d("MapFragment", "Before updateData - attractions size: " + attractions.size());
                    if (attractionAdapter != null) {
                        attractionAdapter.updateData(new ArrayList<>(attractions));
                        Log.d("MapFragment", "After updateData - adapter count: " + attractionAdapter.getItemCount());
                    }

                    // Attach carousel controller với callback
                    if (carouselController != null && !attractions.isEmpty()) {
                        // Make sure RecyclerView is visible and has correct height
                        if (rvAttractions != null) {
                            rvAttractions.setVisibility(View.VISIBLE);
                            ViewGroup.LayoutParams params = rvAttractions.getLayoutParams();
                            if (params != null) {
                                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                rvAttractions.setLayoutParams(params);
                            }
                            Log.d("MapFragment", "RecyclerView set to VISIBLE");
                        }

                        carouselController.attach(attractions, (attraction, position) -> {
                            // Khi lướt carousel, map tự động zoom vào store đó
                            Shop shop = shopRepository.findById(attraction.getId());
                            if (shop != null) {
                                Log.d("MapFragment", "🎯 Carousel snapped to: " + shop.getName() + " at position " + position);
                                focusShop(shop);
                            }
                        });

                        // Force initial focus on first store
                        if (!shops.isEmpty()) {
                            rvAttractions.post(() -> {
                                focusShop(shops.get(0));
                                Log.d("MapFragment", "Initial focus on first store: " + shops.get(0).getName());
                            });
                        }

                        Log.d("MapFragment", "✅ Attached carousel controller with " + attractions.size() + " items");
                    } else {
                        Log.w("MapFragment", "⚠️ Carousel not attached - controller: " + (carouselController != null) + ", attractions: " + attractions.size());
                        if (rvAttractions != null) {
                            rvAttractions.setVisibility(View.GONE);
                        }
                    }

                    Log.d("MapFragment", "✅ UI updated complete - shops: " + shops.size() + ", attractions: " + attractions.size());

                });
            }

            @Override
            public void onDataLoadFailed(String error) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Log.e("MapFragment", "Failed to load stores: " + error);

                    // Show user-friendly message
                    String userMessage;
                    if (error.contains("Connection refused") || error.contains("Failed to connect")) {
                        userMessage = "Cannot connect to server. Please check if backend is running.";
                    } else if (error.contains("UnknownHost")) {
                        userMessage = "Cannot reach server. Check network connection.";
                    } else if (error.contains("timeout")) {
                        userMessage = "Server timeout. Try again later.";
                    } else {
                        userMessage = "Cannot load stores. " + error;
                    }

                    Toast.makeText(getContext(), userMessage, Toast.LENGTH_LONG).show();

                    // Clear everything
                    shops.clear();
                    attractions.clear();
                    if (attractionAdapter != null) {
                        attractionAdapter.updateData(attractions);
                    }
                });
            }
        });
    }

    private void initCarousel() {
        attractionAdapter = new AttractionAdapter(attractions, new AttractionAdapter.OnAttractionClickListener() {
            @Override
            public void onAttractionClick(Attraction a) {
                Shop s = shopRepository.findById(a.getId());
                if (s != null) {
                    showShopDetail(s);
                }
            }

            @Override
            public void onDirectionsClick(Attraction a) {
                if (routeStartLat != null && routeStartLng != null) {
                    // Đã có địa chỉ bắt đầu
                    vm.route(routeStartLat, routeStartLng, a.getLatitude(), a.getLongitude(), MapConfig.ROUTE_MAX_ALTERNATIVES);
                } else {
                    // Lấy lại location thiết bị, reverse geocode để lấy địa chỉ rồi mới routing
                    aPendingRoutingAttraction = a;
                    getCurrentUserLocation(new OnLocationReadyCallback() {
                        @Override
                        public void onLocationReady(double lat, double lng) {
                            showUserLocationMarker(lat, lng);
                            vm.reverseGeocode(lat, lng);
                        }
                        @Override
                        public void onLocationFailed() {
                            Toast.makeText(getContext(), "Không lấy được vị trí bắt đầu!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if (isGeocodingInProgress) {
                    Toast.makeText(getContext(), "Please wait, getting start location...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (routeStartLat == null || routeStartLng == null) {
                    Toast.makeText(getContext(), "Please select a start point", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("ROUTE", "Routing from: " + routeStartLat + "," + routeStartLng +
                        " to: " + a.getLatitude() + "," + a.getLongitude());
                vm.route(routeStartLat, routeStartLng, a.getLatitude(), a.getLongitude(), MapConfig.ROUTE_MAX_ALTERNATIVES);
            }
        });
        LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        carouselController = new AttractionCarouselController(rvAttractions, lm, attractionAdapter);
    }

    private void focusShop(Shop s) {
        Log.d("MapFragment", "Focusing on shop: " + s.getName() + " at " + s.getLatitude() + ", " + s.getLongitude());

        // Zoom vào store với animation
        mapProvider.moveCamera(s.getLatitude(), s.getLongitude(), 16f);

        // Highlight marker
        markerManager.showSelectedMarker(s);

        // Update location text
        if (tvLocation != null) {
            tvLocation.setText(s.getName());
        }
    }

    private void bindViewModel() {
        vm.getGeocodeResults().observe(getViewLifecycleOwner(), results -> {
            isGeocodingInProgress = false;

            if (results == null || results.isEmpty()) {
                Toast.makeText(getContext(), "Not found", Toast.LENGTH_SHORT).show();
                return;
            }
            GeocodeResult r = results.get(0);

            // Nếu là vị trí thiết bị, hiện marker location, ngược lại marker start
            if (r.formattedAddress.equals("Your Location")) {
                mapProvider.removeMarker("start");
                showUserLocationMarker(r.lat, r.lng);
            } else {
                mapProvider.removeMarker("user_location");
                showStartPin(r.lat, r.lng, r.formattedAddress);
            }

            etSearch.setText(r.formattedAddress);
            etSearch.setText(r.formattedAddress); // Display address on search bar
            showStartPin(r.lat, r.lng, r.formattedAddress);
            mapProvider.moveCamera(r.lat, r.lng, 15);

            routeStartLat = r.lat;
            routeStartLng = r.lng;
            routeStartAddress = r.formattedAddress;
            currentUserAddress = r.formattedAddress;

            // Luôn cập nhật lại số km khi có địa chỉ mới hoặc vị trí mới
            if (attractionAdapter != null)
                attractionAdapter.setCurrentLocation(routeStartLat, routeStartLng);

            // Nếu vừa lấy location thiết bị lại mà có request routing thì thực thi routing
            if (aPendingRoutingAttraction != null) {
                vm.route(routeStartLat, routeStartLng, aPendingRoutingAttraction.getLatitude(), aPendingRoutingAttraction.getLongitude(), MapConfig.ROUTE_MAX_ALTERNATIVES);
                aPendingRoutingAttraction = null;
            }
        });

        vm.getSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions == null) return;
            currentSuggestions.clear();
            currentSuggestions.addAll(suggestions);
            suggestionAdapter.notifyDataSetChanged();
            rvSuggestions.setVisibility((suggestions.isEmpty() || !etSearch.isEnabled()) ? View.GONE : View.VISIBLE);
        });

        vm.getRouteResults().observe(getViewLifecycleOwner(), routes -> {
            if (routes == null || routes.isEmpty()) return;
            RouteResult main = routes.get(0);
            mapProvider.clearPolyline("route_main");
            mapProvider.addPolyline("route_main", main.path, 0xFF0066FF, 8f);
//            tvRouteInfo.setText(String.format("Dist: %.1f km | Time: %.1f min",
//                    main.distanceMeters / 1000.0, main.durationSeconds / 60.0));
            routePanel.setVisibility(View.VISIBLE);
        });

        vm.getError().observe(getViewLifecycleOwner(), err -> {
            isGeocodingInProgress = false;
            if (err != null) Toast.makeText(getContext(), "Lỗi: " + err, Toast.LENGTH_SHORT).show();
        });
    }

    private void bindEvents() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!etSearch.isEnabled()) return;
                if (s.length() > 2) {
                    vm.autoComplete(s.toString());
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                    btnClearSearch.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnClickListener(v -> {
            if (!etSearch.isEnabled()) {
                etSearch.setEnabled(true);
                etSearch.setSelection(etSearch.getText().length());
                if (etSearch.getText().length() > 2 && !currentSuggestions.isEmpty()) {
                    rvSuggestions.setVisibility(View.VISIBLE);
                }
            }
        });

        btnClearSearch.setOnClickListener(view -> {
            etSearch.setText("");
            rvSuggestions.setVisibility(View.GONE);
            btnClearSearch.setVisibility(View.GONE);
            etSearch.setEnabled(true);

            routeStartLat = null;
            routeStartLng = null;
            routeStartAddress = null;
            currentUserAddress = null;
            isGeocodingInProgress = false;

            mapProvider.removeMarker("start");
            mapProvider.removeMarker("user_location");
            mapProvider.clearPolyline("route_main");

            // Khi clear, set lại số km thành null (không hiện số km)
            if (attractionAdapter != null)
                attractionAdapter.setCurrentLocation(null, null);
        });

        // BỎ sự kiện routePanel (không còn routePanel nữa)
    }

    private void updateAttractionDistances() {
        if (routeStartLat == null || routeStartLng == null) {
            // Nếu không có vị trí bắt đầu, hiện (??km)
            if (attractionAdapter != null) {
                attractionAdapter.setCurrentLocation(null, null);
            }
        } else {
            if (attractionAdapter != null) {
                attractionAdapter.setCurrentLocation(routeStartLat, routeStartLng);
            }
        }
    }

    private void showShopDetail(Shop shop) {
        ShopDetailBottomSheet.newInstance(shop)
                .show(getParentFragmentManager(), "shop_detail");
    }

    // Callback cho lấy vị trí user xong
    private interface OnLocationReadyCallback {
        void onLocationReady(double lat, double lng);
        void onLocationFailed();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tự động lấy lại location mỗi lần vào fragment
        getCurrentUserLocation(new OnLocationReadyCallback() {
            @Override
            public void onLocationReady(double lat, double lng) {
                routeStartLat = lat;
                routeStartLng = lng;
                vm.reverseGeocode(lat, lng);
                // Cập nhật lại số km
                if (attractionAdapter != null)
                    attractionAdapter.setCurrentLocation(lat, lng);
            }
            @Override
            public void onLocationFailed() {
                tvLocation.setText("Việt Nam");
            }
        });
    }

    @Override
    public void onPause() { super.onPause(); mapProvider.onPause(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); mapProvider.onDestroy(); }
}