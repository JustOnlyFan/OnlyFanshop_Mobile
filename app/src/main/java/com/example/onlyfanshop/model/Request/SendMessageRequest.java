package com.example.onlyfanshop.model.Request;

public class SendMessageRequest {
    private String roomId;
    private String message;
    private String attachmentUrl;
    private String attachmentType;
    private String replyToMessageId;

    public SendMessageRequest() {}

    public SendMessageRequest(String roomId, String message, String attachmentUrl, 
                            String attachmentType, String replyToMessageId) {
        this.roomId = roomId;
        this.message = message;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = attachmentType;
        this.replyToMessageId = replyToMessageId;
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }
}

