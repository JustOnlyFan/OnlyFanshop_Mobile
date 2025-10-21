package com.example.onlyfanshop.map.core.interfaces;

import com.example.onlyfanshop.map.models.GeocodeResult;

import java.util.List;

public interface GeocodingProvider {
    void geocode(String q, Callback cb);
    void reverse(double lat, double lng, Callback cb);
    void reverseGeocode(double lat, double lng, Callback cb); // thêm dòng này

    interface Callback {
        void onSuccess(List<GeocodeResult> results);
        void onError(Throwable t);
    }
}