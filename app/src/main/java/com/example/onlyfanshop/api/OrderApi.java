package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.OrderDetailsDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface OrderApi {
//    @GET("/order/getOrders")
//    Call<ApiResponse<List<OrderDTO>>> getOrders();
    @GET("/order/getOrders")
    Call<ApiResponse<List<OrderDTO>>> getOrders(
            @Query("status") String status
    );
    @GET("/order/getOrderDetails")
    Call<ApiResponse<OrderDetailsDTO>> getOrderDetails(@Query("orderId") int orderId);
    @PUT("/order/setOrderStatus")
    Call<ApiResponse<Void>> setOrderStatus(
            @Query("orderId") int orderId,
            @Query("status") String status
    );

}
