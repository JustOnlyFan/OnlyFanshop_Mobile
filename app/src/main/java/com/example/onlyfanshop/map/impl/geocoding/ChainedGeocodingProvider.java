package com.example.onlyfanshop.map.impl.geocoding;

import com.example.onlyfanshop.map.core.interfaces.GeocodingProvider;
import com.example.onlyfanshop.map.models.GeocodeResult;

import java.util.Arrays;
import java.util.List;

public class ChainedGeocodingProvider implements GeocodingProvider {

    private final List<GeocodingProvider> providers;

    public ChainedGeocodingProvider(GeocodingProvider... p) {
        providers = Arrays.asList(p);
    }

    @Override
    public void geocode(String q, Callback cb) {
        callGeocodeRecursive(0, q, cb);
    }

    private void callGeocodeRecursive(int idx, String q, Callback cb) {
        if (idx >= providers.size()) {
            cb.onSuccess(new java.util.ArrayList<>());
            return;
        }
        providers.get(idx).geocode(q, new Callback() {
            @Override
            public void onSuccess(List<GeocodeResult> results) {
                if (results.isEmpty()) {
                    callGeocodeRecursive(idx + 1, q, cb);
                } else {
                    cb.onSuccess(results);
                }
            }
            @Override
            public void onError(Throwable t) {
                callGeocodeRecursive(idx + 1, q, cb);
            }
        });
    }

    @Override
    public void reverseGeocode(double lat, double lng, Callback cb) {
        callReverseGeocodeRecursive(0, lat, lng, cb);
    }

    private void callReverseGeocodeRecursive(int idx, double lat, double lng, Callback cb) {
        if (idx >= providers.size()) {
            cb.onSuccess(new java.util.ArrayList<>());
            return;
        }
        providers.get(idx).reverseGeocode(lat, lng, new Callback() {
            @Override
            public void onSuccess(List<GeocodeResult> results) {
                if (results.isEmpty()) {
                    callReverseGeocodeRecursive(idx + 1, lat, lng, cb);
                } else {
                    cb.onSuccess(results);
                }
            }
            @Override
            public void onError(Throwable t) {
                callReverseGeocodeRecursive(idx + 1, lat, lng, cb);
            }
        });
    }

    @Override
    public void reverse(double lat, double lng, Callback cb) {
        callReverseRecursive(0, lat, lng, cb);
    }

    private void callReverseRecursive(int idx, double lat, double lng, Callback cb) {
        if (idx >= providers.size()) {
            cb.onSuccess(new java.util.ArrayList<>());
            return;
        }
        providers.get(idx).reverse(lat, lng, new Callback() {
            @Override
            public void onSuccess(List<GeocodeResult> results) {
                if (results.isEmpty()) {
                    callReverseRecursive(idx + 1, lat, lng, cb);
                } else {
                    cb.onSuccess(results);
                }
            }
            @Override
            public void onError(Throwable t) {
                callReverseRecursive(idx + 1, lat, lng, cb);
            }
        });
    }

}