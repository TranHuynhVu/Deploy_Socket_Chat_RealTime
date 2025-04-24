package com.springboot.websocket.entity;

import java.time.LocalDateTime;

public class Message {
    private String id;              // ID duy nhất của tin nhắn
    private String content;         // Nội dung tin nhắn
    private String sender;          // Username của người gửi
    private String receiver;        // Username của người nhận (cho tin nhắn riêng tư)
    private String groupId;         // ID của nhóm (cho tin nhắn nhóm)
    private LocalDateTime timestamp;// Thời điểm gửi tin nhắn

    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
