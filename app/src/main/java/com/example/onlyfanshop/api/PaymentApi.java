package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.PaymentDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PaymentApi {
    @GET("payment/vn-pay")
    Call<ApiResponse<PaymentDTO>> createPayment(
            @Query("amount") double amount,
            @Query("bankCode") String bankCode,
            @Query("address") String address,
            @Query("buyMethod") String buyMethod
            @Query("recipientPhoneNumber") String recipientPhoneNumber
    );
    
    @POST("payment/cod")
    Call<ApiResponse<Integer>> createCODOrder(
            @Query("totalPrice") double totalPrice,
            @Query("address") String address,
            @Query("buyMethod") String buyMethod,
            @Query("recipientPhoneNumber") String recipientPhoneNumber,
            @Query("deliveryType") String deliveryType,
            @Query("storeId") Integer storeId
    );
}
