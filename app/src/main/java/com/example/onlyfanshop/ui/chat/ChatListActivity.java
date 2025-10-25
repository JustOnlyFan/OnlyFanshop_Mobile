package com.example.onlyfanshop.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.adapter.ChatRoomAdapter;
import com.example.onlyfanshop.api.ApiClient;
import com.example.onlyfanshop.api.ChatApi;
import com.example.onlyfanshop.model.chat.ChatRoom;
import com.example.onlyfanshop.service.ChatService;
import com.example.onlyfanshop.utils.AppPreferences;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private RecyclerView chatRoomsRecyclerView;
    private ChatRoomAdapter chatRoomAdapter;
    private List<ChatRoom> chatRoomList;
    private List<ChatRoom> filteredChatRoomList;
    
    private ChatService chatService;
    private EditText searchEditText;
    private LinearLayout emptyStateLayout;
    private TextView totalChatsText, unreadChatsText, onlineChatsText;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        initViews();
        initServices();
        setupRecyclerView();
        loadChatRooms();
    }

    private void initViews() {
        chatRoomsRecyclerView = findViewById(R.id.chatRoomsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        totalChatsText = findViewById(R.id.totalChatsText);
        unreadChatsText = findViewById(R.id.unreadChatsText);
        onlineChatsText = findViewById(R.id.onlineChatsText);
        backButton = findViewById(R.id.backButton);
        
        chatRoomList = new ArrayList<>();
        filteredChatRoomList = new ArrayList<>();
        
        setupSearch();
        setupBackButton();
    }

    private void initServices() {
        ChatApi chatApi = ApiClient.getPrivateClient(this).create(ChatApi.class);
        chatService = new ChatService(chatApi, this);
    }

    private void setupRecyclerView() {
        chatRoomAdapter = new ChatRoomAdapter(filteredChatRoomList, new ChatRoomAdapter.OnChatRoomClickListener() {
            @Override
            public void onChatRoomClick(ChatRoom chatRoom) {
                // Add haptic feedback for better UX
                chatRoomsRecyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                
                // Navigate to chat room with smooth custom animation
                Intent intent = new Intent(ChatListActivity.this, ChatRoomActivity.class);
                intent.putExtra("roomId", chatRoom.getRoomId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        
        // Optimize RecyclerView performance
        chatRoomsRecyclerView.setHasFixedSize(true);
        chatRoomsRecyclerView.setItemAnimator(null); // Disable default animations for better performance
        
        chatRoomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRoomsRecyclerView.setAdapter(chatRoomAdapter);
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChatRooms(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupBackButton() {
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
    
    private void filterChatRooms(String query) {
        filteredChatRoomList.clear();
        
        if (query.isEmpty()) {
            filteredChatRoomList.addAll(chatRoomList);
        } else {
            for (ChatRoom chatRoom : chatRoomList) {
                if (chatRoom.getCustomerName().toLowerCase().contains(query.toLowerCase()) ||
                    (chatRoom.getLastMessage() != null && 
                     chatRoom.getLastMessage().toLowerCase().contains(query.toLowerCase()))) {
                    filteredChatRoomList.add(chatRoom);
                }
            }
        }
        
        chatRoomAdapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (filteredChatRoomList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            chatRoomsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            chatRoomsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateStats() {
        totalChatsText.setText(String.valueOf(chatRoomList.size()));
        
        int unreadCount = 0;
        int onlineCount = 0;
        
        for (ChatRoom chatRoom : chatRoomList) {
            unreadCount += chatRoom.getUnreadCount();
            if (chatRoom.isOnline()) {
                onlineCount++;
            }
        }
        
        unreadChatsText.setText(String.valueOf(unreadCount));
        onlineChatsText.setText(String.valueOf(onlineCount));
    }

    private void loadChatRooms() {
        chatService.getChatRooms(new ChatService.ChatRoomsCallback() {
            @Override
            public void onSuccess(List<ChatRoom> chatRooms) {
                runOnUiThread(() -> {
                    chatRoomList.clear();
                    chatRoomList.addAll(chatRooms);
                    
                    // Update filtered list
                    filterChatRooms(searchEditText.getText().toString());
                    updateStats();
                    
                    Log.d(TAG, "Loaded " + chatRooms.size() + " chat rooms");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading chat rooms: " + error);
                    Toast.makeText(ChatListActivity.this, "Failed to load chat rooms", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat rooms when returning to this activity
        loadChatRooms();
    }
    
    @Override
    public void onBackPressed() {
        // Smooth back navigation with custom animation
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
