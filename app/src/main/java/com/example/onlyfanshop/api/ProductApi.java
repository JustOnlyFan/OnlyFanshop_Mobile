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
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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
    @PUT("product/{id}")
    Call<ProductDetailDTO> updateProduct(@Path("id") int id, @Body ProductRequest request);

    @Multipart
    @POST("/api/upload/change")
    Call<ApiResponse<String>> changeImage(
            @Part MultipartBody.Part file,
            @Part("oldUrl") RequestBody oldUrl
    );

    @PUT("product/image/{id}")
    Call<ApiResponse<Void>> updateImage(
            @Path("id") int id,
            @Query("imgString") String imgString
    );
    @POST("product/productList")
    Call<ApiResponse<HomePageData>> getProductList(
            @Query("page") int page,
            @Query("size") int size,
            @Query("sortBy") String sortBy,
            @Query("order") String order,
            @Query("keyword") String keyword,
            @Query("categoryId") Integer categoryId,
            @Query("brandId") Integer brandId
    );

}