package com.example.onlyfanshop.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.chat.Message;
import com.example.onlyfanshop.model.chat.MessageModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Response;
import com.android.volley.VolleyError;

public class ChatRoomActivity extends AppCompatActivity {

    private String conversationId;
    private RecyclerView recyclerView;
    private EditText edtMessage;
    private ImageButton btnSend;
    private TextView tvHeader;
    private final List<Message> messages = new ArrayList<>();
    private MessagesAdapter adapter;
    private DatabaseReference messagesRef;
    private String selfUserId;
    private boolean isAdminView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        conversationId = getIntent().getStringExtra("conversationId");
        recyclerView = findViewById(R.id.recyclerMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        tvHeader = findViewById(R.id.tvHeader);
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());
        String customerName = getIntent().getStringExtra("customerName");
        if (tvHeader != null && customerName != null) {
            tvHeader.setText(customerName);
            Log.d("ChatRoomActivity", "Customer name from intent: " + customerName);
        } else {
            Log.w("ChatRoomActivity", "No customer name provided in intent");
        }

        isAdminView = getIntent().getBooleanExtra("isAdminView", false);
        if (isAdminView) {
            selfUserId = "admin_uid";
            Log.d("ChatRoomActivity", "AdminChatRoomActivity mode: selfUserId=admin_uid");
        } else {
            selfUserId = FirebaseAuth.getInstance().getUid();
            if (selfUserId == null) {
                // Fallback for non-signed firebase user
                selfUserId = "uid";
                Log.d("ChatRoomActivity", "No Firebase UID, using fallback 'uid' as selfUserId");
            } else {
                Log.d("ChatRoomActivity", "Firebase UID: " + selfUserId);
            }
        }

