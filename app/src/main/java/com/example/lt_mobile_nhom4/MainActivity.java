package com.example.lt_mobile_nhom4;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import com.example.lt_mobile_nhom4.components.friendview.Friend;
import com.example.lt_mobile_nhom4.components.friendview.FriendDialogAdapter;
import com.example.lt_mobile_nhom4.components.UserSearchFragment;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.example.lt_mobile_nhom4.models.FriendModel;
import com.example.lt_mobile_nhom4.utils.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ImageView imgProfile;
    private TextView tvFriendsCount;
    private CardView friendsCountContainer;
    private SharedPreferencesManager prefsManager;
    private FriendDialogAdapter friendsDialogAdapter;
    private Dialog friendsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        imgProfile = findViewById(R.id.imgProfile); // Initialize imgProfile
        tvFriendsCount = findViewById(R.id.tvFriendsCount);
        friendsCountContainer = findViewById(R.id.friendsCountContainer);

        initFriendsDialog();

        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        friendsCountContainer.setOnClickListener(v -> showFriendsDialog());

        initCloudinary();
        prefsManager = SharedPreferencesManager.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                createNotificationChannel();
                showNotification();
            }
        } else {
            createNotificationChannel();
            showNotification();
        }

        if (savedInstanceState == null) {
            CameraFragment cameraFragment = new CameraFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, cameraFragment)
                    .commit();
        }

        updateFriendsCount(0);
        syncFriendsCount(); // Đồng bộ số lượng bạn bè
    }

    private void initFriendsDialog() {
        friendsDialog = new Dialog(this);
        friendsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        friendsDialog.setContentView(R.layout.dialog_friends_list);
        
        ListView friendsListView = friendsDialog.findViewById(R.id.friendsListView);
        friendsDialogAdapter = new FriendDialogAdapter(this);
        friendsListView.setAdapter(friendsDialogAdapter);
        
        Window window = friendsDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showFriendsDialog() {
        fetchFriendsData();
        friendsDialog.show();
    }

    private void fetchFriendsData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        List<FriendModel> friendsList = new ArrayList<>();
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("friends")) {
                    Map<String, Object> friendsMap = (Map<String, Object>) documentSnapshot.get("friends");
                    if (friendsMap != null && !friendsMap.isEmpty()) {
                        for (Map.Entry<String, Object> entry : friendsMap.entrySet()) {
                            String friendId = entry.getKey();
                            String status = (String) entry.getValue();
                            
                            if ("accepted".equals(status)) {
                                // Fetch details for each friend
                                db.collection("users").document(friendId).get()
                                    .addOnSuccessListener(friendDoc -> {
                                        if (friendDoc.exists()) {
                                            String username = friendDoc.getString("username");
                                            String fullName = friendDoc.getString("full-name");
                                            String avatarUrl = friendDoc.getString("image-url");
                                            
                                            FriendModel friend = new FriendModel(
                                                friendId, 
                                                username != null ? username : "User", 
                                                fullName != null ? fullName : "No Name",
                                                avatarUrl
                                            );
                                            
                                            friendsList.add(friend);
                                            friendsDialogAdapter.setFriendList(friendsList);
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to fetch friend details", e));
                            }
                        }
                    }
                }
            })
            .addOnFailureListener(e -> Log.e("MainActivity", "Failed to fetch friends data", e));
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
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.person_24px)
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

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("friends")) {
                        Map<String, Object> friendsMap = (Map<String, Object>) documentSnapshot.get("friends");
                        if (friendsMap != null) {
                            // Count only accepted friends
                            int acceptedFriendsCount = 0;
                            for (Map.Entry<String, Object> entry : friendsMap.entrySet()) {
                                if ("accepted".equals(entry.getValue())) {
                                    acceptedFriendsCount++;
                                }
                            }
                            updateFriendsCount(acceptedFriendsCount);
                            Log.d("MainActivity", "Accepted friends count: " + acceptedFriendsCount);
                        } else {
                            updateFriendsCount(0);
                        }
                    } else {
                        updateFriendsCount(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error fetching friends", e);
                    updateFriendsCount(0);
                });

        // Add a snapshot listener to keep the count updated in real-time
        db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("MainActivity", "Error listening for friend updates", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("friends")) {
                        Map<String, Object> friendsMap = (Map<String, Object>) documentSnapshot.get("friends");
                        if (friendsMap != null) {
                            // Count only accepted friends
                            int acceptedFriendsCount = 0;
                            for (Map.Entry<String, Object> entry : friendsMap.entrySet()) {
                                if ("accepted".equals(entry.getValue())) {
                                    acceptedFriendsCount++;
                                }
                            }
                            updateFriendsCount(acceptedFriendsCount);
                        } else {
                            updateFriendsCount(0);
                        }
                    } else {
                        updateFriendsCount(0);
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
                        updateAvatarUrl(avatarUrl);
                        Glide.with(MainActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.person_24px)
                            .into(imgProfile);
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

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(1, builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
