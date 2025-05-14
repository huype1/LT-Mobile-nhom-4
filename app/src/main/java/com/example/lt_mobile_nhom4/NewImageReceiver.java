package com.example.lt_mobile_nhom4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;

public class NewImageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String imageUrl = intent.getStringExtra("image_url");
        String senderName = intent.getStringExtra("sender_name");
        String friendId = intent.getStringExtra("friend_id");
        String imageId = intent.getStringExtra("image_id");

        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        String lastImagePath = prefs.getString("widget_image_path_" + friendId, null);

        if (lastImagePath != null && lastImagePath.equals(imageUrl)) {
            Log.d("NewImageReceiver", "Ảnh này đã được xử lý trước đó, không cần cập nhật lại.");
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("widget_image_path_" + friendId, imageUrl);
        editor.apply();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("images")
                .document(imageId)  // Dùng imageId để truy vấn thông tin của ảnh
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String description = snapshot.getString("description");  // Lấy description từ Firestore

                        File storageDir = new File(context.getFilesDir(), "widget_images");
                        if (!storageDir.exists()) storageDir.mkdirs();

                        File imageFile = new File(storageDir, "widget_" + friendId + ".jpg");

                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                InputStream in = new URL(imageUrl).openStream();

                                Bitmap resizedBitmap = resizeImage(in, 100, 100);

                                if (resizedBitmap != null) {
                                    FileOutputStream out = new FileOutputStream(imageFile);
                                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

                                    context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("widget_image_path_" + friendId, imageFile.getAbsolutePath())
                                            .apply();

                                    // Gửi Intent cập nhật widget với description
                                    Intent updateIntent = new Intent(context, HomeWidget.class);
                                    updateIntent.setAction("com.example.lt_mobile_nhom4.UPDATE_WIDGET");
                                    updateIntent.putExtra("friendId", friendId);
                                    updateIntent.putExtra("description", description);
                                    updateIntent.putExtra("imagePath", imageFile.getAbsolutePath());
                                    context.sendBroadcast(updateIntent);

                                    NotificationHelper.sendLocalNotification(context, senderName);
                                }

                            } catch (Exception e) {
                                Log.e("NewImageReceiver", "Lỗi tải ảnh hoặc lưu ảnh: " + e.getMessage());
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NewImageReceiver", "Lỗi truy vấn Firestore: " + e.getMessage());
                });
    }


    private Bitmap resizeImage(InputStream inputStream, int targetWidth, int targetHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            int scaleFactor = Math.min(options.outWidth / targetWidth, options.outHeight / targetHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            inputStream.reset();
            return BitmapFactory.decodeStream(inputStream, null, options);
        } catch (Exception e) {
            Log.e("Image Resize", "Error resizing image: " + e.getMessage());
        }
        return null;
    }


}
