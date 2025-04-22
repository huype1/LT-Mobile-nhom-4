package com.example.lt_mobile_nhom4.models;

import java.io.Serializable;

public class FriendModel implements Serializable {
    private String id;
    private String username;
    private String full_name;
    private String avatarUrl;

    // Default constructor needed for Firestore
    public FriendModel() {
    }

    public FriendModel(String id, String username, String full_name, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.full_name = full_name;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return full_name;
    }

    public void setFullName(String full_name) {
        this.full_name = full_name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
