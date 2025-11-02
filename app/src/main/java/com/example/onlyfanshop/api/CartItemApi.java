package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.CartItemDTO;
import com.example.onlyfanshop.model.Request.AddToCartRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.response.CartDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CartItemApi {
    @GET("/cartItem/showCartItem")
    Call<ApiResponse<List<CartItemDTO>>> getCartItem(@Query("username") String username);

    @GET("/cartItem/showInstantBuyItem")
    Call<ApiResponse<List<CartItemDTO>>> getInstantBuyItem(@Query("username") String username);


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
    Call<ApiResponse<Void>> addToCart(@Body AddToCartRequest request);

    @POST("/cart/instantBuy")
    Call<ApiResponse<Void>> instantBuy(@Body AddToCartRequest request);

    @GET("/cart/{userId}")
    Call<ApiResponse<CartDTO>> getCart(@Path("userId") int userId);

    @POST("/cart/clear")
    Call<ApiResponse<Void>> clearCart(@Query("username") String username);

    @PUT("cartItem/onCheck")
    Call<ApiResponse<Void>> onCheck(@Query("cartItemID") Integer cartItemID);

    @DELETE("cart/deleteInstantCart")
    Call<ApiResponse<Void>> deleteInstantCart(@Query("userID") Integer userID);




}
