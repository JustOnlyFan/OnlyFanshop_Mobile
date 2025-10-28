package com.example.onlyfanshop.model.chat;

import android.os.Build;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    
    @SerializedName("epochMillis")
    private Long epochMillis;
    
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
            this.originalTimestamp = timestamp;
            // Format using device local timezone from epoch millis
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            this.timestamp = zdt.format(formatter);
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

    public Long getEpochMillis() {
        return epochMillis;
    }

    public void setEpochMillis(Long epochMillis) {
        this.epochMillis = epochMillis;
        if (epochMillis != null) {
            setTimestampFromLong(epochMillis);
        }
    }

    public void setTime(String time) {
        this.time = time;
    }

    // Helper method to get formatted time
    public String getTime() {
        // Priority: use epochMillis from backend if available
        if (epochMillis != null && epochMillis > 0) {
            try {
                java.time.Instant instant = java.time.Instant.ofEpochMilli(epochMillis);
                java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                return zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                // fallthrough
            }
        }
        
        if (timestamp != null) {
            if (timestamp.matches("\\d{2}:\\d{2}")) {
                return timestamp;
            }
            try {
                long val = Long.parseLong(timestamp);
                java.time.Instant instant = java.time.Instant.ofEpochMilli(val);
                java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                return zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                return timestamp;
            }
        }
        return "";
    }
}

