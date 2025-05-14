package com.example.lt_mobile_nhom4.components.image_view;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.components.ImageHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageHistoryFragment extends Fragment {
    private ViewPager2 viewPager;
    private ImageHistoryPagerAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private List<ImageHistory> imageHistories;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_history, container, false);

        viewPager = view.findViewById(R.id.view_pager);
        adapter = new ImageHistoryPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        view.findViewById(R.id.more).setOnClickListener(v -> {
            showPhotoActionsDialog();
        });
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        imageHistories = new ArrayList<>();

        loadImageHistory();

        return view;
    }

    private void showPhotoActionsDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_photo_actions);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        TextView btnDownload = dialog.findViewById(R.id.btn_save);
        TextView btnDelete = dialog.findViewById(R.id.btn_delete);
        
        btnDownload.setOnClickListener(v -> {
            downloadCurrentImage();
            dialog.dismiss();
        });
        
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có muốn xóa ảnh này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteCurrentImage();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void deleteCurrentImage() {
        int currentPosition = viewPager.getCurrentItem();
        if (imageHistories != null && !imageHistories.isEmpty() && currentPosition < imageHistories.size()) {
            ImageHistory currentImage = imageHistories.get(currentPosition);
            String userId = currentImage.getUserId();
            String imageUrl = currentImage.getImageUrl();
            
            if (firebaseAuth.getCurrentUser() != null &&
                firebaseAuth.getCurrentUser().getUid().equals(userId)) {
                
                // Find the document with this image URL
                db.collection("images")
                        .whereEqualTo("image_url", imageUrl)
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // Delete the document
                                queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(requireContext(), "Ảnh đã được xóa", Toast.LENGTH_SHORT).show();
                                            // Remove from local list and update adapter
                                            imageHistories.remove(currentPosition);
                                            adapter.setImageHistories(imageHistories);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(requireContext(), "Không thể xóa ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(requireContext(), "Bạn chỉ có thể xóa ảnh của mình", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveCurrentImage() {
        int currentPosition = viewPager.getCurrentItem();
        if (imageHistories != null && !imageHistories.isEmpty() && currentPosition < imageHistories.size()) {
            String imageUrl = imageHistories.get(currentPosition).getImageUrl();
            
            Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @androidx.annotation.Nullable Transition<? super Bitmap> transition) {
                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    .format(new Date());
                            String fileName = "IMG_" + timestamp + ".jpg";
                            
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                            
                            Uri imageUri = requireContext().getContentResolver().insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            
                            if (imageUri != null) {
                                try {
                                    OutputStream outputStream = requireContext().getContentResolver().openOutputStream(imageUri);
                                    if (outputStream != null) {
                                        resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                        outputStream.close();
                                        Toast.makeText(requireContext(), "Ảnh đã được lưu", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(requireContext(), "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        
                        @Override
                        public void onLoadCleared(@androidx.annotation.Nullable Drawable placeholder) {
                            // Do nothing
                        }
                    });
        }
    }

    private void downloadCurrentImage() {
        int currentPosition = viewPager.getCurrentItem();
        if (imageHistories != null && !imageHistories.isEmpty() && currentPosition < imageHistories.size()) {
            String imageUrl = imageHistories.get(currentPosition).getImageUrl();
            

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "IMG_" + timestamp + ".jpg";
            
            // Use Android's DownloadManager to handle the download
            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(imageUrl);
            
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("Tải ảnh");
            request.setDescription("Đang tải ảnh...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);
            
            try {
                downloadManager.enqueue(request);
                Toast.makeText(requireContext(), "Đang tải xuống ảnh...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Download error: ", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View root = requireActivity().findViewById(R.id.fragment_container);
        if (root != null) {
            root.setVisibility(View.GONE);

            View cameraView = requireActivity().findViewById(R.id.camera_view);
            View bottomController = requireActivity().findViewById(R.id.linear_bottom);
            if (cameraView != null) cameraView.setVisibility(View.VISIBLE);
            if (bottomController != null) bottomController.setVisibility(View.VISIBLE);
        }
    }
    public void loadImageHistory() {
        final String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "User ID from SharedPreferences: " + currentUserId);

        if (currentUserId == null || currentUserId.isEmpty()) {
            if (firebaseAuth.getCurrentUser() != null) {
                Log.d(TAG, "Retrieved user ID from Firebase: " + currentUserId);
            } else {
                Log.d(TAG, "No user ID available, loading all images");
                return;
            }
        }

        // User is authenticated, load user's images and friends' images
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "User document does not exist for ID: " + currentUserId);
                        return;
                    }

                    List<String> acceptedFriendIds = new ArrayList<>();
                    acceptedFriendIds.add(currentUserId);

                    Map<String, Object> friends = (Map<String, Object>) documentSnapshot.get("friends");
                    if (friends != null) {
                        for (Map.Entry<String, Object> entry : friends.entrySet()) {
                            if ("accepted".equals(entry.getValue())) {
                                acceptedFriendIds.add(entry.getKey());
                            }
                        }
                    }

                    Log.d(TAG, "Loading images for user and " + (acceptedFriendIds.size() - 1) + " friends");

                    db.collection("images")
                            .whereIn("userId", acceptedFriendIds)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                imageHistories = new ArrayList<>();
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    String imageUrl = document.getString("image_url");
                                    String description = document.getString("description");
                                    long timestamp = document.getLong("createdAt") != null ?
                                            document.getLong("createdAt") : System.currentTimeMillis();
                                    String userId = document.getString("userId");

                                    imageHistories.add(new ImageHistory(imageUrl, description, userId, timestamp));
                                }

                                if (adapter != null) {
                                    adapter.setImageHistories(imageHistories);
                                }

                                Log.d(TAG, "Số ảnh load được: " + imageHistories.size());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

