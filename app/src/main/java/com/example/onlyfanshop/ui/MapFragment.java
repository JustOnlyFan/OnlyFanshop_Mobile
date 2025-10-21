// File: com.example.onlyfanshop.ui.MapFragment.java

// (Đã tổng hợp hoàn chỉnh, chỉ giữ những phần đã sửa/chỉnh/cần thiết, bạn có thể copy toàn bộ vào file của mình)

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final double DEFAULT_LAT_VN = 15.9266657;
    private static final double DEFAULT_LNG_VN = 107.9650855;
    private static final float DEFAULT_ZOOM_VN = 5.5f;

    private MapViewModel vm;
    private MapProvider mapProvider;

    private ImageButton btnZoomIn, btnZoomOut;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView tvLocation;
    private RecyclerView rvSuggestions, rvAttractions;
    private TextView tvRouteInfo;
    private LinearLayout routePanel;
    private FloatingActionButton fabLocation;

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
        tvRouteInfo = v.findViewById(R.id.tvRouteInfo);
        routePanel = v.findViewById(R.id.routePanel);
        fabLocation = v.findViewById(R.id.fabLocation);
        btnZoomIn = v.findViewById(R.id.btnZoomIn);
        btnZoomOut = v.findViewById(R.id.btnZoomOut);

        tvLocation.setText("Việt Nam");
        KeyStorage.loadIntoConfig(requireContext());

        initSuggestionAdapter();
        initMap(v);
        initData();
        initCarousel();
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
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
            Toast.makeText(getContext(), "Đã chọn điểm bắt đầu", Toast.LENGTH_SHORT).show();
            isGeocodingInProgress = false;
        });
    }

    private void initData() {
        shopRepository = ShopRepository.getInstance();
        shops = shopRepository.getAllShops();
        attractions = ShopUiMapper.toAttractions(shops);

        showShopPins(shops); // Hiển thị tất cả pin shop
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
                if (isGeocodingInProgress) {
                    Toast.makeText(getContext(), "Vui lòng chờ lấy địa điểm bắt đầu...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (routeStartLat == null || routeStartLng == null) {
                    Toast.makeText(getContext(), "Vui lòng chọn địa điểm bắt đầu", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("ROUTE", "Routing from: " + routeStartLat + "," + routeStartLng +
                        " to: " + a.getLatitude() + "," + a.getLongitude());
                vm.route(routeStartLat, routeStartLng, a.getLatitude(), a.getLongitude(), MapConfig.ROUTE_MAX_ALTERNATIVES);
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        carouselController = new AttractionCarouselController(rvAttractions, lm, attractionAdapter);
        carouselController.attach(attractions, (a, pos) -> {
            Shop s = shopRepository.findById(a.getId());
            if (s != null) focusShop(s);
        });
    }

    private void focusShop(Shop s) {
        mapProvider.moveCamera(s.getLatitude(), s.getLongitude(), 15f);
        markerManager.showSelectedMarker(s);
    }

    private void bindViewModel() {
        vm.getGeocodeResults().observe(getViewLifecycleOwner(), results -> {
            isGeocodingInProgress = false;
            if (results == null || results.isEmpty()) {
                Toast.makeText(getContext(), "Không tìm thấy", Toast.LENGTH_SHORT).show();
                return;
            }
            GeocodeResult r = results.get(0);
            etSearch.setText(r.formattedAddress); // Hiện địa chỉ lên thanh search
            showStartPin(r.lat, r.lng, r.formattedAddress);
            mapProvider.moveCamera(r.lat, r.lng, 15);

            routeStartLat = r.lat;
            routeStartLng = r.lng;
            routeStartAddress = r.formattedAddress;
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
            mapProvider.addPolyline("route_main", main.path, 0xFF0066FF, 8f);
            tvRouteInfo.setText(String.format("Dist: %.1f km | Time: %.1f min",
                    main.distanceMeters / 1000.0, main.durationSeconds / 60.0));
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
            isGeocodingInProgress = false;

            mapProvider.removeMarker("start"); // XÓA marker bắt đầu, shop pins vẫn giữ nguyên
        });

        fabLocation.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    // Reverse geocode lấy địa chỉ
                    vm.reverseGeocode(lat, lng); // MapViewModel phải có hàm này
                } else {
                    Toast.makeText(getContext(), "Không thể lấy vị trí. Hãy thử lại!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        routePanel.findViewById(R.id.btnClearRoute).setOnClickListener(view -> {
            mapProvider.clearPolyline("route_main");
            mapProvider.removeMarker("start");
            mapProvider.removeMarker("end");
            tvRouteInfo.setText("No route selected");
            routePanel.setVisibility(View.GONE);
        });
    }

    private void showShopDetail(Shop shop) {
        ShopDetailBottomSheet.newInstance(shop)
                .show(getParentFragmentManager(), "shop_detail");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fabLocation.performClick();
            } else {
                Toast.makeText(getContext(), "Không có quyền truy cập vị trí!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() { super.onResume(); mapProvider.onResume(); }
    @Override
    public void onPause() { super.onPause(); mapProvider.onPause(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); mapProvider.onDestroy(); }
}