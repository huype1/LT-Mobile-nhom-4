package com.example.lt_mobile_nhom4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide; // Import Glide for image loading
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.ErrorInfo;
import com.example.lt_mobile_nhom4.components.UserSearchFragment;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.example.lt_mobile_nhom4.utils.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ImageView imgProfile;
    private TextView tvFriendsCount;
    private SharedPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        imgProfile = findViewById(R.id.imgProfile); // Initialize imgProfile
        tvFriendsCount = findViewById(R.id.tvFriendsCount);

        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        initCloudinary();
        prefsManager = SharedPreferencesManager.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        logoutButton.setOnClickListener(v -> {
//            FirebaseAuth.getInstance().signOut();
//            Intent intent = new Intent(this, AuthActivity.class);
//            startActivity(intent);
//            finish();
//        });
//
//        searchButton.setOnClickListener(v -> {
//            openUserSearchFragment();
//        });

//        imgProfile.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
//            startActivity(intent);
//        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                createNotificationChannel();
                createFriendNotificationChannel();
            }
        } else {
            // For lower versions, no need for the POST_NOTIFICATIONS permission
            createNotificationChannel();
            createFriendNotificationChannel();
        }


        if (savedInstanceState == null) {
            CameraFragment cameraFragment = new CameraFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, cameraFragment)
                    .commit();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent serviceIntent = new Intent(this, FriendImageListenerService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        updateFriendsCount(0);
        syncFriendsCount(); // Đồng bộ số lượng bạn bè
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserAvatar(); // Load lại avatar từ Firestore
    }

    private void loadUserAvatar() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        // Use Glide to load the avatar URL into the ImageView
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.person_24px) // Default placeholder
                            .into(imgProfile);
                    }
                }
            })
            .addOnFailureListener(e -> Log.e("MainActivity", "Failed to load avatar", e));
    }

    private void updateFriendsCount(int count) {
        tvFriendsCount.setText(count + " Bạn bè");
    }

    private void syncFriendsCount() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("friends")
                .addSnapshotListener((QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "Error fetching friends count", e);
                        return;
                    }
                    if (snapshot != null) {
                        int count = snapshot.size();
                        updateFriendsCount(count); // Cập nhật giao diện
                    }
                });
    }

    private void openUserSearchFragment() {
        UserSearchFragment searchFragment = new UserSearchFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, searchFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dylazm8ji");
            config.put("api_key", "612346784879931");
            config.put("api_secret", "azyemRmaWl5DFnP8lD61dISP69I");
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            Log.d("Cloudinary", "Cloudinary is already initialized");
        }
    }

    private void updateAvatarUrl(String avatarUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", avatarUrl);

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Avatar URL updated successfully"))
            .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update avatar URL", e));
    }

    private void uploadAvatarToCloudinary(Uri imageUri) {
        if (imageUri == null) {
            Log.e("MainActivity", "Image URI is null");
            return;
        }

        MediaManager.get().upload(imageUri)
            .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    Log.d("Cloudinary", "Upload started: " + requestId);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    Log.d("Cloudinary", "Upload progress: " + bytes + "/" + totalBytes);
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String avatarUrl = (String) resultData.get("secure_url");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Log.d("Cloudinary", "Upload successful: " + avatarUrl);
                        updateAvatarUrl(avatarUrl); // Save the URL to Firestore
                        Glide.with(MainActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.person_24px)
                            .into(imgProfile); // Update the ImageView
                    } else {
                        Log.e("Cloudinary", "Upload successful but URL is empty");
                    }
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.e("Cloudinary", "Upload failed: " + error.getDescription());
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    Log.e("Cloudinary", "Upload rescheduled: " + error.getDescription());
                }
            })
            .dispatch();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default",
                    getString(R.string.default_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void createFriendNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "friend_requests",
                    "Friend Activity",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Thông báo khi bạn bè đăng ảnh mới");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

