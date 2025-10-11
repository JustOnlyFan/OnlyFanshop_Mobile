package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.ProductDetailDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    // Kiểm tra kết nối (nếu backend có)
    @GET("product/test")
    Call<ApiResponse<String>> testConnection();

    // Homepage (POST) theo swagger bạn gửi
    @POST("product/public/homepage")
    Call<ApiResponse<HomePageData>> getHomePagePost(
            @Query("page") int page,
            @Query("size") int size,
            @Query("sortBy") String sortBy,
            @Query("order") String order,
            @Query("keyword") String keyword,
            @Query("categoryId") Integer categoryId,
            @Query("brandId") Integer brandId
    );


    @GET("product/public/detail/{productId}")
    Call<ApiResponse<ProductDetailDTO>> getProductDetail(@Path("productId") int productId);


}