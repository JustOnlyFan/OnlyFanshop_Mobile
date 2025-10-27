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
import com.google.firebase.database.ChildEventListener;
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
    private final Map<String, ChildEventListener> childFirebaseListeners = new HashMap<>();

    // Dedupe and tracking
    private final Map<String, java.util.Set<String>> deliveredMessageIds = new HashMap<>();
    private final Map<String, Long> lastFirebaseEventAt = new HashMap<>();

    // Global chat rooms listener
    private ValueEventListener chatRoomsListener;
    
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
        
        // Start Firebase child listener
        startFirebaseChildListener(roomId);
        
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
    
    // ✅ Firebase child listener for new messages only (reliable realtime)
    private void startFirebaseChildListener(String roomId) {
        try {
            DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("ChatRooms")
                    .child(roomId)
                    .child("messages");

            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    try {
                        ChatMessage message = parseMessageFromSnapshot(snapshot, roomId);
                        if (message == null) return;

                        String messageId = snapshot.getKey();
                        java.util.Set<String> seen = deliveredMessageIds.computeIfAbsent(roomId, k -> new java.util.HashSet<>());
                        if (messageId != null && seen.contains(messageId)) {
                            return; // already delivered
                        }
                        if (messageId != null) {
                            seen.add(messageId);
                        }
                        lastMessageTimestamps.put(roomId, message.getOriginalTimestamp());
                        lastFirebaseEventAt.put(roomId, System.currentTimeMillis());

                        OnNewMessageListener cb = messageListeners.get(roomId);
                        if (cb != null) {
                            mainHandler.post(() -> cb.onNewMessage(message));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onChildAdded parse error: " + e.getMessage());
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    // Optionally handle message updates (read status, edits)
                    try {
                        ChatMessage message = parseMessageFromSnapshot(snapshot, roomId);
                        if (message == null) return;
                        lastFirebaseEventAt.put(roomId, System.currentTimeMillis());
                        OnNewMessageListener cb = messageListeners.get(roomId);
                        if (cb != null) {
                            mainHandler.post(() -> cb.onNewMessage(message));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onChildChanged parse error: " + e.getMessage());
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    // No-op for now
                }

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                    // No-op
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Firebase child listener cancelled for room: " + roomId + ", error: " + error.getMessage());
                }
            };

            messagesRef.addChildEventListener(listener);
            childFirebaseListeners.put(roomId, listener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting Firebase child listener: " + e.getMessage());
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
                // If Firebase has recently delivered events, skip polling to reduce load
                Long lastEvent = lastFirebaseEventAt.get(roomId);
                long now = System.currentTimeMillis();
                if (lastEvent == null || (now - lastEvent) > 5000) {
                    pollForNewMessages(roomId);
                }
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
        // Also start a lightweight Firebase listener to trigger immediate refreshes
        startChatRoomsFirebaseListener();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollForChatRooms();
            } catch (Exception e) {
                Log.e(TAG, "Error in chat room polling: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS); // Poll every 3 seconds
    }

    private void startChatRoomsFirebaseListener() {
        try {
            if (chatRoomsListener != null) return; // already started
            DatabaseReference roomsRef = FirebaseDatabase.getInstance()
                    .getReference("ChatRooms");
            chatRoomsListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Any change to rooms triggers immediate refresh via API
                    pollForChatRooms();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "ChatRooms listener cancelled: " + error.getMessage());
                }
            };
            roomsRef.addValueEventListener(chatRoomsListener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting chat rooms Firebase listener: " + e.getMessage());
        }
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
        deliveredMessageIds.remove(roomId);
        lastFirebaseEventAt.remove(roomId);
        
        ValueEventListener vListener = firebaseListeners.remove(roomId);
        ChildEventListener cListener = childFirebaseListeners.remove(roomId);
        try {
            DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("ChatRooms")
                    .child(roomId)
                    .child("messages");
            if (vListener != null) {
                messagesRef.removeEventListener(vListener);
            }
            if (cListener != null) {
                messagesRef.removeEventListener(cListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing Firebase listener: " + e.getMessage());
        }
    }
    
    // ✅ Stop listening for chat rooms
    public void stopListeningForChatRooms() {
        Log.d(TAG, "Stopping real-time listening for chat rooms");
        roomListeners.clear();
        try {
            if (chatRoomsListener != null) {
                DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("ChatRooms");
                roomsRef.removeEventListener(chatRoomsListener);
                chatRoomsListener = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing chat rooms Firebase listener: " + e.getMessage());
        }
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
                Log.e(TAG, "Error removing Firebase value listener: " + e.getMessage());
            }
        }
        for (Map.Entry<String, ChildEventListener> entry : childFirebaseListeners.entrySet()) {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("ChatRooms")
                        .child(entry.getKey())
                        .child("messages");
                messagesRef.removeEventListener(entry.getValue());
            } catch (Exception e) {
                Log.e(TAG, "Error removing Firebase child listener: " + e.getMessage());
            }
        }

        // Remove chat rooms global listener
        try {
            if (chatRoomsListener != null) {
                DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("ChatRooms");
                roomsRef.removeEventListener(chatRoomsListener);
                chatRoomsListener = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing chatRooms listener: " + e.getMessage());
        }

        firebaseListeners.clear();
        childFirebaseListeners.clear();
        messageListeners.clear();
        roomListeners.clear();
        lastMessageTimestamps.clear();
        lastRoomUpdateTimes.clear();
        deliveredMessageIds.clear();
        lastFirebaseEventAt.clear();
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

    // ✅ Seed seen messages to avoid re-delivery on initial attach
    public void seedSeenMessages(String roomId, List<ChatMessage> existingMessages) {
        if (existingMessages == null || existingMessages.isEmpty()) return;
        long maxTs = 0L;
        java.util.Set<String> seen = deliveredMessageIds.computeIfAbsent(roomId, k -> new java.util.HashSet<>());
        for (ChatMessage m : existingMessages) {
            if (m.getMessageId() != null) {
                seen.add(m.getMessageId());
            }
            if (m.getOriginalTimestamp() > maxTs) {
                maxTs = m.getOriginalTimestamp();
            }
        }
        lastMessageTimestamps.put(roomId, maxTs);
        lastFirebaseEventAt.put(roomId, System.currentTimeMillis());
    }
}
