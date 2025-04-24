package com.springboot.websocket;

import com.springboot.websocket.entity.Message;
import com.springboot.websocket.service.ChatService;
import com.springboot.websocket.service.UserManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebsocketApplicationTests {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserManager userManager;

    @Test
    void contextLoads() {
    }

    @Test
    void testPrivateMessageCreation() {
        String content = "Hello!";
        String sender = "user1";
        String receiver = "user2";

        Message message = chatService.createPrivateMessage(content, sender, receiver);

        assertNotNull(message);
        assertEquals(content, message.getContent());
        assertEquals(sender, message.getSender());
        assertEquals(receiver, message.getReceiver());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testUserManagerOperations() {
        String username = "testUser";
        
        userManager.addUser(username);
        assertTrue(userManager.isUserOnline(username));
        
        userManager.removeUser(username);
        assertFalse(userManager.isUserOnline(username));
    }

    @Test
    void testPrivateMessageRetrieval() {
        String sender = "user1";
        String receiver = "user2";
        String content = "Test message";

        chatService.createPrivateMessage(content, sender, receiver);
        var messages = chatService.getPrivateMessages(sender, receiver);

        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> 
            m.getContent().equals(content) && 
            m.getSender().equals(sender) && 
            m.getReceiver().equals(receiver)
        ));
    }
}
