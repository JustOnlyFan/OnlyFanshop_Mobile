package com.example.onlyfanshop.model.chat;

import androidx.annotation.Keep;

/**
 * MessageModel theo chuẩn Firebase Chat Architecture
 * Lưu trữ tin nhắn trong Firebase Realtime Database
 * Path: chats/{messageId}
 */
@Keep
public class MessageModel {
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private String messageId;

    public MessageModel() {
        // Default constructor required for Firebase
    }

    public MessageModel(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public MessageModel(String messageId, String senderId, String receiverId, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
