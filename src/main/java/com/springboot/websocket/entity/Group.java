package com.springboot.websocket.entity;

import java.util.HashSet;
import java.util.Set;

public class Group {
    private String id;              // ID duy nhất của nhóm
    private String name;            // Tên nhóm
    private Set<String> members;    // Danh sách username của các thành viên

    public Group() {
        this.members = new HashSet<>();
    }

    public Group(String id) {
        this.id = id;
        this.members = new HashSet<>();
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }
}
