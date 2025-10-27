package com.example.onlyfanshop.model.chat;

import android.os.Build;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private String timestamp;
    private String attachmentUrl;
    private String attachmentType;
    private String replyToMessageId;
    private boolean isRead;
    private String roomId;
    private boolean isMe;
    private int avatarRes;

    private String time;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderId, String senderName, String message,
                       String timestamp, String attachmentUrl, String attachmentType,
                       String replyToMessageId, boolean isRead, String roomId, boolean isMe, int avatarRes) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = attachmentType;
        this.replyToMessageId = replyToMessageId;
        this.isRead = isRead;
        this.roomId = roomId;
        this.isMe = isMe;
        this.avatarRes = avatarRes;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    // Method to handle Long timestamp from Firebase
    public void setTimestampFromLong(Long timestamp) {
        if (timestamp != null) {
            // Store the original timestamp for sorting
            this.originalTimestamp = timestamp;
            
            // Convert Unix timestamp (milliseconds) to LocalDateTime
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofEpochSecond(
                timestamp / 1000, 0, java.time.ZoneOffset.UTC);
            
            // Format to readable time string
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            this.timestamp = dateTime.format(formatter);
        }
    }
    
    // Add field to store original timestamp for sorting
    private long originalTimestamp = 0;
    
    public long getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    public void setOriginalTimestamp(long originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public int getAvatarRes() {
        return avatarRes;
    }

    public void setAvatarRes(int avatarRes) {
        this.avatarRes = avatarRes;
    }

    public void setTime(String time) {
        this.time = time;
    }

    // Helper method to get formatted time
    public String getTime() {
        if (timestamp != null) {
            // If timestamp is already formatted (HH:mm), return it
            if (timestamp.matches("\\d{2}:\\d{2}")) {
                return timestamp;
            }
            
            // If timestamp is a Unix timestamp (long number), convert it
            try {
                long timestampLong = Long.parseLong(timestamp);
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofEpochSecond(
                    timestampLong / 1000, 0, java.time.ZoneOffset.UTC);
                return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            } catch (NumberFormatException e) {
                // If it's not a number, try to parse as LocalDateTime
                try {
                    java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timestamp);
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                } catch (Exception ex) {
                    return timestamp; // Return raw string if all parsing fails
                }
            }
        }
        return "";
    }
}

