package com.example.lt_mobile_nhom4.components.friendview;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class FriendViewActivity extends AppCompatActivity {

    private TextView friendSelector;
    private ImageView dropdownIcon;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private List<Friend> friends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_view);

        friendSelector = findViewById(R.id.friend_selector);
        dropdownIcon = findViewById(R.id.dropdown_icon);
        recyclerView = findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Mặc định khởi tạo adapter với bạn đầu tiên (tránh null)
        friends = FriendData.getFriends();
        Friend firstFriend = friends.get(0);
        for (Photo photo : firstFriend.getPhotos()) {
            photo.setFriend(firstFriend);
        }
        photoAdapter = new PhotoAdapter(this, firstFriend, firstFriend.getPhotos());
        recyclerView.setAdapter(photoAdapter);

        showAllPhotos();

        findViewById(R.id.friend_selector_container).setOnClickListener(v -> showFriendDialog());
    }

    private void showAllPhotos() {
        List<Photo> allPhotos = new ArrayList<>();
        for (Friend f : friends) {
            for (Photo p : f.getPhotos()) {
                p.setFriend(f); // 🧩 Gán friend cho từng photo
                allPhotos.add(p);
            }
        }
        photoAdapter = new PhotoAdapter(this, null, allPhotos); // null vì không phải một friend cụ thể
        recyclerView.setAdapter(photoAdapter);
    }

    private void showFriendDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.friend_menu_dialog, null);
        ListView listView = dialogView.findViewById(R.id.friend_list_view);

        List<Friend> displayFriends = new ArrayList<>();
        displayFriends.add(new Friend("Mọi người", R.drawable.ic_all_friends, new ArrayList<>()));
        displayFriends.addAll(friends);

        FriendDialogAdapter adapter = new FriendDialogAdapter(this, displayFriends);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Friend selectedFriend = displayFriends.get(position);
            friendSelector.setText(selectedFriend.getName());

            if (selectedFriend.getName().equals("Mọi người")) {
                showAllPhotos();
            } else {
                for (Photo photo : selectedFriend.getPhotos()) {
                    photo.setFriend(selectedFriend); // 🧩 Gán friend cho từng photo
                }
                photoAdapter = new PhotoAdapter(this, selectedFriend, selectedFriend.getPhotos());
                recyclerView.setAdapter(photoAdapter);
            }

            dropdownIcon.setRotation(0);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> dropdownIcon.setRotation(0));
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        dropdownIcon.setRotation(180);
    }
}
