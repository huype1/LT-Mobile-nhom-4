package com.example.lt_mobile_nhom4.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageHistory {
    private String imageUrl;
    private String description;
    private String userId;
    private long timestamp;
}
