package com.example.onlyfanshop.service;

import android.util.Log;

import com.example.onlyfanshop.api.ChatApi;
import com.example.onlyfanshop.model.chat.ChatMessage;
import com.example.onlyfanshop.model.chat.ChatRoom;
import com.example.onlyfanshop.model.Request.SendMessageRequest;
import com.example.onlyfanshop.model.Request.UpdateFCMTokenRequest;
import com.example.onlyfanshop.model.response.ApiResponse;
import com.example.onlyfanshop.utils.AppPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import android.content.Context;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatService {
    private static final String TAG = "ChatService";
    private final ChatApi chatApi;
    private final Context context;

    public ChatService(ChatApi chatApi, Context context) {
        this.chatApi = chatApi;
        this.context = context;
    }

    public interface ChatRoomsCallback {
        void onSuccess(List<ChatRoom> chatRooms);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onSuccess(List<ChatMessage> messages);
        void onError(String error);
    }

    public interface SendMessageCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface RoomCallback {
        void onSuccess(String roomId);
        void onError(String error);
    }

    public interface FCMTokenCallback {
        void onSuccess();
        void onError(String error);
    }

    public void getChatRooms(ChatRoomsCallback callback) {
        chatApi.getChatRooms().enqueue(new Callback<ApiResponse<List<ChatRoom>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatRoom>>> call, Response<ApiResponse<List<ChatRoom>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to get chat rooms");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatRoom>>> call, Throwable t) {
                Log.e(TAG, "Error getting chat rooms", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getMessagesForRoom(String roomId, MessagesCallback callback) {
        chatApi.getMessagesForRoom(roomId).enqueue(new Callback<ApiResponse<List<ChatMessage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessage>>> call, Response<ApiResponse<List<ChatMessage>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        List<ChatMessage> messages = response.body().getData();
                        // Set isMe flag based on current user
                        String currentUserId = AppPreferences.getUserId(context);
                        Log.d(TAG, "Current User ID: " + currentUserId);
                        
                        for (ChatMessage message : messages) {
                            Log.d(TAG, "Message senderId: " + message.getSenderId() + ", isMe: " + message.getSenderId().equals(currentUserId));
                            message.setMe(message.getSenderId().equals(currentUserId));
                        }
                        callback.onSuccess(messages);
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to get messages");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessage>>> call, Throwable t) {
                Log.e(TAG, "Error getting messages", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void sendMessage(String roomId, String message, SendMessageCallback callback) {
        SendMessageRequest request = new SendMessageRequest(roomId, message, null, null, null);
        
        chatApi.sendMessage(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to send message");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "Error sending message", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void markMessagesAsRead(String roomId, SendMessageCallback callback) {
        chatApi.markMessagesAsRead(roomId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to mark messages as read");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "Error marking messages as read", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getOrCreateCustomerRoom(RoomCallback callback) {
        chatApi.getOrCreateCustomerRoom().enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to get/create customer room");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "Error getting/creating customer room", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateFCMToken(String fcmToken, FCMTokenCallback callback) {
        UpdateFCMTokenRequest request = new UpdateFCMTokenRequest(fcmToken);
        
        chatApi.updateFCMToken(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Failed to update FCM token");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "Error updating FCM token", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void listenForNewMessages(String roomId, OnNewMessageListener listener) {
        // ✅ Initialize Firebase in background thread to prevent ANR
        new Thread(() -> {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("ChatRooms")
                        .child(roomId)
                        .child("messages");
        
                messagesRef.addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        try {
                            // Parse message manually to handle Long timestamp
                            ChatMessage message = new ChatMessage();
                            message.setMessageId(snapshot.getKey());
                            message.setSenderId(snapshot.child("senderId").getValue(String.class));
                            message.setSenderName(snapshot.child("senderName").getValue(String.class));
                            message.setMessage(snapshot.child("message").getValue(String.class));
                            
                            // Handle Long timestamp
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                            if (timestamp != null) {
                                message.setTimestampFromLong(timestamp);
                            }
                            
                            message.setAttachmentUrl(snapshot.child("attachmentUrl").getValue(String.class));
                            message.setAttachmentType(snapshot.child("attachmentType").getValue(String.class));
                            message.setReplyToMessageId(snapshot.child("replyToMessageId").getValue(String.class));
                            
                            Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);
                            message.setRead(isRead != null ? isRead : false);
                            
                            message.setRoomId(roomId);
                            
                            // Set isMe flag
                            String currentUserId = AppPreferences.getUserId(context);
                            message.setMe(message.getSenderId().equals(currentUserId));
                            
                            listener.onNewMessage(message);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing message: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                        // Handle message updates if needed
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        // Handle message removal if needed
                    }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                        // Handle message reordering if needed
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error listening for messages: " + error.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error setting up Firebase listener: " + e.getMessage());
            }
        }).start();
    }

    public interface OnNewMessageListener {
        void onNewMessage(ChatMessage newMessage);
    }

}
