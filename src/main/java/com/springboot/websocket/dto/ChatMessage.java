package com.springboot.websocket.dto;

public class ChatMessage {
    private String content;
    private String sender;
    private String receiver;
    private String groupId;
    private MessageType type;

    public enum MessageType {
        CHAT,           // Tin nhắn chat thông thường
        JOIN,          // Thông báo có người tham gia
        LEAVE,         // Thông báo có người rời đi
        USERS_LIST,    // Cập nhật danh sách người dùng online
        GROUP_CREATED  // Thông báo tạo nhóm mới
    }

    // Getters và Setters
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

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}