        // Normalize conversationId to the canonical format "{adminUid}_{customerUid}"
        conversationId = normalizeConversationId(conversationId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessagesAdapter(messages, selfUserId, isAdminView);
        recyclerView.setAdapter(adapter);

        if (conversationId != null) {
            Log.d("ChatRoomActivity", "Connecting to Firebase with conversationId: " + conversationId);
            
            // Extract admin/customer from normalized conversationId
            String[] parts = conversationId != null ? conversationId.split("_") : new String[0];
            String adminUid = parts.length >= 1 ? parts[0] : "admin_uid";
            String customerUid = parts.length >= 2 ? parts[1] : selfUserId;
            Log.d("ChatRoomActivity", "selfUserId: " + selfUserId + ", adminUid: " + adminUid + ", customerUid: " + customerUid);
            
            // Null checks
            if (selfUserId == null || otherUserId == null) {
                Log.e("ChatRoomActivity", "selfUserId or otherUserId is null");
                return;
            }
            
            // Load tin nhắn từ Firebase Realtime Database theo conversationId cụ thể
            DatabaseReference conversationMessagesRef = FirebaseDatabase.getInstance()
                .getReference("conversations")
                .child(conversationId)
                .child("messages");
                
            Log.d("ChatRoomActivity", "Loading messages for conversation: " + conversationId);
            
            conversationMessagesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        messages.clear();
                        Log.d("ChatRoomActivity", "Processing " + snapshot.getChildrenCount() + " messages for conversation: " + conversationId);
                        
                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                MessageModel messageModel = data.getValue(MessageModel.class);
                                Log.d("ChatRoomActivity", "Processing message: " + data.getKey());
                                Log.d("ChatRoomActivity", "  - senderId: " + (messageModel != null ? messageModel.getSenderId() : "null"));
                                Log.d("ChatRoomActivity", "  - receiverId: " + (messageModel != null ? messageModel.getReceiverId() : "null"));
                                Log.d("ChatRoomActivity", "  - message: " + (messageModel != null ? messageModel.getMessage() : "null"));
                                
                                if (messageModel != null && 
                                    messageModel.getSenderId() != null && 
                                    messageModel.getReceiverId() != null &&
                                    !isSampleMessage(messageModel.getMessage())) {
                                    
                                    Log.d("ChatRoomActivity", "Message belongs to this conversation - adding to list");
                                    
                                    // Convert MessageModel to Message for compatibility
                                    String messageText = messageModel.getMessage() != null ? messageModel.getMessage() : "";
                                    String senderName;
                                    
                                    if (messageModel.getSenderId().equals(selfUserId)) {
                                        // Tin nhắn từ user hiện tại
                                        senderName = getUsernameFromToken();
                                    } else {
                                        // Tin nhắn từ user khác - lấy username thật từ database
                                        senderName = getRealUsernameFromSenderId(messageModel.getSenderId());
                                    }
                                    
                                    Message message = new Message(
                                        data.getKey(),
                                        conversationId,
                                        messageModel.getSenderId(),
                                        senderName,
                                        messageText,
                                        messageModel.getTimestamp()
                                    );
                                    messages.add(message);
                                }
                            } catch (Exception e) {
                                Log.e("ChatRoomActivity", "Error processing message: " + e.getMessage());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            recyclerView.scrollToPosition(messages.size() - 1);
                        }
                        Log.d("ChatRoomActivity", "Loaded " + messages.size() + " messages for conversation: " + conversationId);
                    } catch (Exception e) {
                        Log.e("ChatRoomActivity", "Error in onDataChange: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatRoomActivity", "Failed to load messages: " + error.getMessage());
                }
            });

            // Ensure conversation exists in conversations collection
            ensureConversationExists();
        } else {
            Log.w("ChatRoomActivity", "No conversationId provided, cannot load messages");
        }

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = edtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Log.w("ChatRoomActivity", "Attempted to send empty message");
            return;
        }
        
        String uid = selfUserId;
        if (conversationId == null) {
            Log.e("ChatRoomActivity", "Cannot send message: conversationId is null");
            return;
        }
        
        // Get real username from JWT token
        String realUsername = getUsernameFromToken();
        Log.d("ChatRoomActivity", "Sending message: " + text + " from user: " + uid + " with username: " + realUsername);
        
        // Extract receiver ID from normalized conversationId
        String[] parts = conversationId != null ? conversationId.split("_") : new String[0];
        String adminUid = parts.length >= 1 ? parts[0] : "admin_uid";
        String customerUid = parts.length >= 2 ? parts[1] : selfUserId;
        String receiverId = isAdminView ? customerUid : adminUid;
        
        // Lưu tin nhắn vào Firebase Realtime Database theo conversationId cụ thể
        DatabaseReference conversationMessagesRef = FirebaseDatabase.getInstance()
            .getReference("conversations")
            .child(conversationId)
            .child("messages");
            
        String messageId = conversationMessagesRef.push().getKey();
        long now = System.currentTimeMillis();
        
        MessageModel messageModel = new MessageModel(
                uid,
                receiverId,
                text,
                now
        );
        
        if (messageId != null) {
            conversationMessagesRef.child(messageId).setValue(messageModel)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatRoomActivity", "Message saved to Firebase successfully: " + messageId + " in conversation: " + conversationId);
                    // Sau khi lưu thành công, gửi thông báo FCM
                    sendNotification(receiverId, realUsername, text);
                    // Update conversation with full data
                    updateConversationData(text, now);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRoomActivity", "Failed to save message: " + e.getMessage());
                });
        } else {
            Log.e("ChatRoomActivity", "Failed to generate message ID");
        }
        
        edtMessage.setText("");
    }

    private String getUsernameFromToken() {
        try {
            // Get JWT token from SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String token = prefs.getString("jwt_token", null);
            
            if (token == null || token.isEmpty()) {
                Log.w("ChatRoomActivity", "No JWT token found, using default username");
                return "User";
            }
            
            // Decode JWT token to get username
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                Log.w("ChatRoomActivity", "Invalid JWT token format");
                return "User";
            }
            
            // Decode payload (base64)
            String payload = parts[1];
            // Add padding if needed
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            byte[] decodedBytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
            String payloadJson = new String(decodedBytes);
            
            // Parse JSON to get username
            org.json.JSONObject jsonObject = new org.json.JSONObject(payloadJson);
            String username = jsonObject.optString("username", "User");
            
            Log.d("ChatRoomActivity", "Decoded username from JWT: " + username);
            return username;
            
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error decoding JWT token: " + e.getMessage());
            return "User";
        }
    }
    
    private String getRealUsernameFromSenderId(String senderId) {
        try {
            // Lấy username thật từ Firebase Database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
            userRef.child("username").get().addOnSuccessListener(snapshot -> {
                String username = snapshot.getValue(String.class);
                if (username != null && !username.isEmpty()) {
                    Log.d("ChatRoomActivity", "Real username for " + senderId + ": " + username);
                } else {
                    Log.w("ChatRoomActivity", "No username found for " + senderId);
                }
            }).addOnFailureListener(e -> {
                Log.e("ChatRoomActivity", "Failed to get username for " + senderId + ": " + e.getMessage());
            });
            
            // Trả về tên tạm thời, sẽ được cập nhật khi có dữ liệu từ Firebase
            return "Customer";
            
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error getting real username: " + e.getMessage());
            return "Customer";
        }
    }
    
    private String getRealCustomerName(String customerId) {
        try {
            // Nếu customerId là "uid" (generic), sử dụng tên từ JWT token
            if (customerId == null || customerId.equals("uid") || customerId.equals("admin_uid")) {
                String username = getUsernameFromToken();
                Log.d("ChatRoomActivity", "Using JWT username for customer: " + username);
                return username;
            }
            
            // Nếu customerId là Firebase UID thật, lấy từ database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(customerId);
            userRef.child("username").get().addOnSuccessListener(snapshot -> {
                String username = snapshot.getValue(String.class);
                if (username != null && !username.isEmpty()) {
                    Log.d("ChatRoomActivity", "Real customer name for " + customerId + ": " + username);
                } else {
                    Log.w("ChatRoomActivity", "No username found for customer " + customerId);
                }
            }).addOnFailureListener(e -> {
                Log.e("ChatRoomActivity", "Failed to get customer username for " + customerId + ": " + e.getMessage());
            });
            
            // Trả về tên tạm thời, sẽ được cập nhật khi có dữ liệu từ Firebase
            return "Customer " + customerId;
            
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error getting real customer name: " + e.getMessage());
            return "Customer " + customerId;
        }
    }
    
    private boolean isSampleMessage(String message) {
        if (message == null || message.isEmpty()) {
            return true;
        }
        
        // Chỉ lọc tin nhắn thực sự là mẫu, không lọc tin nhắn thật
        // Không lọc "xin chao vì đây có thể là tin nhắn thật
        String[] sampleMessages = {
            "test",
            "sample", 
            "demo",
            "uh huh",
            "ji",
            "jl"
        };
        
        String lowerMessage = message.toLowerCase().trim();
        for (String sample : sampleMessages) {
            if (lowerMessage.equals(sample.toLowerCase())) {
                Log.d("ChatRoomActivity", "Filtering out sample message: " + message);
                return true;
            }
        }
        
        return false;
    }

    private void updateConversationData(String lastMessage, long timestamp) {
        if (conversationId == null) return;
        
        Log.d("ChatRoomActivity", "Updating conversation data: " + conversationId);
        
        // Extract customer and admin IDs from normalized conversationId
        String[] parts = conversationId != null ? conversationId.split("_") : new String[0];
        String adminId = parts.length >= 1 ? parts[0] : "admin_uid";
        String customerId = parts.length >= 2 ? parts[1] : selfUserId;
        
        // Get customer name - should be the actual customer's name, not admin's name
        String customerName = getRealCustomerName(customerId);
        Log.d("ChatRoomActivity", "Final customer name: " + customerName);
        
        // Update only specific fields, not the entire conversation object
        DatabaseReference conversationRef = FirebaseDatabase.getInstance()
            .getReference("conversations")
            .child(conversationId);
            
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("timestamp", timestamp);
        
        conversationRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> Log.d("ChatRoomActivity", "Conversation updated successfully"))
            .addOnFailureListener(e -> Log.e("ChatRoomActivity", "Failed to update conversation: " + e.getMessage()));
    }

    private void syncMessageToDatabase(Message message) {
        // Sync message to MySQL database via backend API
        Log.d("ChatRoomActivity", "Syncing message to database: " + message.getText());
        
        try {
            // Get JWT token for authentication
            android.content.SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String token = prefs.getString("jwt_token", null);
            
            if (token == null || token.isEmpty()) {
                Log.w("ChatRoomActivity", "No JWT token found, cannot sync to database");
                return;
            }
            
            // Extract receiver ID (admin)
            String[] parts = conversationId.split("_");
            String receiverId = parts.length > 0 ? parts[0] : "admin_uid";
            
            // TODO: Implement backend API call to sync message
            // POST /api/chat/sync-message
            // Headers: Authorization: Bearer {token}
            // Body: { senderId, receiverId, message }
            Log.d("ChatRoomActivity", "Backend sync not implemented yet");
            Log.d("ChatRoomActivity", "  - senderId: " + message.getSenderId());
            Log.d("ChatRoomActivity", "  - receiverId: " + receiverId);
            Log.d("ChatRoomActivity", "  - message: " + message.getText());
            
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error syncing message to database: " + e.getMessage());
        }
    }
    
    private void sendNotification(String receiverId, String senderName, String messageText) {
        try {
            // Lấy FCM token của người nhận từ database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
            userRef.child("fcmToken").get().addOnSuccessListener(snapshot -> {
                String receiverToken = snapshot.getValue(String.class);
                if (receiverToken != null && !receiverToken.isEmpty()) {
                    sendFCMNotification(receiverToken, senderName, messageText);
                } else {
                    Log.w("ChatRoomActivity", "No FCM token found for receiver: " + receiverId);
                }
            }).addOnFailureListener(e -> {
                Log.e("ChatRoomActivity", "Failed to get FCM token: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error sending notification: " + e.getMessage());
        }
    }
    
    private void sendFCMNotification(String receiverToken, String senderName, String messageText) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("to", receiverToken);

            JSONObject data = new JSONObject();
            data.put("title", senderName);
            data.put("body", messageText);

            notification.put("notification", data);

            JsonObjectRequest request = new JsonObjectRequest(
                "https://fcm.googleapis.com/fcm/send", 
                notification,
                response -> Log.d("FCM", "Notification sent successfully"),
                error -> Log.e("FCM", "Error sending notification: " + error.getMessage())
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=YOUR_SERVER_KEY"); // Thay YOUR_SERVER_KEY bằng key thực
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error creating FCM request: " + e.getMessage());
        }
    }

    private void ensureConversationExists() {
        if (conversationId == null) return;
        
        Log.d("ChatRoomActivity", "Ensuring conversation exists: " + conversationId);
        DatabaseReference conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        DatabaseReference convRef = conversationsRef.child(conversationId);
        
        convRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Log.d("ChatRoomActivity", "Creating conversation: " + conversationId);
                // Extract customer and admin IDs from normalized conversationId
                String[] parts = conversationId != null ? conversationId.split("_") : new String[0];
                String adminId = parts.length >= 1 ? parts[0] : "admin_uid";
                String customerId = parts.length >= 2 ? parts[1] : selfUserId;
                
                // Get customer name - should be the actual customer's name, not admin's name
                String customerName = getRealCustomerName(customerId);
                Log.d("ChatRoomActivity", "Final customer name: " + customerName);
                
                // Create conversation object
                com.example.onlyfanshop.model.chat.Conversation conv = new com.example.onlyfanshop.model.chat.Conversation(
                        conversationId,
                        customerId,
                        adminId,
                        customerName, // Use real customer name
                        "Admin",
                        "Conversation started",
                        System.currentTimeMillis()
                );

                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("id", conv.getId());
                conversationData.put("customerId", conv.getCustomerId());
                conversationData.put("adminId", conv.getAdminId());
                conversationData.put("customerName", conv.getCustomerName());
                conversationData.put("adminName", conv.getAdminName());
                conversationData.put("lastMessage", conv.getLastMessage());
                conversationData.put("lastTimestamp", conv.getLastTimestamp());
                conversationData.put("messages", new HashMap<String, Object>());
                
                convRef.setValue(conversationData)
                    .addOnSuccessListener(aVoid -> Log.d("ChatRoomActivity", "Conversation created successfully with messages node"))
                    .addOnFailureListener(e -> Log.e("ChatRoomActivity", "Failed to create conversation: " + e.getMessage()));
            } else {
                Log.d("ChatRoomActivity", "Conversation already exists");
            }
        }).addOnFailureListener(e -> {
            Log.e("ChatRoomActivity", "Failed to check conversation existence: " + e.getMessage());
        });
    }

    private String normalizeConversationId(String original) {
        try {
            if (original == null || original.trim().isEmpty()) return original;
            String adminConst = getString(R.string.admin_uid) != null ? getString(R.string.admin_uid) : "admin_uid";
            String[] parts = original.split("_");
            String foundAdmin = null;
            String foundCustomer = null;
            for (String p : parts) {
                if (p == null || p.isEmpty()) continue;
                if (p.equals(adminConst)) {
                    foundAdmin = p;
                } else {
                    foundCustomer = p;
                }
            }
            if (foundAdmin == null) {
                // If admin part is missing, assume first token is admin
                foundAdmin = parts[0];
            }
            if (foundCustomer == null) {
                // Fallback to self user if missing
                foundCustomer = selfUserId != null ? selfUserId : "customer_unknown";
            }
            String normalized = foundAdmin + "_" + foundCustomer;
            if (!normalized.equals(original)) {
                Log.d("ChatRoomActivity", "Normalized conversationId from '" + original + "' to '" + normalized + "'");
            }
            return normalized;
        } catch (Exception e) {
            Log.e("ChatRoomActivity", "Error normalizing conversationId: " + e.getMessage());
            return original;
        }
    }

    private static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {
        private final List<Message> data;
        private final String selfUserId;
        private final boolean isAdminView;
        private final java.util.Map<String, String> reactions = new java.util.HashMap<>();
        MessagesAdapter(List<Message> data, String selfUserId, boolean isAdminView) { this.data = data; this.selfUserId = selfUserId; this.isAdminView = isAdminView; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Message m = data.get(position);
            
            // Determine who sent the message and align accordingly
            boolean isMine;
            if (isAdminView) {
                // In admin view, admin messages should be on the right
                isMine = "admin_uid".equals(m.getSenderId());
            } else {
                // In customer view, messages from the logged-in firebase user go right
                isMine = m.getSenderId() != null && m.getSenderId().equals(selfUserId);
            }
            
            Log.d("MessagesAdapter", "=== MESSAGE DEBUG ===");
            Log.d("MessagesAdapter", "Position: " + position);
            Log.d("MessagesAdapter", "SenderId: '" + m.getSenderId() + "'");
            Log.d("MessagesAdapter", "SelfUserId: '" + selfUserId + "'");
            Log.d("MessagesAdapter", "IsMine: " + isMine);
            Log.d("MessagesAdapter", "Message: '" + m.getText() + "'");
            Log.d("MessagesAdapter", "SenderName: '" + m.getSenderName() + "'");
            
            // No additional forcing needed; alignment is decided by isAdminView + senderId
            
            Log.d("MessagesAdapter", "Final isMine: " + isMine);
            Log.d("MessagesAdapter", "=== END DEBUG ===");
            
            // Show appropriate container
            h.containerIncoming.setVisibility(isMine ? View.GONE : View.VISIBLE);
            h.containerOutgoing.setVisibility(isMine ? View.VISIBLE : View.GONE);
            if (isMine) {
                h.tvOutgoing.setText(m.getText());
                h.tvTimeOutgoing.setText(android.text.format.DateFormat.format("HH:mm", m.getTimestamp()));
                String r = reactions.get(m.getId());
                h.tvReactionOutgoing.setText(r != null ? r : "");
                h.bubbleOutgoing.setOnLongClickListener(v -> { showReactionPopup(v, m); return true; });
            } else {
                h.tvIncoming.setText(m.getText());
                h.tvTimeIncoming.setText(android.text.format.DateFormat.format("HH:mm", m.getTimestamp()));
                String r = reactions.get(m.getId());
                h.tvReactionIncoming.setText(r != null ? r : "");
                h.bubbleIncoming.setOnLongClickListener(v -> { showReactionPopup(v, m); return true; });
            }
        }

        private void showReactionPopup(View anchor, Message m) {
            View content = LayoutInflater.from(anchor.getContext()).inflate(R.layout.view_reaction_bar, null);
            PopupWindow popup = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popup.setElevation(8f);
            View.OnClickListener pick = v -> {
                String emoji = ((TextView) v).getText().toString();
                if ("＋".equals(emoji)) {
                    popup.dismiss();
                    return;
                }
                reactions.put(m.getId(), emoji);
                notifyDataSetChanged();
                popup.dismiss();
            };
            int[] ids = new int[]{R.id.r1,R.id.r2,R.id.r3,R.id.r4,R.id.r5,R.id.r6,R.id.r7};
            for (int id : ids) content.findViewById(id).setOnClickListener(pick);
            int[] loc = new int[2];
            anchor.getLocationOnScreen(loc);
            anchor.post(() -> popup.showAsDropDown(anchor, -anchor.getWidth()/2, -anchor.getHeight()*3));
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ViewGroup containerIncoming, containerOutgoing;
            TextView tvIncoming, tvOutgoing, tvTimeIncoming, tvTimeOutgoing, tvReactionIncoming, tvReactionOutgoing;
            View bubbleIncoming, bubbleOutgoing;
            VH(@NonNull View itemView) {
                super(itemView);
                containerIncoming = itemView.findViewById(R.id.containerIncoming);
                containerOutgoing = itemView.findViewById(R.id.containerOutgoing);
                tvIncoming = itemView.findViewById(R.id.tvIncoming);
                tvOutgoing = itemView.findViewById(R.id.tvOutgoing);
                tvTimeIncoming = itemView.findViewById(R.id.tvTimeIncoming);
                tvTimeOutgoing = itemView.findViewById(R.id.tvTimeOutgoing);
                tvReactionIncoming = itemView.findViewById(R.id.tvReactionIncoming);
                tvReactionOutgoing = itemView.findViewById(R.id.tvReactionOutgoing);
                bubbleIncoming = itemView.findViewById(R.id.bubbleIncoming);
                bubbleOutgoing = itemView.findViewById(R.id.bubbleOutgoing);
            }
        }
    }
}


