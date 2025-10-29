package com.example.onlyfanshop.map.shop;

import android.util.Log;

import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.StoreLocationApi;
import com.example.onlyfanshop.model.StoreLocation;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Load stores từ database thay vì hardcoded data
 */
public class DatabaseShopDataSource implements ShopDataSource {

    private final List<Shop> data = new ArrayList<>();
    private boolean isLoaded = false;
    private OnDataLoadedListener listener;

    public interface OnDataLoadedListener {
        void onDataLoaded(List<Shop> shops);
        void onDataLoadFailed(String error);
    }

    public DatabaseShopDataSource() {
        // Don't seed hardcoded data
    }

    /**
     * Load stores từ database API
     */
    public void loadFromDatabase(OnDataLoadedListener listener) {
        this.listener = listener;
        
        StoreLocationApi api = ApiClient.getPublicClient().create(StoreLocationApi.class);
        
        api.getAllStoreLocations().enqueue(new Callback<ApiResponse<List<StoreLocation>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<StoreLocation>>> call, Response<ApiResponse<List<StoreLocation>>> response) {
                Log.d("DatabaseShopDataSource", "Response code: " + response.code());
                Log.d("DatabaseShopDataSource", "Response successful: " + response.isSuccessful());
                
                if (response.isSuccessful()) {
                    ApiResponse<List<StoreLocation>> body = response.body();
                    Log.d("DatabaseShopDataSource", "Response body: " + (body != null ? "not null" : "null"));
                    
                    if (body != null) {
                        Log.d("DatabaseShopDataSource", "Response data: " + (body.getData() != null ? "not null" : "null"));
                        Log.d("DatabaseShopDataSource", "Response message: " + body.getMessage());
                        
                        if (body.getData() != null) {
                            List<StoreLocation> storeLocations = body.getData();
                            
                            Log.d("DatabaseShopDataSource", "Loaded " + storeLocations.size() + " stores from API");
                            
                            // Clear old data
                            data.clear();
                            
                            // Convert StoreLocation to Shop
                            for (StoreLocation location : storeLocations) {
                                Shop shop = new Shop(
                                    String.valueOf(location.getLocationID()),
                                    location.getName() != null ? location.getName() : "Store",
                                    location.getDescription() != null ? location.getDescription() : "",
                                    location.getImageUrl() != null ? location.getImageUrl() : "",
                                    location.getLatitude() != null ? location.getLatitude() : 0.0,
                                    location.getLongitude() != null ? location.getLongitude() : 0.0,
                                    location.getAddress() != null ? location.getAddress() : "",
                                    location.getPhone() != null ? location.getPhone() : "",
                                    location.getOpeningHours() != null ? location.getOpeningHours() : ""
                                );
                                data.add(shop);
                                Log.d("DatabaseShopDataSource", "Added: " + shop.getName() + " at " + shop.getLatitude() + "," + shop.getLongitude());
                            }
                            
                            isLoaded = true;
                            
                            // Notify listener even if empty
                            if (DatabaseShopDataSource.this.listener != null) {
                                DatabaseShopDataSource.this.listener.onDataLoaded(new ArrayList<>(data));
                            }
                        } else {
                            // Data is null but response is successful - database is empty
                            Log.w("DatabaseShopDataSource", "Response data is null - database may be empty");
                            isLoaded = true;
                            data.clear();
                            
                            if (DatabaseShopDataSource.this.listener != null) {
                                DatabaseShopDataSource.this.listener.onDataLoaded(new ArrayList<>());
                            }
                        }
                    } else {
                        String error = "Response body is null";
                        Log.e("DatabaseShopDataSource", error);
                        if (DatabaseShopDataSource.this.listener != null) {
                            DatabaseShopDataSource.this.listener.onDataLoadFailed(error);
                        }
                    }
                } else {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    Log.e("DatabaseShopDataSource", error);
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("DatabaseShopDataSource", "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e("DatabaseShopDataSource", "Cannot read error body", e);
                    }
                    
                    if (DatabaseShopDataSource.this.listener != null) {
                        DatabaseShopDataSource.this.listener.onDataLoadFailed(error);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<StoreLocation>>> call, Throwable t) {
                String error = "Error: " + t.getMessage();
                Log.e("DatabaseShopDataSource", error, t);
                if (DatabaseShopDataSource.this.listener != null) {
                    DatabaseShopDataSource.this.listener.onDataLoadFailed(error);
                }
            }
        });
    }

    @Override 
    public List<Shop> getAll() { 
        return new ArrayList<>(data); 
    }
    
    @Override 
    public void add(Shop shop) { 
        data.add(shop); 
    }
    
    @Override 
    public void addAll(List<Shop> list) { 
        data.addAll(list); 
    }

    @Override 
    public boolean update(Shop shop) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(shop.getId())) {
                data.set(i, shop);
                return true;
            }
        }
        return false;
    }

    @Override 
    public boolean remove(String id) {
        return data.removeIf(s -> s.getId().equals(id));
    }

    @Override 
    public Shop findById(String id) {
        for (Shop s : data) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}

