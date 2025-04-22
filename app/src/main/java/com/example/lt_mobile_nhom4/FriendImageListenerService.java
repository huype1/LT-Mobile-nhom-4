package com.example.lt_mobile_nhom4;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class FriendImageListenerService extends Service {

    private static final String CHANNEL_ID = "friend_listener_service";

    @Override
    public void onCreate() {
        super.onCreate();
        createServiceNotificationChannel();
        startForeground(1, createServiceNotification("Đang lắng nghe ảnh mới từ bạn bè..."));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listenToFriendImages(currentUserId);
    }

    private void listenToFriendImages(String currentUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong("last_update_time", 0);

        if (System.currentTimeMillis() - lastUpdateTime < 5 * 60 * 1000) {
            return;
        }

        prefs.edit().putLong("last_update_time", System.currentTimeMillis()).apply();

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> friends = (Map<String, Object>) snapshot.get("friends");
                        if (friends == null) return;

                        List<String> friendIds = new ArrayList<>(friends.keySet());

                        db.collection("images")
                                .whereIn("userId", friendIds)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(1)
                                .addSnapshotListener((snapshots, error) -> {
                                    if (error != null || snapshots == null) return;

                                    String latestImageUrl = null;
                                    String latestFriendId = null;
                                    long latestCreatedAt = 0;

                                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                        if (dc.getType() == DocumentChange.Type.ADDED) {
                                            String userId = dc.getDocument().getString("userId");
                                            String imageUrl = dc.getDocument().getString("image_url");
                                            long createdAt = dc.getDocument().getLong("createdAt");

                                            if (createdAt > latestCreatedAt) {
                                                latestCreatedAt = createdAt;
                                                latestImageUrl = imageUrl;
                                                latestFriendId = userId;
                                            }
                                        }
                                    }

                                    if (latestImageUrl != null && latestFriendId != null) {
                                        fetchSenderNameAndTriggerWidget(latestFriendId, latestImageUrl, String.valueOf(latestCreatedAt));
                                    }
                                });
                    }
                });
    }



    private void fetchSenderNameAndTriggerWidget(String friendId, String imageUrl, String createdAt) {
        FirebaseFirestore.getInstance().collection("users").document(friendId)
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String fullName = snapshot.getString("full-name");
                        triggerWidgetUpdate(this, friendId, imageUrl, fullName, createdAt);
                    }
                });
    }

    private void triggerWidgetUpdate(Context context, String friendId, String imageUrl, String senderName, String description) {
        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        File storageDir = new File(context.getFilesDir(), "widget_images");
        if (!storageDir.exists()) storageDir.mkdirs();

        File imageFile = new File(storageDir, "widget_" + friendId + ".jpg");

        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                prefs.edit()
                        .putString("widget_image_path_" + friendId, imageFile.getAbsolutePath())
                        .apply();

                Intent updateIntent = new Intent(context, com.example.lt_mobile_nhom4.HomeWidget.class);
                updateIntent.setAction("com.example.lt_mobile_nhom4.UPDATE_WIDGET");
                updateIntent.putExtra("friendId", friendId);
                updateIntent.putExtra("senderName", senderName);
                updateIntent.putExtra("description", description);
                updateIntent.putExtra("imagePath", imageFile.getAbsolutePath());
                context.sendBroadcast(updateIntent);

                NotificationHelper.sendLocalNotification(context, senderName);

            } catch (Exception e) {
                Log.e("TriggerWidgetUpdate", "Lỗi tải ảnh: " + e.getMessage());
            }
        });
    }

    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Friend Image Listener Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createServiceNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lắng nghe ảnh bạn bè")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
