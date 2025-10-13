package com.example.onlyfanshop.map.shop;

import androidx.annotation.Nullable;

import com.example.onlyfanshop.map.core.interfaces.MapProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý marker cửa hàng trên bản đồ và marker đang được chọn.
 */
public class ShopMarkerManager {

    private final MapProvider mapProvider;
    private final Map<String, Shop> markerToShop = new HashMap<>();
    private static final String SELECTED_MARKER_ID = "selected_shop";

    public ShopMarkerManager(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    public void showSelectedMarker(Shop shop) {
        if (shop == null) return;
        // Xoá marker đang chọn cũ
        try { mapProvider.removeMarker(SELECTED_MARKER_ID); } catch (Exception ignore) {}
        // Thêm marker chọn mới
        mapProvider.addMarker(
                SELECTED_MARKER_ID,
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getName(),
                shop.getDescription()
        );
        markerToShop.put(SELECTED_MARKER_ID, shop);
    }

    public String addMarkerForShop(Shop shop) {
        if (shop == null) return null;
        String id = "shop_" + shop.getId();
        mapProvider.addMarker(
                id,
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getName(),
                shop.getDescription()
        );
        markerToShop.put(id, shop);
        return id;
    }

    @Nullable
    public Shop getShopForMarker(String markerId) {
        return markerToShop.get(markerId);
    }

    public void clear() {
        // Tuỳ MapProvider có API clearAll hay không; ở đây chỉ xoá id đã biết
        try { mapProvider.removeMarker(SELECTED_MARKER_ID); } catch (Exception ignore) {}
        markerToShop.clear();
    }
}