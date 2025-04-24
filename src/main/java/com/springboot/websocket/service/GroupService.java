package com.springboot.websocket.service;

import com.springboot.websocket.controller.ChatController;
import com.springboot.websocket.entity.Group;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GroupService {
    // Lưu trữ các nhóm, key là groupId, value là đối tượng Group
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    
    // Counter để tạo ID duy nhất cho mỗi nhóm mới
    private final AtomicLong groupIdCounter = new AtomicLong(1);
    
    // Để thông báo cho các thành viên khi có nhóm mới
    private final ChatController chatController;

    public GroupService(ChatController chatController) {
        this.chatController = chatController;
    }

    /**
     * Tạo nhóm chat mới
     */
    public Group createGroup(String name, String creatorUsername, List<String> memberUsernames) {
        if (memberUsernames.size() < 1) {
            throw new RuntimeException("Group must have at least 2 members including creator");
        }

        // Thêm người tạo vào danh sách thành viên
        Set<String> members = new HashSet<>();
        members.add(creatorUsername);
        members.addAll(memberUsernames);

        // Tạo và cấu hình nhóm mới
        Group group = new Group();
        group.setId(String.valueOf(groupIdCounter.getAndIncrement()));
        group.setName(name);
        group.setMembers(members);
        
        groups.put(group.getId(), group);
        
        // Gửi thông báo cho tất cả thành viên về nhóm mới
        chatController.notifyGroupCreation(group.getId(), group.getName(), members);
        
        return group;
    }

    /**
     * Tìm nhóm theo ID
     */
    public Optional<Group> findById(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    /**
     * Kiểm tra một người dùng có phải là thành viên của nhóm không
     */
    public boolean isMember(String groupId, String username) {
        return findById(groupId)
            .map(group -> group.getMembers().contains(username))
            .orElse(false);
    }

    /**
     * Thêm thành viên mới vào nhóm
     */
    public Group addMember(String groupId, String username) {
        Group group = findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));
        group.getMembers().add(username);
        return group;
    }

    /**
     * Xóa thành viên khỏi nhóm
     */
    public Group removeMember(String groupId, String username) {
        Group group = findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));
        group.getMembers().remove(username);
        return group;
    }
}