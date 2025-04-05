package com.example.lt_mobile_nhom4.components;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String username;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String imageUrl;
    private String createdAt;
    private Map<String, String> friends;

    public enum FriendStatus {
        PENDING_SENT("pending_sent"),
        PENDING_RECEIVED("pending_received"),
        ACCEPTED("accepted"),
        NONE("none");

        private final String value;

        FriendStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static FriendStatus fromString(String text) {
            for (FriendStatus status : FriendStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return NONE;
        }
    }

    public FriendStatus getFriendStatus(String userId) {
        if (friends == null) {
            return FriendStatus.NONE;
        }
        
        String status = friends.get(userId);
        return status != null ? FriendStatus.fromString(status) : FriendStatus.NONE;
    }

    public void setFriendStatus(String userId, FriendStatus status) {
        if (friends == null) {
            friends = new HashMap<>();
        }
        friends.put(userId, status.getValue());
    }
}
