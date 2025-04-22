package com.example.lt_mobile_nhom4.components.friendview;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class PhotoDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textFriendName;
    private ImageView avatarImage;
    private ImageButton btnBack, btnCamera, btnMore;

    private Friend currentFriend;
    private List<Photo> photoList;
    private int selectedIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        // Ánh xạ view
        recyclerView = findViewById(R.id.recycler_detail_photos);
        textFriendName = findViewById(R.id.text_friend_name);
        avatarImage = findViewById(R.id.image_friend_avatar);
        btnBack = findViewById(R.id.back);
        btnCamera = findViewById(R.id.camera);
        btnMore = findViewById(R.id.more);

        // Lấy dữ liệu bạn bè và vị trí ảnh
        currentFriend = (Friend) getIntent().getSerializableExtra("friend");
        selectedIndex = getIntent().getIntExtra("selected_index", 0);

        if (currentFriend != null) {
            photoList = currentFriend.getPhotos();

            // Debug size
            Log.d("PhotoDetail", "photoList size: " + (photoList != null ? photoList.size() : 0));

            textFriendName.setText(currentFriend.getName());
            avatarImage.setImageResource(currentFriend.getImageResId());

            // Setup RecyclerView
            PhotoDetailAdapter adapter = new PhotoDetailAdapter(photoList);
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
            recyclerView.setAdapter(adapter);
            recyclerView.scrollToPosition(selectedIndex);
        }

        // Xử lý các nút
        btnBack.setOnClickListener(v -> finish());

        btnCamera.setOnClickListener(v -> {
            // TODO: Mở camera
        });

        btnMore.setOnClickListener(v -> showBottomMenu());
    }

    private void showBottomMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_photo_actions, null);

        view.findViewById(R.id.btn_share).setOnClickListener(v -> {
            // TODO: Chia sẻ ảnh
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            // TODO: Lưu ảnh
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            // TODO: Xoá ảnh
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }
}
