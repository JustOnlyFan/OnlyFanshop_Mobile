package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.StoreLocation;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface StoreLocationApi {
    
    @GET("store-locations")
    Call<ApiResponse<List<StoreLocation>>> getAllStoreLocations();
    
    @GET("store-locations/{id}")
    Call<ApiResponse<StoreLocation>> getStoreLocationById(@Path("id") Integer id);
    
    @POST("store-locations")
    Call<ApiResponse<StoreLocation>> createStoreLocation(@Body StoreLocation storeLocation);
    
    @PUT("store-locations/{id}")
    Call<ApiResponse<StoreLocation>> updateStoreLocation(@Path("id") Integer id, @Body StoreLocation storeLocation);
    
    @DELETE("store-locations/{id}")
    Call<ApiResponse<Void>> deleteStoreLocation(@Path("id") Integer id);
}



