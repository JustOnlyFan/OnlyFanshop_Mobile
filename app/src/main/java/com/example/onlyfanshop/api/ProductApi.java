package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.BrandDTO;
import com.example.onlyfanshop.model.CategoryDTO;
import com.example.onlyfanshop.model.ProductDTO;
import com.example.onlyfanshop.model.ProductDetailDTO;
import com.example.onlyfanshop.model.Request.ProductRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.HomePageData;


import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("product/public/test")
    Call<ApiResponse<String>> testConnection();


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

    @GET("brands/public")
    Call<List<BrandDTO>> getAllBrands();

    @GET("category/public")
    Call<List<CategoryDTO>> getAllCategories();
    @Multipart
    @POST("/api/upload/image")
    Call<ApiResponse<String>> uploadImageToFirebase(
            @Part MultipartBody.Part file
    );

    @POST("product")
    Call<ApiResponse<ProductDTO>> addProduct(
            @Body ProductRequest product
    );

}