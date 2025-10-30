package com.example.onlyfanshop.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.ChatAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ChatApi;
import com.example.onlyfanshop.model.chat.ChatMessage;
import com.example.onlyfanshop.service.ChatService;
import com.example.onlyfanshop.service.RealtimeChatService;
import com.example.onlyfanshop.utils.AppPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText messageInput;
    private ImageButton sendButton;

    private ChatService chatService;
    private RealtimeChatService realtimeChatService;
    private String roomId;
    private String currentUserId;

    private LinearLayoutManager layoutManager;
    private ImageView backButton;
    private TextView chatTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // ✅ Initialize UI components first (fast operations)
        initViews();
        setupRecyclerView();
        setupBackButton();
        setupSendButton();

        // ✅ Initialize services first (fast operation)
        initServices();
        
        // Defer network calls until onStart to avoid firing before UI is visible
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start loading messages and listening when activity becomes visible
        new Thread(() -> {
            try {
                ensureFirebaseReadyThen(() -> {
                    runOnUiThread(() -> {
                        loadMessages();
                        setupFirebaseListener();
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in background initialization: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Failed to initialize chat", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void ensureFirebaseReadyThen(Runnable onReady) {
        try {
            if (roomId == null) { onReady.run(); return; }
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser u = auth.getCurrentUser();
            if (u != null) {
                writeParticipantAndThen(u.getUid(), onReady);
                return;
            }
            auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                    FirebaseUser cu = firebaseAuth.getCurrentUser();
                    if (cu != null) {
                        firebaseAuth.removeAuthStateListener(this);
                        writeParticipantAndThen(cu.getUid(), onReady);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "ensureFirebaseReadyThen error: " + e.getMessage());
            onReady.run();
        }
    }

    private void writeParticipantAndThen(String uid, Runnable onReady) {
        try {
            DatabaseReference participantsRef = FirebaseDatabase.getInstance()
                    .getReference("Conversations")
                    .child(roomId)
                    .child("participants");
            participantsRef.child(uid).setValue(true)
                    .addOnSuccessListener(r -> {
                        Log.d(TAG, "Joined participants for room: " + roomId + " as " + uid);
                        onReady.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Join participants failed: " + e.getMessage());
                        onReady.run();
                    });
        } catch (Exception e) {
            Log.e(TAG, "writeParticipantAndThen error: " + e.getMessage());
            onReady.run();
        }
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backBtn);
        chatTitle = findViewById(R.id.chatName);
        messageList = new ArrayList<>();
        
        // Get room ID from intent
        roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) {
            Toast.makeText(this, "Room ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set chat title based on user role
        if (chatTitle != null) {
            String userRole = AppPreferences.getUserRole(this);
            Log.d(TAG, "User Role: " + userRole + ", Room ID: " + roomId);
            
            if ("ADMIN".equals(userRole)) {
                // For admin, extract customer name from room ID
                String customerName = extractCustomerNameFromRoomId(roomId);
                Log.d(TAG, "Extracted customer name: " + customerName);
                chatTitle.setText(customerName + " Chat");
            } else {
                // For customer, show admin name
                chatTitle.setText("OnlyFanShop Chat");
            }
        }
    }

    private void initServices() {
        ChatApi chatApi = ApiClient.getPrivateClient(this).create(ChatApi.class);
        chatService = new ChatService(chatApi, this);
        realtimeChatService = RealtimeChatService.getInstance(this, chatApi);
        currentUserId = AppPreferences.getUserId(this);
        Log.d(TAG, "Current User ID from AppPreferences: " + currentUserId);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // ✅ Tin nhắn mới luôn ở cuối
        
        // Optimize RecyclerView performance
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setItemAnimator(null); // Disable default animations for better performance
        
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add haptic feedback for better UX
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    
                    // Smooth back navigation with custom animation
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            });
        }
    }

    private void loadMessages() {
        chatService.getMessagesForRoom(roomId, new ChatService.MessagesCallback() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Received " + messages.size() + " messages");

                    messageList.clear();
                    messageList.addAll(messages);
                    chatAdapter.notifyDataSetChanged();

                    // ✅ Auto scroll xuống cuối cùng
                    chatRecyclerView.postDelayed(() -> scrollToBottom(true), 150);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading messages: " + error);
                    Toast.makeText(ChatRoomActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupFirebaseListener() {
        // ✅ Setup real-time listener for messages
        realtimeChatService.startListeningForMessages(roomId, newMessage -> {
            runOnUiThread(() -> {
                // ✅ Real-time message processing
                processNewMessage(newMessage);
            });
        });
    }
    
    // ✅ New method to process messages in real-time
    private void processNewMessage(ChatMessage newMessage) {
        // ✅ Check for duplicate messages to prevent duplicates
        boolean isDuplicate = false;
        int existingIndex = -1;
        
        for (int i = 0; i < messageList.size(); i++) {
            ChatMessage existingMessage = messageList.get(i);
            
            // ✅ Kiểm tra duplicate bằng messageId thật
            if (existingMessage.getMessageId() != null && 
                existingMessage.getMessageId().equals(newMessage.getMessageId())) {
                isDuplicate = true;
                break;
            }
            
            // ✅ Kiểm tra tin nhắn tạm thời cần thay thế
            if (existingMessage.getMessageId() != null && 
                existingMessage.getMessageId().startsWith("temp_") &&
                existingMessage.getMessage().equals(newMessage.getMessage()) &&
                existingMessage.getSenderId().equals(newMessage.getSenderId())) {
                existingIndex = i;
                break;
            }
        }
        
        if (isDuplicate) {
            Log.d(TAG, "Skipped duplicate message: " + newMessage.getMessage());
        } else if (existingIndex >= 0) {
            // ✅ Thay thế tin nhắn tạm thời bằng tin nhắn thật
            messageList.set(existingIndex, newMessage);
            chatAdapter.notifyItemChanged(existingIndex);
            Log.d(TAG, "Replaced temporary message with real message: " + newMessage.getMessage());
        } else {
            // ✅ Thêm tin nhắn mới và sắp xếp theo thời gian
            messageList.add(newMessage);
            // ✅ Sắp xếp tin nhắn theo timestamp để đảm bảo thứ tự đúng
            messageList.sort((m1, m2) -> Long.compare(m1.getOriginalTimestamp(), m2.getOriginalTimestamp()));
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(true);
            Log.d(TAG, "Added new message: " + newMessage.getMessage());
        }
        
        // ✅ Force refresh UI for real-time updates
        chatRecyclerView.post(() -> {
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(true);
        });
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (messageText.isEmpty()) return;

            // Add haptic feedback for better UX
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

            // ✅ Clear input immediately so user can type next message
            messageInput.setText("");

            // ✅ Disable send button temporarily để tránh spam
            sendButton.setEnabled(false);
            sendButton.setAlpha(0.5f);

            // ✅ Gửi tin nhắn
            sendMessage(messageText);
        });
    }

    private void sendMessage(String messageText) {
        // ✅ Tạo tin nhắn tạm thời để hiển thị ngay lập tức
        ChatMessage tempMessage = new ChatMessage();
        tempMessage.setMessageId("temp_" + System.currentTimeMillis()); // Temporary ID
        tempMessage.setSenderId(currentUserId);
        tempMessage.setSenderName(AppPreferences.getUsername(this));
        tempMessage.setMessage(messageText);
        long currentTime = System.currentTimeMillis();
        tempMessage.setTimestampFromLong(currentTime); // This will set both timestamp and originalTimestamp
        tempMessage.setMe(true);
        tempMessage.setRoomId(roomId);
        tempMessage.setRead(false);
        
        // ✅ Thêm tin nhắn vào UI ngay lập tức
        messageList.add(tempMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        
        // ✅ Force refresh UI ngay lập tức với multiple attempts
        chatRecyclerView.post(() -> {
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(true);
        });
        
        // ✅ Additional refresh attempts for real-time feel
        chatRecyclerView.postDelayed(() -> {
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(true);
        }, 10);
        
        chatRecyclerView.postDelayed(() -> {
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(true);
        }, 50);
        
        Log.d(TAG, "Added temporary message to UI: " + messageText);
        
        // ✅ Gửi tin nhắn lên server
        chatService.sendMessage(roomId, messageText, new ChatService.SendMessageCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Message sent successfully to server");
                    // ✅ Enable lại send button
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    // Firebase listener sẽ cập nhật tin nhắn với ID thật từ server
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error sending message: " + error);
                    // ✅ Xóa tin nhắn tạm thời nếu gửi thất bại
                    int index = messageList.indexOf(tempMessage);
                    if (index >= 0) {
                        messageList.remove(index);
                        chatAdapter.notifyItemRemoved(index);
                    }
                    // ✅ Enable lại send button
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    Toast.makeText(ChatRoomActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scrollToBottom(boolean smooth) {
        if (!messageList.isEmpty()) {
            int lastPos = messageList.size() - 1;
            chatRecyclerView.post(() -> {
                if (smooth)
                    chatRecyclerView.smoothScrollToPosition(lastPos);
                else
                    chatRecyclerView.scrollToPosition(lastPos);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatService.markMessagesAsRead(roomId, new ChatService.SendMessageCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Messages marked as read");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error marking messages as read: " + error);
            }
        });
    }
    
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Stop real-time listening when activity is destroyed
        if (realtimeChatService != null && roomId != null) {
            realtimeChatService.stopListeningForMessages(roomId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listeners/pollers when screen is no longer visible
        if (realtimeChatService != null && roomId != null) {
            realtimeChatService.stopListeningForMessages(roomId);
        }
    }
    
    private String extractCustomerNameFromRoomId(String roomId) {
        try {
            // Room ID format: chatRoom_username_userId
            // Example: chatRoom_NTT_4
            if (roomId != null && roomId.startsWith("chatRoom_")) {
                String[] parts = roomId.split("_");
                if (parts.length >= 3) {
                    // parts[1] is the username
                    return parts[1];
                }
            }
            return "Customer";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting customer name from room ID: " + e.getMessage());
            return "Customer";
        }
    }
}
