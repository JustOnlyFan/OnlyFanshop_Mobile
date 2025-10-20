package com.example.onlyfanshop.map.config;

public class MapConfig {

    // Các KEY mặc định (placeholder). Sẽ bị override bởi KeyStorage (SharedPreferences).
    public static String OPEN_CAGE_API_KEY = "7084707d97154c21b96e2a9ff50bfa1a";
    public static String GEOAPIFY_API_KEY = "0d9adf8a2e964c238dd91bbf4301d406";
    public static String LOCATION_IQ_API_KEY = "pk.6a63f388fbd716914de899f77dfb04c6";
    public static String OPEN_ROUTE_SERVICE_KEY = "5b3ce3597851110001cf6248a2ea1fc91d99410eab47d250d75bcb0b";
    public static String GRAPH_HOPPER_KEY = "5a6d1ed0-cb46-42e4-b281-21b43a06a485";

    public enum GeocodingPrimary { OPENCAGE, GEOAPIFY, LOCATIONIQ, CHAINED }
    public enum RoutingPrimary { OPENROUTESERVICE, GRAPHHOPPER, CHAINED }
    public enum AutocompletePrimary {
        GEOAPIFY,
        LOCATIONIQ,
        CHAINED
    }
    public static AutocompletePrimary AUTOCOMPLETE_PRIMARY = AutocompletePrimary.CHAINED;

    public static GeocodingPrimary GEOCODING_PRIMARY = GeocodingPrimary.CHAINED;
    // MapConfig.java
    public static final RoutingPrimary ROUTING_PRIMARY = RoutingPrimary.CHAINED;

    public static int ROUTE_MAX_ALTERNATIVES = 1;
}