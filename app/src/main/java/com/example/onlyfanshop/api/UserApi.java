package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.Request.LoginRequest;
import com.example.onlyfanshop.model.Request.RegisterRequest;
import com.example.onlyfanshop.model.UserDTO;
import com.example.onlyfanshop.model.response.UserPageResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface UserApi {
    @POST("login/signin")
    Call<ApiResponse<UserDTO>> login(@Body LoginRequest request);
    @POST("login/register")
    Call<ApiResponse<Void>> register(@Body RegisterRequest request);
    @FormUrlEncoded
    @POST("login/send-otp")
    Call<ApiResponse<Void>> sendOtp(@Field("email") String email);

    @FormUrlEncoded
    @POST("login/verify-otp")
    Call<ApiResponse<Void>> verifyOtp(
            @Field("email") String email,
            @Field("otp") String otp
    );
    @GET("login/check-account")
    Call<Map<String, Boolean>> checkAccount(
            @Query("username") String username,
            @Query("email") String email
    );

    @FormUrlEncoded
    @POST("login/reset-password")
    Call<ApiResponse<Void>> resetPassword(
            @Field("email") String email,
            @Field("newPassword") String newPassword
    );
    
    // Google login endpoint
    @POST("api/auth/google/login")
    Call<ApiResponse<UserDTO>> googleLogin(@Body GoogleLoginRequest request, @retrofit2.http.Header("X-Custom-Token") String customToken);
    
    // Inner class for Google login request
    class GoogleLoginRequest {
        private String email;
        private String username;
        
        public GoogleLoginRequest(String email, String username) {
            this.email = email;
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }
    @POST("users/logout")
    Call<ApiResponse<Void>> logout();

    @PUT("users/changeAddress")
    Call<ApiResponse<Void>> changeAddress(@Query("address") String address,@Header("Authorization") String token);

    @GET("users/getAllUsers")
    Call<ApiResponse<UserPageResponse>> getAllUsers(
            @Query("keyword") String keyword,
            @Query("role") String role,
            @Query("page") int page,
            @Query("size") int size,
            @Query("sortField") String sortField,
            @Query("sortDirection") String sortDirection
    );
}
