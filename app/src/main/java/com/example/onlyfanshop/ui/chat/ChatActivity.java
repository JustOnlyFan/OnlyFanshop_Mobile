package com.example.onlyfanshop.ui.chat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onlyfanshop.R;
import com.example.onlyfanshop.model.chat.ChatMessage;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private EditText messageEditText;
    private Button sendButton;
    private FirebaseFirestore db;
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private final List<ChatMessage> chatMessageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        db = FirebaseFirestore.getInstance();

        messageAdapter = new MessageAdapter(chatMessageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                ChatMessage chatMessage = new ChatMessage(messageText, "User1", new Date().getTime());
                db.collection("chat_messages")
                        .add(chatMessage)
                        .addOnSuccessListener(documentReference -> messageEditText.setText(""))
                        .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Gửi thất bại!", Toast.LENGTH_SHORT).show());
            }
        });

        listenForMessages();
    }

    private void listenForMessages() {
        db.collection("chat_messages")
                .orderBy("messageTime", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || value == null) return;
                        chatMessageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            chatMessageList.add(message);
                        }
                        messageAdapter.notifyDataSetChanged();
                        if (!chatMessageList.isEmpty()) chatRecyclerView.scrollToPosition(chatMessageList.size() - 1);
                    }
                });
    }
}





