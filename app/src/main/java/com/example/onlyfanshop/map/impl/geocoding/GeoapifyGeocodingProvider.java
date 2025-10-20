package com.example.onlyfanshop.map.impl.geocoding;

import com.example.onlyfanshop.map.config.MapConfig;
import com.example.onlyfanshop.map.core.interfaces.GeocodingProvider;
import com.example.onlyfanshop.map.models.GeocodeResult;
import com.example.onlyfanshop.map.util.HttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeoapifyGeocodingProvider implements GeocodingProvider {

    private static final String GEOCODE = "https://api.geoapify.com/v1/geocode/search";
    private static final String REVERSE = "https://api.geoapify.com/v1/geocode/reverse";

    @Override
    public void geocode(String q, Callback cb) {
        String url = GEOCODE + "?text=" + HttpClient.urlEncode(q) + "&limit=5&format=json&apiKey=" + MapConfig.GEOAPIFY_API_KEY;
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject o = new JSONObject(body);
                    JSONArray arr = o.getJSONArray("results");
                    List<GeocodeResult> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject r = arr.getJSONObject(i);
                        GeocodeResult gr = new GeocodeResult();
                        gr.lat = r.getDouble("lat");
                        gr.lng = r.getDouble("lon");
                        gr.formattedAddress = r.optString("formatted");
                        list.add(gr);
                    }
                    cb.onSuccess(list);
                } catch (Exception e){ cb.onError(e); }
            }
            @Override
            public void onError(Exception e){ cb.onError(e); }
        });
    }

    @Override
    public void reverse(double lat, double lng, Callback cb) {
        String url = REVERSE + "?lat=" + lat + "&lon=" + lng + "&limit=1&format=json&apiKey=" + MapConfig.GEOAPIFY_API_KEY;
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject o = new JSONObject(body);
                    JSONArray arr = o.getJSONArray("results");
                    List<GeocodeResult> list = new ArrayList<>();
                    if (arr.length() > 0) {
                        JSONObject r = arr.getJSONObject(0);
                        GeocodeResult gr = new GeocodeResult();
                        gr.lat = r.getDouble("lat");
                        gr.lng = r.getDouble("lon");
                        gr.formattedAddress = r.optString("formatted");
                        list.add(gr);
                    }
                    cb.onSuccess(list);
                } catch (Exception e){ cb.onError(e); }
            }
            @Override
            public void onError(Exception e){ cb.onError(e); }
        });
    }
    @Override
    public void reverseGeocode(double lat, double lng, Callback cb) {
        // Gọi API reverse geocode hoặc đơn giản gọi lại reverse nếu logic giống nhau
        reverse(lat, lng, cb);
    }
}