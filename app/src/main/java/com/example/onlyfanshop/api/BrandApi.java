package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.BrandManagementDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BrandApi {
    @PUT("brands/{id}")
    Call<BrandDTO> updateBrand(@Path("id") int id, @Body BrandManagementDTO brand);

    @PUT("brands/switchActive/{id}")
    Call<BrandDTO> toggleActive(@Path("id") int id, @Query("active") boolean active);

    @GET("brands/public")
    Call<List<BrandManagementDTO>> getAllBrands();

    @POST("brands/create")
    Call<BrandManagementDTO> createBrand(@Body BrandManagementDTO brand);


}
