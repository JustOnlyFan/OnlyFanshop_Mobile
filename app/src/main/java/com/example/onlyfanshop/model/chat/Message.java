package com.example.onlyfanshop.model.chat;

import androidx.annotation.Keep;

/**
 * Chat message model stored in Firebase Realtime Database.
 * Path: chats/{conversationId}/messages/{messageId}
 */
@Keep
public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String id, String conversationId, String senderId, String senderName, String text, long timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}








