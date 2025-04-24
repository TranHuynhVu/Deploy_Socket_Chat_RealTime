package com.springboot.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    //Đăng ký endpoint WebSocket mà client sẽ sử dụng để kết nối
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

    /**
     * Cấu hình message broker để định tuyến tin nhắn
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // cho các tin nhắn gửi đến server xử lý
        // tin nhắn gửi đến server sẽ được định tuyến đến các controller và bắt đầu bằng "/app"
        registry.setApplicationDestinationPrefixes("/app");

        // cho tin nhắn broadcast và private
        // /topic gửi broadcast đến tất cả client (group chat)
        // /user gửi đến một client cụ thể (private chat)
        registry.enableSimpleBroker("/topic", "/user");
        
        // cho tin nhắn private
        // Khi gửi về /user/{username}/private, STOMP sẽ hiểu và định tuyến đến user cụ thể.
        registry.setUserDestinationPrefix("/user");
    }
}