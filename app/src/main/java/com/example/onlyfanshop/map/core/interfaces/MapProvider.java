package com.example.onlyfanshop.map.core.interfaces;

import android.content.Context;
import android.view.View;

import java.util.List;

public interface MapProvider {
    View createMapView(Context context);
    void onResume();
    void onPause();
    void onDestroy();

    void moveCamera(double lat, double lng, double zoom);
    void addMarker(String id, double lat, double lng, String title, String snippet);
    void addMarker(String id, double lat, double lng, String title, String snippet, android.graphics.drawable.Drawable icon);
    void updateMarker(String id, double lat, double lng);
    void removeMarker(String id);
    void addPolyline(String id, List<double[]> latLngs, int color, float width);
    void clearPolyline(String id);
    void addPolygon(String id, List<double[]> latLngs, int strokeColor, int fillColor, float strokeWidth);

    void setOnMapLongClickListener(OnMapLongClickListener l);
    void setOnMapClickListener(OnMapClickListener l);

    interface OnMapLongClickListener {
        void onLongClick(double lat, double lng);
    }
    interface OnMapClickListener {
        void onClick(double lat, double lng);
    }
    float getZoomLevel();
    double getCenterLat();
    double getCenterLng();
}