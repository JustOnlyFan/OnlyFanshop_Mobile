package com.example.onlyfanshop.api;

import com.example.onlyfanshop.model.chat.ChatMessage;
import com.example.onlyfanshop.model.chat.ChatRoom;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.model.Request.SendMessageRequest;
import com.example.onlyfanshop.model.Request.UpdateFCMTokenRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ChatApi {
    
    @GET("api/chat/rooms")
    Call<ApiResponse<List<ChatRoom>>> getChatRooms();
    
    @GET("api/chat/rooms/{roomId}/messages")
    Call<ApiResponse<List<ChatMessage>>> getMessagesForRoom(@Path("roomId") String roomId);
    
    @POST("api/chat/messages")
    Call<ApiResponse<String>> sendMessage(@Body SendMessageRequest request);
    
    @POST("api/chat/rooms/{roomId}/read")
    Call<ApiResponse<String>> markMessagesAsRead(@Path("roomId") String roomId);
    
    @GET("api/chat/rooms/customer")
    Call<ApiResponse<String>> getOrCreateCustomerRoom();
    
    @PUT("api/users/fcm-token")
    Call<ApiResponse<String>> updateFCMToken(@Body UpdateFCMTokenRequest request);
}
