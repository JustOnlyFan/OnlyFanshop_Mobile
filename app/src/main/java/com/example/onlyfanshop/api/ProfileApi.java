package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.Request.UpdateUserRequest;
import com.example.onlyfanshop.model.response.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface ProfileApi {
    @GET("users/getUser")
    Call<UserResponse> getUser();

    @PUT("users/updateUser")
    Call<UserResponse> updateUser(@Body UpdateUserRequest request);
}