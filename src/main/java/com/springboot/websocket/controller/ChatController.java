package com.springboot.websocket.controller;

import com.springboot.websocket.dto.ChatMessage;
import com.springboot.websocket.service.UserManager;
import com.springboot.websocket.service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate; // Dùng để gửi tin nhắn
    private final UserManager userManager;                 // Quản lý người dùng online
    private final ChatService chatService;                // Xử lý lưu trữ tin nhắn

    public ChatController(SimpMessagingTemplate messagingTemplate, UserManager userManager, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.userManager = userManager;
        this.chatService = chatService;
    }

    /**
     * Xử lý tin nhắn công khai gửi đến tất cả người dùng
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    /**
     * Xử lý khi người dùng tham gia chat
     * @return thông báo cho mọi người biết có người mới tham gia
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Lưu username vào session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        userManager.addUser(chatMessage.getSender());
        
        // Gửi danh sách người dùng online cho tất cả
        ChatMessage onlineUsersMessage = new ChatMessage();
        onlineUsersMessage.setType(ChatMessage.MessageType.USERS_LIST);
        onlineUsersMessage.setContent(String.join(",", userManager.getOnlineUsers()));
        messagingTemplate.convertAndSend("/topic/public", onlineUsersMessage);
        
        return chatMessage;
    }

    /**
     * Xử lý tin nhắn riêng tư giữa 2 người dùng
     */
    @MessageMapping("/chat.private")
    public void privateMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        
        // Lưu tin nhắn
        chatService.createPrivateMessage(
            chatMessage.getContent(),
            chatMessage.getSender(),
            chatMessage.getReceiver()
        );

        // Gửi tin nhắn cho người nhận
        messagingTemplate.convertAndSendToUser(
            chatMessage.getReceiver(),
            "/private",
            chatMessage
        );

        // Gửi tin nhắn cho người gửi để hiển thị
        messagingTemplate.convertAndSendToUser(
            chatMessage.getSender(),
            "/private",
            chatMessage
        );
    }

    /**
     * Xử lý tin nhắn trong nhóm chat
     * @param chatMessage tin nhắn chứa ID nhóm
     */
    @MessageMapping("/chat.group")
    public void groupMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        
        // Lưu tin nhắn nhóm vào database
        chatService.createGroupMessage(
            chatMessage.getContent(),
            chatMessage.getSender(),
            chatMessage.getGroupId()
        );

        // Gửi tin nhắn đến tất cả thành viên trong nhóm
        messagingTemplate.convertAndSend(
            "/topic/group/" + chatMessage.getGroupId(),
            chatMessage
        );
    }
    
    /**
     * Thông báo khi tạo nhóm chat mới
     */
    public void notifyGroupCreation(String groupId, String groupName, java.util.Set<String> members) {
        ChatMessage notification = new ChatMessage();
        notification.setType(ChatMessage.MessageType.GROUP_CREATED);
        
        // Tạo thông tin nhóm để gửi cho các thành viên
        java.util.Map<String, Object> groupInfo = new java.util.HashMap<>();
        groupInfo.put("group", java.util.Map.of(
            "id", groupId,
            "name", groupName,
            "type", "group"
        ));
        groupInfo.put("members", members);
        
        try {
            notification.setContent(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(groupInfo));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
        }
        messagingTemplate.convertAndSend("/topic/public", notification);
    }

    /**
     * Xử lý khi người dùng ngắt kết nối
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (username != null) {
            // Xóa người dùng khỏi danh sách online
            userManager.removeUser(username);
            
            // Thông báo người dùng đã rời đi
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);

            // Cập nhật lại danh sách người dùng online
            ChatMessage onlineUsersMessage = new ChatMessage();
            onlineUsersMessage.setType(ChatMessage.MessageType.USERS_LIST);
            onlineUsersMessage.setContent(String.join(",", userManager.getOnlineUsers()));
            messagingTemplate.convertAndSend("/topic/public", onlineUsersMessage);

            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}
