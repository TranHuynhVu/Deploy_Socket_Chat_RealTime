package com.springboot.websocket.service;

import com.springboot.websocket.entity.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    private final Map<String, List<Message>> privateMessages = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> groupMessages = new ConcurrentHashMap<>();

    public List<Message> getPrivateMessages(String sender, String receiver) {
        String chatId = getChatId(sender, receiver);
        return new ArrayList<>(privateMessages.getOrDefault(chatId, new ArrayList<>()));
    }

    public List<Message> getGroupMessages(String groupId) {
        return new ArrayList<>(groupMessages.getOrDefault(groupId, new ArrayList<>()));
    }

    public Set<String> getPrivateChats(String username) {
        Set<String> chats = new HashSet<>();
        privateMessages.forEach((chatId, messages) -> {
            if (chatId.contains(username)) {
                String[] users = chatId.split("_");
                String otherUser = users[0].equals(username) ? users[1] : users[0];
                if (!messages.isEmpty()) {
                    chats.add(otherUser);
                }
            }
        });
        return chats;
    }

    public Message createPrivateMessage(String content, String senderUsername, String receiverUsername) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(senderUsername);
        message.setReceiver(receiverUsername);
        message.setTimestamp(LocalDateTime.now());
        
        String chatId = getChatId(senderUsername, receiverUsername);
        List<Message> messages = privateMessages.computeIfAbsent(chatId, k -> new ArrayList<>());
        synchronized (messages) {
            messages.add(message);
        }
        return message;
    }

    public Message createGroupMessage(String content, String senderUsername, String groupId) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(senderUsername);
        message.setGroupId(groupId);
        message.setTimestamp(LocalDateTime.now());
        
        List<Message> messages = groupMessages.computeIfAbsent(groupId, k -> new ArrayList<>());
        synchronized (messages) {
            messages.add(message);
        }
        return message;
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? 
               user1 + "_" + user2 : 
               user2 + "_" + user1;
    }
}