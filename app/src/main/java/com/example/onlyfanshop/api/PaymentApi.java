package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.PaymentDTO;
import com.example.onlyfanshop.model.response.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PaymentApi {
    @GET("payment/vn-pay")
    Call<ApiResponse<PaymentDTO>> createPayment(
            @Query("amount") double amount,
            @Query("bankCode") String bankCode,
            @Query("address") String address,
            @Query("recipientPhoneNumber") String recipientPhoneNumber
    );
}
