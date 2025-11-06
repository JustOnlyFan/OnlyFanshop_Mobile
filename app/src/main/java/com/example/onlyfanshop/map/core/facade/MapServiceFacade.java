package com.example.onlyfanshop.map.core.facade;

import com.example.onlyfanshop.map.config.MapConfig;
import com.example.onlyfanshop.map.core.interfaces.AutocompleteProvider;
import com.example.onlyfanshop.map.core.interfaces.GeocodingProvider;
import com.example.onlyfanshop.map.core.interfaces.RoutingProvider;
import com.example.onlyfanshop.map.impl.autocomplete.GeoapifyAutocompleteProvider;
import com.example.onlyfanshop.map.impl.autocomplete.LocationIQAutocompleteProvider;
import com.example.onlyfanshop.map.impl.geocoding.ChainedGeocodingProvider;
import com.example.onlyfanshop.map.impl.geocoding.GeoapifyGeocodingProvider;
import com.example.onlyfanshop.map.impl.geocoding.LocationIQGeocodingProvider;
import com.example.onlyfanshop.map.impl.geocoding.OpenCageGeocodingProvider;
import com.example.onlyfanshop.map.impl.routing.ChainedRoutingProvider;
import com.example.onlyfanshop.map.impl.routing.GraphHopperRoutingProvider;
import com.example.onlyfanshop.map.impl.routing.OpenRouteServiceRoutingProvider;

public class MapServiceFacade {

    private final GeocodingProvider geocoding;
    private final RoutingProvider routing;
    private final AutocompleteProvider autocomplete;

    public MapServiceFacade() {
        geocoding = buildGeocoding();
        routing = buildRouting();
        autocomplete = buildAutocomplete();
    }

    private GeocodingProvider buildGeocoding() {
        switch (MapConfig.GEOCODING_PRIMARY) {
            case OPENCAGE: return new OpenCageGeocodingProvider();
            case GEOAPIFY: return new GeoapifyGeocodingProvider();
            case LOCATIONIQ: return new LocationIQGeocodingProvider();
            case CHAINED:
            default:
                return new ChainedGeocodingProvider(
                        new OpenCageGeocodingProvider(),
                        new GeoapifyGeocodingProvider(),
                        new LocationIQGeocodingProvider()
                );
        }
    }

    private RoutingProvider buildRouting() {
        switch (MapConfig.ROUTING_PRIMARY) {
            case GRAPHHOPPER: return new GraphHopperRoutingProvider();
            case OPENROUTESERVICE: return new OpenRouteServiceRoutingProvider();
            case CHAINED:
            default:
                // Ưu tiên OpenRouteService trước, fallback sang GraphHopper
                return new ChainedRoutingProvider(
                        new OpenRouteServiceRoutingProvider(),
                        new GraphHopperRoutingProvider()
                );
        }
    }

    private AutocompleteProvider buildAutocomplete() {
        switch (MapConfig.AUTOCOMPLETE_PRIMARY) {
            case LOCATIONIQ: return new LocationIQAutocompleteProvider();
            case GEOAPIFY:
            default: return new GeoapifyAutocompleteProvider();
        }
    }

    public GeocodingProvider geocoding() { return geocoding; }
    public RoutingProvider routing() { return routing; }
    public AutocompleteProvider autocomplete() { return autocomplete; }
}