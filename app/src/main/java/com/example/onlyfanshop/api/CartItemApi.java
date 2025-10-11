package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.CartDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CartItemApi {
    @GET("/cartItem/showCartItem")
    Call<ApiResponse<List<CartItemDTO>>> getCartItem(@Query("username") String username);

    @POST("/cartItem/addQuantity")
    Call<ApiResponse<Void>> addQuantity(
            @Query("username") String username,
            @Query("productID") int productId
    );

    @POST("/cartItem/minusQuantity")
    Call<ApiResponse<Void>> minusQuantity(
            @Query("username") String username,
            @Query("productID") int productId
    );

    @POST("/cart/addToCart")
    Call<ApiResponse<Void>> addToCart(@Query("productID") int productId, @Query("username") String username);

    @GET("/cart/{userId}")
    Call<ApiResponse<CartDTO>> getCart(@Path("userId") int userId);


}
