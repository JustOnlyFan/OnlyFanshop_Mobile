package com.example.onlyfanshop.model.chat;

import java.time.LocalDateTime;
import java.util.Map;

public class ChatRoom {
    private String roomId;
    private Map<String, Boolean> participants;
    private String lastMessage;
    private String lastMessageTime;
    private String customerName;
    private String customerAvatar;
    private boolean isOnline;
    private int unreadCount;

    public ChatRoom() {}

    public ChatRoom(String roomId, Map<String, Boolean> participants, String lastMessage, 
                   String lastMessageTime, String customerName, String customerAvatar, 
                   boolean isOnline, int unreadCount) {
        this.roomId = roomId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.customerName = customerName;
        this.customerAvatar = customerAvatar;
        this.isOnline = isOnline;
        this.unreadCount = unreadCount;
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Map<String, Boolean> getParticipants() { return participants; }
    public void setParticipants(Map<String, Boolean> participants) { this.participants = participants; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerAvatar() { return customerAvatar; }
    public void setCustomerAvatar(String customerAvatar) { this.customerAvatar = customerAvatar; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
