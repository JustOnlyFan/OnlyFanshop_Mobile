package com.example.onlyfanshop.ui.chat;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Thin wrapper activity to open ChatRoomActivity in admin view mode.
 * Ensures admin perspective where admin messages align right and customer left.
 */
public class AdminChatRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent src = getIntent();
        Intent forward = new Intent(this, ChatRoomActivity.class);
        if (src != null) {
            if (src.hasExtra("conversationId")) {
                forward.putExtra("conversationId", src.getStringExtra("conversationId"));
            }
            if (src.hasExtra("customerName")) {
                forward.putExtra("customerName", src.getStringExtra("customerName"));
            }
        }
        forward.putExtra("isAdminView", true);
        startActivity(forward);
        finish();
    }
}


