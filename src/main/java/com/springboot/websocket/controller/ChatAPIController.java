package com.springboot.websocket.controller;

import com.springboot.websocket.entity.Group;
import com.springboot.websocket.entity.Message;
import com.springboot.websocket.service.ChatService;
import com.springboot.websocket.service.GroupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatAPIController {
    private final ChatService chatService;
    private final GroupService groupService;

    public ChatAPIController(ChatService chatService, GroupService groupService) {
        this.chatService = chatService;
        this.groupService = groupService;
    }

    /**
     * Lấy danh sách các cuộc trò chuyện của người dùng
     */
    @GetMapping("/chats")
    public ResponseEntity<?> getChats(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        List<Map<String, Object>> chats = new ArrayList<>();
        chatService.getPrivateChats(username).forEach(otherUser -> {
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("id", otherUser);
            chatData.put("name", otherUser);
            chatData.put("type", "private");
            chats.add(chatData);
        });

        return ResponseEntity.ok(chats);
    }

    /**
     * Lấy lịch sử tin nhắn riêng tư với một người dùng cụ thể
     */
    @GetMapping("/messages/private/{username}")
    public ResponseEntity<?> getPrivateMessages(@PathVariable String username, HttpSession session) {
        String currentUser = (String) session.getAttribute("username");
        if (currentUser == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        List<Message> messages = chatService.getPrivateMessages(currentUser, username);
        return ResponseEntity.ok(messages);
    }

    /**
     * Lấy lịch sử tin nhắn của một nhóm
     */
    @GetMapping("/messages/group/{groupId}")
    public ResponseEntity<?> getGroupMessages(@PathVariable String groupId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        if (!groupService.isMember(groupId, username)) {
            return ResponseEntity.badRequest().body("Not a member of this group");
        }

        List<Message> messages = chatService.getGroupMessages(groupId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Tạo nhóm chat mới
     * @param request chứa tên nhóm và danh sách thành viên
     */
    @PostMapping("/group/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        String groupName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        List<String> members = (List<String>) request.get("members");

        if (groupName == null || members == null || members.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid group data");
        }

        try {
            Group group = groupService.createGroup(groupName, username, members);
            Map<String, Object> response = new HashMap<>();
            response.put("id", group.getId());
            response.put("name", group.getName());
            response.put("type", "group");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}