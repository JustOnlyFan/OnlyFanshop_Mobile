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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Map<String, ChildEventListener> firebaseChildListeners = new HashMap<>();
    private final Map<String, ValueEventListener> roomMetaListeners = new HashMap<>();
    // Track polling tasks per room to allow cancellation and avoid duplicates
    private final Map<String, ScheduledFuture<?>> messagePollingFutures = new HashMap<>();
    private ScheduledFuture<?> chatRoomsPollingFuture;
    private final Map<String, AtomicBoolean> messagePollInFlight = new ConcurrentHashMap<>();
    private final AtomicBoolean chatRoomsPollInFlight = new AtomicBoolean(false);
    
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
        
        // If already listening for this room, just replace listener and return
        if (messageListeners.containsKey(roomId)) {
            Log.d(TAG, "Already listening for room: " + roomId + ", updating listener only");
            messageListeners.put(roomId, listener);
            return;
        }
        messageListeners.put(roomId, listener);
        
        // Start Firebase listener
        startFirebaseListener(roomId);
        
        // Start polling as backup
        startPolling(roomId);
    }
    
    // ✅ Start real-time listening for chat rooms
    public void startListeningForChatRooms(OnChatRoomUpdateListener listener) {
        Log.d(TAG, "Starting real-time listening for chat rooms");
        
        // If already listening, just replace listener
        if (roomListeners.containsKey("global")) {
            Log.d(TAG, "Already listening for chat rooms, updating listener only");
            roomListeners.put("global", listener);
        } else {
            roomListeners.put("global", listener);
        }
        
        // Start Firebase listener to nudge instant updates, plus polling as fallback
        startChatRoomsFirebaseListener();
        startChatRoomPolling();
    }
    
    // ✅ Firebase listener - Improved for better real-time updates
    private void startFirebaseListener(String roomId) {
        try {
            Runnable attach = () -> {
                try {
                    // ✅ Listen to Messages path - đây là path đúng trong Firebase
                    DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                            .getReference("Messages")
                            .child(roomId);

                    // ✅ ChildEventListener để detect tin nhắn mới ngay lập tức
                    ChildEventListener childListener = new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                            Log.d(TAG, "New message detected via onChildAdded for room: " + roomId);
                            ChatMessage message = parseMessageFromSnapshot(snapshot, roomId);
                            if (message != null) {
                                // Forward immediately; UI layer will deduplicate by messageId
                                OnNewMessageListener listener = messageListeners.get(roomId);
                                if (listener != null) {
                                    mainHandler.post(() -> listener.onNewMessage(message));
                                }
                                // Update last seen timestamp as a hint for poll fallback
                                Long messageTime = message.getOriginalTimestamp();
                                if (messageTime != null) {
                                    Long lastTime = lastMessageTimestamps.get(roomId);
                                    if (lastTime == null || messageTime > lastTime) {
                                        lastMessageTimestamps.put(roomId, messageTime);
                                    }
                                }
                            }
                        }

                        @Override 
                        public void onChildChanged(DataSnapshot s, String p) {
                            Log.d(TAG, "Message changed for room: " + roomId);
                            // Process changed message as well
                            ChatMessage message = parseMessageFromSnapshot(s, roomId);
                            if (message != null) {
                                OnNewMessageListener listener = messageListeners.get(roomId);
                                if (listener != null) {
                                    mainHandler.post(() -> listener.onNewMessage(message));
                                }
                            }
                        }
                        
                        @Override public void onChildRemoved(DataSnapshot s) {}
                        @Override public void onChildMoved(DataSnapshot s, String p) {}
                        
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Firebase child listener cancelled: " + error.getMessage());
                        }
                    };
                    messagesRef.addChildEventListener(childListener);
                    firebaseChildListeners.put(roomId, childListener);

                    // ✅ ValueEventListener để detect bất kỳ thay đổi nào
                    ValueEventListener valueListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Log.d(TAG, "Messages data changed for room: " + roomId);
                            processFirebaseMessages(roomId, snapshot);
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Firebase listener cancelled for room: " + roomId + ", error: " + error.getMessage());
                        }
                    };
                    messagesRef.addValueEventListener(valueListener);
                    firebaseListeners.put(roomId, valueListener);

                    // ✅ Listen to Conversations/{roomId}/lastMessageTime để detect khi có tin nhắn mới
                    // Đây là cách nhanh nhất để detect tin nhắn mới từ máy ảo khác
                    DatabaseReference lastMessageTimeRef = FirebaseDatabase.getInstance()
                            .getReference("Conversations")
                            .child(roomId)
                            .child("lastMessageTime");
                    ValueEventListener metaListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            // Trigger immediate fetch of latest messages khi lastMessageTime thay đổi
                            Log.d(TAG, "lastMessageTime changed for room: " + roomId + ", value: " + snapshot.getValue());
                            // ✅ Trigger immediate poll để fetch tin nhắn mới ngay lập tức
                            pollForNewMessages(roomId);
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Meta listener cancelled: " + error.getMessage());
                        }
                    };
                    lastMessageTimeRef.addValueEventListener(metaListener);
                    roomMetaListeners.put(roomId, metaListener);
                    
                    // ✅ Listen to Conversations/{roomId}/lastMessage để detect tin nhắn mới
                    DatabaseReference lastMessageRef = FirebaseDatabase.getInstance()
                            .getReference("Conversations")
                            .child(roomId)
                            .child("lastMessage");
                    ValueEventListener lastMessageListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Log.d(TAG, "lastMessage changed for room: " + roomId + ", message: " + snapshot.getValue());
                            // Trigger immediate poll khi lastMessage thay đổi
                            pollForNewMessages(roomId);
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Last message listener cancelled: " + error.getMessage());
                        }
                    };
                    lastMessageRef.addValueEventListener(lastMessageListener);
                    roomMetaListeners.put(roomId + "_lastMessage", lastMessageListener);
                    
                } catch (Exception e) {
                    Log.e(TAG, "attach listeners error: " + e.getMessage());
                }
            };

            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) {
                FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                        FirebaseUser cu = firebaseAuth.getCurrentUser();
                        if (cu != null) {
                            firebaseAuth.removeAuthStateListener(this);
                            mainHandler.post(attach);
                        }
                    }
                });
            } else {
                mainHandler.post(attach);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting Firebase listener: " + e.getMessage());
        }
    }
    
    // ✅ Process Firebase messages - Improved to prevent duplicates
    private void processFirebaseMessages(String roomId, DataSnapshot snapshot) {
        try {
            List<ChatMessage> newMessages = new ArrayList<>();
            Long currentLastTime = lastMessageTimestamps.get(roomId);
            
            for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                try {
                    ChatMessage message = parseMessageFromSnapshot(messageSnapshot, roomId);
                    if (message != null) {
                        // ✅ Check if this is a new message by comparing timestamp
                        Long messageTime = message.getOriginalTimestamp();
                        
                        // ✅ Chỉ thêm tin nhắn mới hơn lastTime để tránh duplicate
                        if (messageTime != null && (currentLastTime == null || messageTime > currentLastTime)) {
                            newMessages.add(message);
                            // Update lastTime ngay lập tức để tránh duplicate
                            if (currentLastTime == null || messageTime > currentLastTime) {
                                currentLastTime = messageTime;
                                lastMessageTimestamps.put(roomId, messageTime);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message: " + e.getMessage());
                }
            }
            
            // ✅ Notify listeners cho tin nhắn mới
            if (!newMessages.isEmpty()) {
                OnNewMessageListener listener = messageListeners.get(roomId);
                if (listener != null) {
                    for (ChatMessage message : newMessages) {
                        mainHandler.post(() -> listener.onNewMessage(message));
                        Log.d(TAG, "Processed new message from Firebase: " + message.getMessage() + " at time: " + message.getOriginalTimestamp());
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Firebase messages: " + e.getMessage());
        }
    }
    
    // ✅ Polling system for messages
    private void startPolling(String roomId) {
        // Prevent duplicate pollers for the same room
        ScheduledFuture<?> existing = messagePollingFutures.get(roomId);
        if (existing != null && !existing.isCancelled()) {
            Log.d(TAG, "Polling already active for room: " + roomId);
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollForNewMessages(roomId);
            } catch (Exception e) {
                Log.e(TAG, "Error in polling: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS); // Faster fallback ~1s

        messagePollingFutures.put(roomId, future);
    }
    
    // ✅ Poll for new messages - Improved to prevent duplicates
    private void pollForNewMessages(String roomId) {
        try {
            // ✅ Prevent concurrent polling for the same room
            AtomicBoolean inFlight = messagePollInFlight.computeIfAbsent(roomId, k -> new AtomicBoolean(false));
            if (!inFlight.compareAndSet(false, true)) {
                Log.d(TAG, "Poll already in flight for room: " + roomId);
                return;
            }
            
            // Get messages from API
            chatApi.getMessagesForRoom(roomId).enqueue(new retrofit2.Callback<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> call, 
                                     retrofit2.Response<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> response) {
                    try {
                        inFlight.set(false); // Reset flag
                        
                        if (response.isSuccessful() && response.body() != null && response.body().getStatusCode() == 200) {
                            List<ChatMessage> messages = response.body().getData();
                            if (messages == null || messages.isEmpty()) {
                                return;
                            }
                            
                            Log.d(TAG, "Polled " + messages.size() + " messages for room: " + roomId);
                            
                            // ✅ Process new messages - chỉ xử lý tin nhắn mới hơn lastTime
                            Long currentLastTime = lastMessageTimestamps.get(roomId);
                            OnNewMessageListener listener = messageListeners.get(roomId);
                            
                            if (listener != null) {
                                for (ChatMessage message : messages) {
                                    Long messageTime = message.getOriginalTimestamp();
                                    if (messageTime == null) continue;
                                    
                                    // ✅ Chỉ notify tin nhắn mới hơn lastTime để tránh duplicate
                                    if (currentLastTime == null || messageTime > currentLastTime) {
                                        // Update lastTime ngay lập tức để tránh duplicate trong lần poll tiếp theo
                                        if (currentLastTime == null || messageTime > currentLastTime) {
                                            lastMessageTimestamps.put(roomId, messageTime);
                                            currentLastTime = messageTime;
                                        }
                                        // Notify listener
                                        mainHandler.post(() -> listener.onNewMessage(message));
                                        Log.d(TAG, "Notified new message: " + message.getMessage() + " at time: " + messageTime);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing polled messages: " + e.getMessage());
                        inFlight.set(false);
                    }
                }
                
                @Override
                public void onFailure(retrofit2.Call<com.example.onlyfanshop.model.response.ApiResponse<List<ChatMessage>>> call, Throwable t) {
                    Log.e(TAG, "Error polling messages: " + t.getMessage());
                    AtomicBoolean inFlight = messagePollInFlight.get(roomId);
                    if (inFlight != null) {
                        inFlight.set(false);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in pollForNewMessages: " + e.getMessage());
            AtomicBoolean inFlight = messagePollInFlight.get(roomId);
            if (inFlight != null) {
                inFlight.set(false);
            }
        }
    }
    
    // ✅ Polling for chat rooms
    private void startChatRoomPolling() {
        if (chatRoomsPollingFuture != null && !chatRoomsPollingFuture.isCancelled()) {
            Log.d(TAG, "Chat room polling already active");
            return;
        }

        chatRoomsPollingFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollForChatRooms();
            } catch (Exception e) {
                Log.e(TAG, "Error in chat room polling: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS); // Faster fallback ~1s
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

    // Best-effort Firebase listener to trigger faster chat room refreshes
    private void startChatRoomsFirebaseListener() {
        try {
            DatabaseReference chatRoomsRef = FirebaseDatabase.getInstance()
                    .getReference("Conversations");

            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Nudge immediate refresh of chat rooms list
                    mainHandler.post(() -> pollForChatRooms());
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "ChatRooms Firebase listener cancelled: " + error.getMessage());
                }
            };

            chatRoomsRef.addValueEventListener(listener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting chat rooms Firebase listener: " + e.getMessage());
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
                // Detect seconds vs millis
                long ts = timestamp;
                if (ts < 1_000_000_000_000L) ts *= 1000L;
                message.setTimestampFromLong(ts);
            }
            
            message.setAttachmentUrl(snapshot.child("attachmentUrl").getValue(String.class));
            message.setAttachmentType(snapshot.child("attachmentType").getValue(String.class));
            message.setReplyToMessageId(snapshot.child("replyToMessageId").getValue(String.class));
            
            Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);
            message.setRead(isRead != null ? isRead : false);
            
            message.setRoomId(roomId);

            String currentUserId = AppPreferences.getUserId(context);
            String sender = message.getSenderId();
            message.setMe(currentUserId != null && currentUserId.equals(sender));
            
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

        ChildEventListener child = firebaseChildListeners.remove(roomId);
        if (child != null) {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("Messages")
                        .child(roomId);
                messagesRef.removeEventListener(child);
            } catch (Exception e) {
                Log.e(TAG, "Error removing Firebase child listener: " + e.getMessage());
            }
        }

        ValueEventListener meta = roomMetaListeners.remove(roomId);
        if (meta != null) {
            try {
                DatabaseReference lastMessageTimeRef = FirebaseDatabase.getInstance()
                        .getReference("Conversations")
                        .child(roomId)
                        .child("lastMessageTime");
                lastMessageTimeRef.removeEventListener(meta);
            } catch (Exception e) {
                Log.e(TAG, "Error removing room meta listener: " + e.getMessage());
            }
        }
        
        // ✅ Remove lastMessage listener
        ValueEventListener lastMessage = roomMetaListeners.remove(roomId + "_lastMessage");
        if (lastMessage != null) {
            try {
                DatabaseReference lastMessageRef = FirebaseDatabase.getInstance()
                        .getReference("Conversations")
                        .child(roomId)
                        .child("lastMessage");
                lastMessageRef.removeEventListener(lastMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error removing lastMessage listener: " + e.getMessage());
            }
        }
        
        // ✅ Clear polling flag
        messagePollInFlight.remove(roomId);

        // Cancel polling task for this room if exists
        ScheduledFuture<?> future = messagePollingFutures.remove(roomId);
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
            Log.d(TAG, "Cancelled polling for room: " + roomId);
        }
    }
    
    // ✅ Stop listening for chat rooms
    public void stopListeningForChatRooms() {
        Log.d(TAG, "Stopping real-time listening for chat rooms");
        roomListeners.clear();

        // Cancel chat room polling if running
        if (chatRoomsPollingFuture != null && !chatRoomsPollingFuture.isCancelled()) {
            chatRoomsPollingFuture.cancel(true);
            chatRoomsPollingFuture = null;
            Log.d(TAG, "Cancelled chat rooms polling");
        }
    }
    
    // ✅ Stop all listening
    public void stopAll() {
        Log.d(TAG, "Stopping all real-time listening");
        
        // Stop all Firebase listeners
        for (Map.Entry<String, ValueEventListener> entry : firebaseListeners.entrySet()) {
            try {
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                        .getReference("Messages")
                        .child(entry.getKey());
                messagesRef.removeEventListener(entry.getValue());
            } catch (Exception e) {
                Log.e(TAG, "Error removing Firebase listener: " + e.getMessage());
            }
        }
        
        // ✅ Remove all room meta listeners
        for (Map.Entry<String, ValueEventListener> entry : roomMetaListeners.entrySet()) {
            try {
                String key = entry.getKey();
                if (key.endsWith("_lastMessage")) {
                    String roomId = key.replace("_lastMessage", "");
                    DatabaseReference lastMessageRef = FirebaseDatabase.getInstance()
                            .getReference("Conversations")
                            .child(roomId)
                            .child("lastMessage");
                    lastMessageRef.removeEventListener(entry.getValue());
                } else {
                    DatabaseReference lastMessageTimeRef = FirebaseDatabase.getInstance()
                            .getReference("Conversations")
                            .child(key)
                            .child("lastMessageTime");
                    lastMessageTimeRef.removeEventListener(entry.getValue());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing room meta listener: " + e.getMessage());
            }
        }
        
        // ✅ Clear all polling flags
        messagePollInFlight.clear();
        
        firebaseListeners.clear();
        messageListeners.clear();
        roomListeners.clear();
        lastMessageTimestamps.clear();
        lastRoomUpdateTimes.clear();

        // Cancel all polling futures
        for (Map.Entry<String, ScheduledFuture<?>> entry : messagePollingFutures.entrySet()) {
            try {
                ScheduledFuture<?> f = entry.getValue();
                if (f != null && !f.isCancelled()) f.cancel(true);
            } catch (Exception ignored) {}
        }
        messagePollingFutures.clear();

        if (chatRoomsPollingFuture != null && !chatRoomsPollingFuture.isCancelled()) {
            chatRoomsPollingFuture.cancel(true);
            chatRoomsPollingFuture = null;
        }
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
