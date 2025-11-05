package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.OrderDTO;
import com.example.onlyfanshop.model.OrderDetailsDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;
import java.util.Map;

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
    @PUT("/order/cancelOrder")
    Call<ApiResponse<Void>> cancelOrder(
            @Query("orderId") int orderId
    );

    @GET("/order/getOrdersPending")
    Call<ApiResponse<List<OrderDTO>>> getOrdersPending();

    @GET("/order/getOrdersConfirmed")
    Call<ApiResponse<List<OrderDTO>>> getOrdersConfirmed();

    @GET("/order/getOrdersShipping")
    Call<ApiResponse<List<OrderDTO>>> getOrdersShipping();

    @GET("/order/getOrdersCompleted")
    Call<ApiResponse<List<OrderDTO>>> getOrdersCompleted();

    @GET("/order/getOrdersCancelled")
    Call<ApiResponse<List<OrderDTO>>> getOrdersCancelled();

    @GET("/order/badgeCount")
    Call<ApiResponse<Map<String, Long>>> getBadgeCount();

}
