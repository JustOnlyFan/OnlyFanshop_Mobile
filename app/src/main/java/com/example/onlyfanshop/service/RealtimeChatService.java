package com.example.onlyfanshop.service;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.example.onlyfanshop.api.ChatApi;
import com.example.onlyfanshop.model.chat.ChatMessage;
import com.example.onlyfanshop.model.chat.ChatRoom;
import com.example.onlyfanshop.utils.AppPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RealtimeChatService {
    private static final String TAG = "RealtimeChatService";
    private static RealtimeChatService instance;
    
    private final Context context;
    private final ChatApi chatApi;
    private final Handler mainHandler;
    private final ScheduledExecutorService scheduler;
    
    // Event listeners
    private final Map<String, OnNewMessageListener> messageListeners = new HashMap<>();
    private final Map<String, OnChatRoomUpdateListener> roomListeners = new HashMap<>();
    
    // Polling
    private final Map<String, Long> lastMessageTimestamps = new HashMap<>();
    private final Map<String, String> lastRoomUpdateTimes = new HashMap<>();
    
    // Firebase listeners
    private final Map<String, ValueEventListener> firebaseListeners = new HashMap<>();
    
    private RealtimeChatService(Context context, ChatApi chatApi) {
        this.context = context;
        this.chatApi = chatApi;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public static synchronized RealtimeChatService getInstance(Context context, ChatApi chatApi) {
        if (instance == null) {
            instance = new RealtimeChatService(context, chatApi);
        }
        return instance;
    }
    
    // ✅ Start real-time listening for messages
    public void startListeningForMessages(String roomId, OnNewMessageListener listener) {
        Log.d(TAG, "Starting real-time listening for room: " + roomId);
        
        // Store listener
        messageListeners.put(roomId, listener);
        
        // Start Firebase listener
        startFirebaseListener(roomId);
        
        // Start polling as backup
        startPolling(roomId);
    }
    
    // ✅ Start real-time listening for chat rooms
    public void startListeningForChatRooms(OnChatRoomUpdateListener listener) {
        Log.d(TAG, "Starting real-time listening for chat rooms");
        
        // Store listener
        roomListeners.put("global", listener);
        
        // Start polling for chat rooms
        startChatRoomPolling();
    }
    
    // ✅ Firebase listener
    private void startFirebaseListener(String roomId) {
        try {
            DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("ChatRooms")
                    .child(roomId)
                    .child("messages");
            
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Log.d(TAG, "Firebase data changed for room: " + roomId);
                    processFirebaseMessages(roomId, snapshot);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Firebase listener cancelled for room: " + roomId + ", error: " + error.getMessage());
                }
            };
            
            messagesRef.addValueEventListener(listener);
            firebaseListeners.put(roomId, listener);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Firebase listener: " + e.getMessage());
        }
    }
    
    // ✅ Process Firebase messages
    private void processFirebaseMessages(String roomId, DataSnapshot snapshot) {
        try {
            List<ChatMessage> newMessages = new ArrayList<>();
            
            for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                try {
                    ChatMessage message = parseMessageFromSnapshot(messageSnapshot, roomId);
                    if (message != null) {
                        // Check if this is a new message
                        Long messageTime = message.getOriginalTimestamp();
                        Long lastTime = lastMessageTimestamps.get(roomId);
                        
                        if (lastTime == null || messageTime > lastTime) {
                            newMessages.add(message);
                            lastMessageTimestamps.put(roomId, messageTime);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message: " + e.getMessage());
                }
            }
            
            // Notify listeners
            if (!newMessages.isEmpty()) {
                OnNewMessageListener listener = messageListeners.get(roomId);
                if (listener != null) {
                    for (ChatMessage message : newMessages) {
                        mainHandler.post(() -> listener.onNewMessage(message));
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Firebase messages: " + e.getMessage());
        }
    }
    
    // ✅ Polling system for messages
    private void startPolling(String roomId) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollForNewMessages(roomId);
            } catch (Exception e) {
                Log.e(TAG, "Error in polling: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS); // Poll every 2 seconds
    }
    
    // ✅ Poll for new messages
    private void pollForNewMessages(String roomId) {
        try {
            // Get messages from API
            chatApi.getMessagesForRoom(roomId).enqueue(new retrofit2.Callback<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> call, 
                                     retrofit2.Response<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStatusCode() == 200) {
                        List<ChatMessage> messages = response.body().getData();
                        
                        // Process new messages
                        for (ChatMessage message : messages) {
                            Long messageTime = message.getOriginalTimestamp();
                            Long lastTime = lastMessageTimestamps.get(roomId);
                            
                            if (lastTime == null || messageTime > lastTime) {
                                lastMessageTimestamps.put(roomId, messageTime);
                                
                                OnNewMessageListener listener = messageListeners.get(roomId);
                                if (listener != null) {
                                    mainHandler.post(() -> listener.onNewMessage(message));
                                }
                            }
                        }
                    }
                }
                
                @Override
                public void onFailure(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> call, Throwable t) {
                    Log.e(TAG, "Error polling messages: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in pollForNewMessages: " + e.getMessage());
        }
    }
    
    // ✅ Polling for chat rooms
    private void startChatRoomPolling() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollForChatRooms();
            } catch (Exception e) {
                Log.e(TAG, "Error in chat room polling: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS); // Poll every 3 seconds
    }
    
    // ✅ Poll for chat rooms
    private void pollForChatRooms() {
        try {
            chatApi.getChatRooms().enqueue(new retrofit2.Callback<com.example.onlyfanshop.model.response.ApiResponse<List<ChatRoom>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatRoom>>> call, 
                                     retrofit2.Response<com.example.onlyfanshop.model.response.ApiResponse<List<ChatRoom>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStatusCode() == 200) {
                        List<ChatRoom> rooms = response.body().getData();
                        
                        OnChatRoomUpdateListener listener = roomListeners.get("global");
                        if (listener != null) {
                            mainHandler.post(() -> listener.onChatRoomsUpdated(rooms));
                        }
                    }
                }
                
                @Override
                public void onFailure(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatRoom>>> call, Throwable t) {
                    Log.e(TAG, "Error polling chat rooms: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in pollForChatRooms: " + e.getMessage());
        }
    }
    
    // ✅ Parse message from snapshot
    private ChatMessage parseMessageFromSnapshot(DataSnapshot snapshot, String roomId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setMessageId(snapshot.getKey());
            message.setSenderId(snapshot.child("senderId").getValue(String.class));
            message.setSenderName(snapshot.child("senderName").getValue(String.class));
            message.setMessage(snapshot.child("message").getValue(String.class));
            
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
            
            String currentUserId = AppPreferences.getUserId(context);
            message.setMe(message.getSenderId().equals(currentUserId));
            
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message: " + e.getMessage());
            return null;
        }
    }
    
    // ✅ Stop listening for messages
    public void stopListeningForMessages(String roomId) {
        Log.d(TAG, "Stopping real-time listening for room: " + roomId);
        
        messageListeners.remove(roomId);
        lastMessageTimestamps.remove(roomId);
        
        ValueEventListener listener = firebaseListeners.remove(roomId);
        if (listener != null) {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("ChatRooms")
                        .child(roomId)
                        .child("messages");
                messagesRef.removeEventListener(listener);
            } catch (Exception e) {
                Log.e(TAG, "Error removing Firebase listener: " + e.getMessage());
            }
        }
    }
    
    // ✅ Stop listening for chat rooms
    public void stopListeningForChatRooms() {
        Log.d(TAG, "Stopping real-time listening for chat rooms");
        roomListeners.clear();
    }
    
    // ✅ Stop all listening
    public void stopAll() {
        Log.d(TAG, "Stopping all real-time listening");
        
        // Stop all Firebase listeners
        for (Map.Entry<String, ValueEventListener> entry : firebaseListeners.entrySet()) {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("ChatRooms")
                        .child(entry.getKey())
                        .child("messages");
                messagesRef.removeEventListener(entry.getValue());
            } catch (Exception e) {
                Log.e(TAG, "Error removing Firebase listener: " + e.getMessage());
            }
        }
        
        firebaseListeners.clear();
        messageListeners.clear();
        roomListeners.clear();
        lastMessageTimestamps.clear();
        lastRoomUpdateTimes.clear();
    }
    
    // ✅ Cleanup
    public void cleanup() {
        stopAll();
        scheduler.shutdown();
    }
    
    // Interfaces
    public interface OnNewMessageListener {
        void onNewMessage(ChatMessage newMessage);
    }
    
    public interface OnChatRoomUpdateListener {
        void onChatRoomsUpdated(List<ChatRoom> chatRooms);
    }
}
