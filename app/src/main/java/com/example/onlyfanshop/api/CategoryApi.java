package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.CategoryManagementDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CategoryApi {
    @GET("category/public")
    Call<List<CategoryManagementDTO>> getAllCategories();

    @DELETE("category/delete/{id}")
    Call<ApiResponse<Void>> deleteCategory(@Path("id") int id);

    @PUT("category/{id}")
    Call<CategoryManagementDTO> updateCategory(@Path("id") int id, @Body CategoryManagementDTO category);
    @PUT("category/switchActive/{id}")
    Call<CategoryManagementDTO> toggleActive(@Path("id") int id, @Query("active") boolean active);
    @POST("category/create")
    Call<CategoryManagementDTO> createCategory(@Body CategoryManagementDTO category);
}
