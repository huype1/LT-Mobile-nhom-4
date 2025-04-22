package com.example.lt_mobile_nhom4.components.friendview;

import java.io.Serializable;
import java.util.List;

public class Friend implements Serializable {
    private String name;
    private int imageResId;
    private List<Photo> photos;

    public Friend(String name, int imageResId, List<Photo> photos) {
        this.name = name;
        this.imageResId = imageResId;
        this.photos = photos;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
}
