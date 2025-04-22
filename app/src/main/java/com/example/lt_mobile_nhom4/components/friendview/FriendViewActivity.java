package com.example.lt_mobile_nhom4.components.friendview;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lt_mobile_nhom4.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class FriendViewActivity extends AppCompatActivity {

    private TextView Everyone;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;

    private List<Friend> friendList;
    private List<Photo> displayedPhotos = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_view);

        Everyone = findViewById(R.id.friend_selector);
        recyclerView = findViewById(R.id.recycler_view);

        friendList = FriendData.getFriends(this);

        updatePhotosForEveryone();

        photoAdapter = new PhotoAdapter(this, displayedPhotos);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(photoAdapter);

        Everyone.setOnClickListener(v -> showFriendPicker());
    }

    private void updatePhotosForEveryone() {
        displayedPhotos.clear();
        for (Friend friend : friendList) {
            displayedPhotos.addAll(friend.getPhotos());
        }
        photoAdapter.notifyDataSetChanged();
        Everyone.setText("Mọi người");
    }

    private void updatePhotosForFriend(Friend friend) {
        displayedPhotos.clear();
        displayedPhotos.addAll(friend.getPhotos());
        photoAdapter.notifyDataSetChanged();
        Everyone.setText(friend.getName());
    }

    private void showFriendPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_photo_actions, null);

        // Bổ sung các nút chọn bạn trong dialog này tùy thích
        for (Friend friend : friendList) {
            TextView tv = new TextView(this);
            tv.setText(friend.getName());
            tv.setTextSize(18);
            tv.setPadding(20, 20, 20, 20);
            tv.setOnClickListener(v -> {
                updatePhotosForFriend(friend);
                dialog.dismiss();
            });
            ((ViewGroup) view).addView(tv);
        }

        TextView everyone = new TextView(this);
        everyone.setText("Mọi người");
        everyone.setTextSize(18);
        everyone.setPadding(20, 20, 20, 20);
        everyone.setOnClickListener(v -> {
            updatePhotosForEveryone();
            dialog.dismiss();
        });
        ((ViewGroup) view).addView(everyone);

        dialog.setContentView(view);
        dialog.show();
    }
}
