package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.NotificationDTO;
import com.example.onlyfanshop.model.response.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationApi {
    @GET("notifications/user/{userID}")
    Call<ApiResponse<List<NotificationDTO>>> getUserNotifications(@Path("userID") int userID);

    @PUT("notifications/{id}/read")
    Call<Void> markAsRead(@Path("id") int id);

    @GET("notifications/user/{userID}/unread-count")
    Call<Long> getUnreadCount(@Path("userID") int userId);
}
