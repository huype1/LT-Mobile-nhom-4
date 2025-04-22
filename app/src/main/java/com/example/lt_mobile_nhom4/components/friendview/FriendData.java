package com.example.lt_mobile_nhom4.components.friendview;

import android.content.Context;

import com.example.lt_mobile_nhom4.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FriendData {
    public static List<Friend> getFriends(Context context) {
        List<Friend> friendList = new ArrayList<>();

        String[] names = {"An", "Bình", "Chi"};
        int[] avatars = {R.drawable.person_24px, R.drawable.person_24px, R.drawable.person_24px};

        File baseDir = new File(context.getExternalFilesDir(null), "images");

        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            int avatar = avatars[i];

            File friendDir = new File(baseDir, name);
            List<Photo> photoList = new ArrayList<>();

            if (friendDir.exists()) {
                File[] imageFiles = friendDir.listFiles((dir, filename) -> filename.endsWith(".jpg") || filename.endsWith(".png"));
                if (imageFiles != null) {
                    Arrays.sort(imageFiles); // Optional: sort theo tên
                    for (File image : imageFiles) {
                        photoList.add(new Photo(image.getAbsolutePath()));
                    }
                }
            }

            friendList.add(new Friend(name, avatar, photoList));
        }

        return friendList;
    }
}
