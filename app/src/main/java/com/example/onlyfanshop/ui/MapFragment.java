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
import com.example.onlyfanshop.model.Attraction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

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
    private final List<Attraction> attractions = new ArrayList<>();
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

        // Load key đã lưu
        KeyStorage.loadIntoConfig(requireContext());

        initMap(v);
        initAttractions();
        bindViewModel();
        bindEvents();
    }

    private void initMap(View root){
        mapProvider = new OsmMapProvider();
        FrameLayout container = root.findViewById(R.id.mapContainer);
        View mapView = mapProvider.createMapView(requireContext());
        container.addView(mapView);
        mapProvider.moveCamera(10.762622, 106.660172, 12);

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
    }

    private void initAttractions() {
        // Initialize attractions with sample data
        attractions.clear();
        attractions.add(new Attraction(
            "1", 
            "Miniatur Wunderland", 
            "Tiny trains travel through a world of wonder.",
            "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400",
            53.5456, 9.9936, 
            "Kehrwieder 2, 20457 Hamburg, Germany"
        ));
        attractions.add(new Attraction(
            "2", 
            "Dungeon Hamburg", 
            "Dive in history",
            "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400",
            53.5456, 9.9936, 
            "Kehrwieder 2, 20457 Hamburg, Germany"
        ));
        attractions.add(new Attraction(
            "3", 
            "Elbphilharmonie", 
            "Architectural masterpiece by the water",
            "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400",
            53.5456, 9.9936, 
            "Platz der Deutschen Einheit 1, 20457 Hamburg, Germany"
        ));

        // Setup attractions RecyclerView
        attractionAdapter = new AttractionAdapter(attractions, new AttractionAdapter.OnAttractionClickListener() {
            @Override
            public void onAttractionClick(Attraction attraction) {
                // Move map to attraction location
                mapProvider.moveCamera(attraction.getLatitude(), attraction.getLongitude(), 15);
                mapProvider.addMarker("attraction", attraction.getLatitude(), attraction.getLongitude(), 
                    attraction.getTitle(), attraction.getDescription());
            }

            @Override
            public void onDirectionsClick(Attraction attraction) {
                // Start route to attraction
                if (routeStart != null) {
                    vm.route(routeStart[0], routeStart[1], attraction.getLatitude(), attraction.getLongitude(), 
                        MapConfig.ROUTE_MAX_ALTERNATIVES);
                } else {
                    Toast.makeText(getContext(), "Please select a starting point first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rvAttractions.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvAttractions.setAdapter(attractionAdapter);
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
            // TODO: Setup suggestions RecyclerView adapter
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
            Toast.makeText(getContext(), "Getting your location...", Toast.LENGTH_SHORT).show();
        });

        // Route panel clear button
        routePanel.findViewById(R.id.btnClearRoute).setOnClickListener(v -> {
            mapProvider.clearPolyline("route_main");
            mapProvider.removeMarker("start");
            mapProvider.removeMarker("end");
            tvRouteInfo.setText("No route selected");
            routePanel.setVisibility(View.GONE);
        });
    }

    @Override public void onResume(){ super.onResume(); mapProvider.onResume(); }
    @Override public void onPause(){ super.onPause(); mapProvider.onPause(); }
    @Override public void onDestroyView(){ super.onDestroyView(); mapProvider.onDestroy(); }
}