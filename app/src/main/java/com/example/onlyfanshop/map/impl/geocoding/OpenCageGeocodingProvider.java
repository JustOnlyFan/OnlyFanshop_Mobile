package com.example.onlyfanshop.map.impl.geocoding;

import com.example.onlyfanshop.map.config.MapConfig;
import com.example.onlyfanshop.map.core.interfaces.GeocodingProvider;
import com.example.onlyfanshop.map.models.GeocodeResult;
import com.example.onlyfanshop.map.util.HttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OpenCageGeocodingProvider implements GeocodingProvider {

    private static final String BASE = "https://api.opencagedata.com/geocode/v1/json";

    @Override
    public void geocode(String q, Callback cb) {
        String url = BASE + "?q=" + HttpClient.urlEncode(q) + "&key=" + MapConfig.OPEN_CAGE_API_KEY + "&limit=5&language=en";
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject o = new JSONObject(body);
                    JSONArray arr = o.getJSONArray("results");
                    List<GeocodeResult> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject r = arr.getJSONObject(i);
                        JSONObject g = r.getJSONObject("geometry");
                        GeocodeResult gr = new GeocodeResult();
                        gr.lat = g.getDouble("lat");
                        gr.lng = g.getDouble("lng");
                        gr.formattedAddress = r.optString("formatted");
                        list.add(gr);
                    }
                    cb.onSuccess(list);
                } catch (Exception e) { cb.onError(e); }
            }
            @Override
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    @Override
    public void reverse(double lat, double lng, Callback cb) {
        String url = BASE + "?q=" + lat + "+" + lng + "&key=" + MapConfig.OPEN_CAGE_API_KEY + "&limit=1";
        HttpClient.get(url, new HttpClient.ResponseCallback() {
            @Override
            public void onSuccess(String body) {
                try {
                    JSONObject o = new JSONObject(body);
                    JSONArray arr = o.getJSONArray("results");
                    List<GeocodeResult> list = new ArrayList<>();
                    if (arr.length() > 0) {
                        JSONObject r = arr.getJSONObject(0);
                        JSONObject g = r.getJSONObject("geometry");
                        GeocodeResult gr = new GeocodeResult();
                        gr.lat = g.getDouble("lat");
                        gr.lng = g.getDouble("lng");
                        gr.formattedAddress = r.optString("formatted");
                        list.add(gr);
                    }
                    cb.onSuccess(list);
                } catch (Exception e) { cb.onError(e); }
            }
            @Override
            public void onError(Exception e) { cb.onError(e); }
        });
    }
    @Override
    public void reverseGeocode(double lat, double lng, Callback cb) {
        // Gọi API reverse geocode hoặc đơn giản gọi lại reverse nếu logic giống nhau
        reverse(lat, lng, cb);
    }

}