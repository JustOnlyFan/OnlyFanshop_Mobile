package com.example.onlyfanshop.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.ViewModel.MapViewModel;
import com.example.onlyfanshop.adapter.AttractionAdapter;
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

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final double DEFAULT_LAT_VN = 15.9266657;   // Trung tâm VN tương đối
    private static final double DEFAULT_LNG_VN = 107.9650855;
    private static final float DEFAULT_ZOOM_VN = 5.5f;         // Thấy toàn quốc

    private MapViewModel vm;
    private MapProvider mapProvider;

    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView tvLocation;
    private RecyclerView rvSuggestions, rvAttractions;
    private TextView tvRouteInfo;
    private LinearLayout routePanel;
    private FloatingActionButton fabLocation;

    private final List<PlaceSuggestion> currentSuggestions = new ArrayList<>();

    // New: Repository + MarkerManager + CarouselController
    private ShopRepository shopRepository;
    private ShopMarkerManager markerManager;
    private AttractionCarouselController carouselController;

    private List<Shop> shops = new ArrayList<>();
    private List<Attraction> attractions = new ArrayList<>();
    private AttractionAdapter attractionAdapter;
    private double[] routeStart = null;

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle savedInstanceState){
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

        tvLocation.setText("Việt Nam");
        KeyStorage.loadIntoConfig(requireContext());

        initMap(v);
        initData();
        initCarousel();
        bindViewModel();
        bindEvents();
    }

    private void initMap(View root){
        mapProvider = new OsmMapProvider();
        FrameLayout container = root.findViewById(R.id.mapContainer);
        View mapView = mapProvider.createMapView(requireContext());
        container.addView(mapView);

        markerManager = new ShopMarkerManager(mapProvider);

        // Camera mặc định Việt Nam
        mapProvider.moveCamera(DEFAULT_LAT_VN, DEFAULT_LNG_VN, DEFAULT_ZOOM_VN);

        mapProvider.setOnMapClickListener((lat, lng) -> rvSuggestions.setVisibility(View.GONE));
        mapProvider.setOnMapLongClickListener((lat, lng) -> {
            if (routeStart == null){
                routeStart = new double[]{lat, lng};
                mapProvider.addMarker("start", lat, lng, "Start", "");
                Toast.makeText(getContext(), "Đã chọn điểm bắt đầu", Toast.LENGTH_SHORT).show();
            } else {
                mapProvider.addMarker("end", lat, lng, "End", "");
                vm.route(routeStart[0], routeStart[1], lat, lng, MapConfig.ROUTE_MAX_ALTERNATIVES);
                routeStart = null;
            }
        });

        // Nếu MapProvider có onMarkerClick:
        // mapProvider.setOnMarkerClickListener(id -> {
        //     Shop s = markerManager.getShopForMarker(id);
        //     if (s != null) showShopDetail(s);
        // });
    }

    private void initData() {
        shopRepository = ShopRepository.getInstance();
        shops = shopRepository.getAllShops();
        attractions = ShopUiMapper.toAttractions(shops);
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
                if (routeStart != null) {
                    vm.route(routeStart[0], routeStart[1], a.getLatitude(), a.getLongitude(), MapConfig.ROUTE_MAX_ALTERNATIVES);
                } else {
                    Toast.makeText(getContext(), "Vui lòng chọn điểm bắt đầu trước", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        carouselController = new AttractionCarouselController(rvAttractions, lm, attractionAdapter);
        carouselController.attach(attractions, (a, pos) -> {
            // Tự động trỏ map đến item đang hiển thị ở giữa carousel
            Shop s = shopRepository.findById(a.getId());
            if (s != null) focusShop(s);
        });
    }

    private void focusShop(Shop s) {
        mapProvider.moveCamera(s.getLatitude(), s.getLongitude(), 15f);
        markerManager.showSelectedMarker(s);
    }

    private void bindViewModel(){
        vm.getGeocodeResults().observe(getViewLifecycleOwner(), results -> {
            if (results == null || results.isEmpty()) {
                Toast.makeText(getContext(), "Không tìm thấy", Toast.LENGTH_SHORT).show();
                return;
            }
            GeocodeResult r = results.get(0);
            mapProvider.addMarker("search", r.lat, r.lng, r.formattedAddress, "");
            mapProvider.moveCamera(r.lat, r.lng, 15);
        });

        vm.getSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions == null) return;
            currentSuggestions.clear();
            currentSuggestions.addAll(suggestions);
            // TODO: adapter cho gợi ý
            rvSuggestions.setVisibility(View.VISIBLE);
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
            if (err != null) Toast.makeText(getContext(), "Lỗi: " + err, Toast.LENGTH_SHORT).show();
        });
    }

    private void bindEvents(){
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if (s.length() > 2) {
                    vm.autoComplete(s.toString());
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    rvSuggestions.setVisibility(View.GONE);
                    btnClearSearch.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s){}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            rvSuggestions.setVisibility(View.GONE);
            btnClearSearch.setVisibility(View.GONE);
        });

        fabLocation.setOnClickListener(v -> {
            // TODO: Get current location and move map
            Toast.makeText(getContext(), "Đang lấy vị trí của bạn...", Toast.LENGTH_SHORT).show();
        });

        routePanel.findViewById(R.id.btnClearRoute).setOnClickListener(v -> {
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

    @Override public void onResume(){ super.onResume(); mapProvider.onResume(); }
    @Override public void onPause(){ super.onPause(); mapProvider.onPause(); }
    @Override public void onDestroyView(){ super.onDestroyView(); mapProvider.onDestroy(); }
}