package com.springboot.websocket.service;

import com.springboot.websocket.entity.User;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public boolean existsByUsername(String username) {
        return users.containsKey(username);
    }

    public User addUser(String username) {
        if (existsByUsername(username)) {
            return users.get(username);
        }

        User user = new User();
        user.setUsername(username);
        users.put(username, user);
        return user;
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
