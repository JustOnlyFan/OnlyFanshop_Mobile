package com.example.onlyfanshop.map.impl.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.onlyfanshop.map.core.interfaces.MapProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmMapProvider implements MapProvider {

    private MapView mapView;
    private final Map<String, Marker> markerMap = new HashMap<>();
    private final Map<String, Polyline> polylineMap = new HashMap<>();
    private final Map<String, Polygon> polygonMap = new HashMap<>();

    private OnMapClickListener clickListener;
    private OnMapLongClickListener longClickListener;

    @Override
    public View createMapView(Context context) {
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        mapView = new MapView(context);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (clickListener != null) clickListener.onClick(p.getLatitude(), p.getLongitude());
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (longClickListener != null) longClickListener.onLongClick(p.getLatitude(), p.getLongitude());
                return false;
            }
        });
        mapView.getOverlays().add(eventsOverlay);

        return mapView;
    }

    @Override public void onResume() { if (mapView != null) mapView.onResume(); }
    @Override public void onPause()  { if (mapView != null) mapView.onPause(); }
    @Override public void onDestroy(){ }

    @Override
    public void moveCamera(double lat, double lng, double zoom) {
        if (mapView == null) return;
        mapView.getController().setZoom(zoom);
        mapView.getController().setCenter(new GeoPoint(lat, lng));
    }

    // Thêm hàm addMarker nhận icon riêng
    public void addMarker(String id, double lat, double lng, String title, String snippet, Drawable icon) {
        if (mapView == null) return;
        Marker m = markerMap.get(id);
        if (m == null) {
            m = new Marker(mapView);
            markerMap.put(id, m);
            mapView.getOverlays().add(m);
        }
        m.setPosition(new GeoPoint(lat, lng));
        m.setTitle(title);
        m.setSnippet(snippet);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if (icon != null) m.setIcon(icon);
        mapView.invalidate();
    }

    // Giữ hàm cũ cho shop marker (icon mặc định)
    @Override
    public void addMarker(String id, double lat, double lng, String title, String snippet) {
        addMarker(id, lat, lng, title, snippet, null);
    }

    @Override
    public void updateMarker(String id, double lat, double lng) {
        Marker m = markerMap.get(id);
        if (m != null) {
            m.setPosition(new GeoPoint(lat, lng));
            mapView.invalidate();
        }
    }

    @Override
    public void removeMarker(String id) {
        Marker m = markerMap.remove(id);
        if (m != null) {
            mapView.getOverlays().remove(m);
            mapView.invalidate();
        }
    }

    @Override
    public void addPolyline(String id, List<double[]> latLngs, int color, float width) {
        if (mapView == null) return;
        Polyline pl = polylineMap.get(id);
        if (pl != null) {
            mapView.getOverlays().remove(pl);
        }
        pl = new Polyline();
        for (double[] d : latLngs) pl.addPoint(new GeoPoint(d[0], d[1]));
        pl.setColor(color);
        pl.setWidth(width);
        polylineMap.put(id, pl);
        mapView.getOverlays().add(pl);
        mapView.invalidate();
    }

    @Override
    public void clearPolyline(String id) {
        Polyline pl = polylineMap.remove(id);
        if (pl != null) {
            mapView.getOverlays().remove(pl);
            mapView.invalidate();
        }
    }

    @Override
    public void addPolygon(String id, List<double[]> latLngs, int strokeColor, int fillColor, float strokeWidth) {
        if (mapView == null) return;
        Polygon polygon = polygonMap.get(id);
        if (polygon != null) {
            mapView.getOverlays().remove(polygon);
        }
        polygon = new Polygon();
        for (double[] d : latLngs) polygon.addPoint(new GeoPoint(d[0], d[1]));
        polygon.setStrokeColor(strokeColor);
        polygon.setFillColor(fillColor);
        polygon.setStrokeWidth(strokeWidth);
        polygonMap.put(id, polygon);
        mapView.getOverlays().add(polygon);
        mapView.invalidate();
    }

    @Override
    public void setOnMapLongClickListener(OnMapLongClickListener l) {
        this.longClickListener = l;
    }

    @Override
    public void setOnMapClickListener(OnMapClickListener l) {
        this.clickListener = l;
    }

    @Override
    public float getZoomLevel() {
        if (mapView != null) {
            return (float) mapView.getZoomLevelDouble();
        }
        return 0f;
    }

    @Override
    public double getCenterLat() {
        if (mapView != null) {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            return center.getLatitude();
        }
        return 0d;
    }

    @Override
    public double getCenterLng() {
        if (mapView != null) {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            return center.getLongitude();
        }
        return 0d;
    }
}