package com.example.onlyfanshop.map.impl.geocoding;

import com.example.onlyfanshop.map.config.MapConfig;
import com.example.onlyfanshop.map.core.interfaces.GeocodingProvider;
import com.example.onlyfanshop.map.models.GeocodeResult;
import com.example.onlyfanshop.map.util.HttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocationIQGeocodingProvider implements GeocodingProvider {

    private static final String BASE = "https://us1.locationiq.com/v1";

    @Override
    public void geocode(String q, Callback cb) {
        String url = BASE + "/search.php?key=" + MapConfig.LOCATION_IQ_API_KEY +
                "&q=" + HttpClient.urlEncode(q) + "&format=json&limit=5";
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONArray arr = new JSONArray(body);
                    List<GeocodeResult> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject r = arr.getJSONObject(i);
                        GeocodeResult gr = new GeocodeResult();
                        gr.lat = r.getDouble("lat");
                        gr.lng = r.getDouble("lon");
                        gr.formattedAddress = r.optString("display_name");
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
        String url = BASE + "/reverse.php?key=" + MapConfig.LOCATION_IQ_API_KEY +
                "&lat=" + lat + "&lon=" + lng + "&format=json";
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject obj = new JSONObject(body);
                    List<GeocodeResult> list = new ArrayList<>();
                    GeocodeResult gr = new GeocodeResult();
                    gr.lat = lat; gr.lng = lng;
                    gr.formattedAddress = obj.optString("display_name");
                    list.add(gr);
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