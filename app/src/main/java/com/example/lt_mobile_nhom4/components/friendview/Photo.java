package com.example.lt_mobile_nhom4.components.friendview;

public class Photo {
    private int resId;

    private Friend friend;

    public Friend getFriend() {
        return friend;
    }

    public void setFriend(Friend friend) {
        this.friend = friend;
    }

    public Photo(int resId) {
        this.resId = resId;
    }

    public int getResId() {
        return resId;
    }
}
