package com.example.lt_mobile_nhom4.components.camera;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.lt_mobile_nhom4.HomeWidget;
import com.example.lt_mobile_nhom4.MyApplication;
import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.components.ImageHistory;
import com.example.lt_mobile_nhom4.components.ImageHistoryAdapter;
import com.example.lt_mobile_nhom4.components.image_view.ImageHistoryFragment;
import com.example.lt_mobile_nhom4.utils.SharedPreferencesManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private FirebaseFirestore db;

    private String[] REQUIRED_PERMISSIONS;

    private PreviewView previewView;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private final String TAG = "CameraFragment";
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ImageButton cameraCapture, cameraFlash, cameraFlip, sendButton, cancelButton;
    private RelativeLayout imageViewLayout;
    private LinearLayout captureSetting;
    private View sendController;
    private File lastCapturedPhotoFile;
    private EditText text;

    private RecyclerView recyclerView;
    private ImageHistoryAdapter adapter;
    private LinearLayoutManager layoutManager;
    private FirebaseAuth firebaseAuth;
    SharedPreferencesManager prefsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        REQUIRED_PERMISSIONS = permissions.toArray(new String[0]);

        db = MyApplication.getFirestore();

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera_and_view, container, false);
        previewView = view.findViewById(R.id.camera_view);
        cameraFlash = view.findViewById(R.id.camera_flash);
        cameraFlip = view.findViewById(R.id.camera_flip);
        cameraCapture = view.findViewById(R.id.camera_capture);
        imageViewLayout = view.findViewById(R.id.image_view_layout);
        sendController = view.findViewById(R.id.send_controller);
        captureSetting = view.findViewById(R.id.capture_setting);
        sendButton = view.findViewById(R.id.image_send);
        cancelButton = view.findViewById(R.id.image_cancel);
        text = view.findViewById(R.id.text_add_message);


        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

        return view;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
                previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Khởi tạo camera lỗi", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        updatePreviewScale();
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

        prefsManager = SharedPreferencesManager.getInstance(requireContext());

        adapter = new ImageHistoryAdapter();

        if (allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    1001
            );
        }
        cameraExecutor = Executors.newSingleThreadExecutor();

        cameraFlash.setOnClickListener(v -> toggleFlash());
        cameraFlip.setOnClickListener(v -> toggleCamera());
        cameraCapture.setOnClickListener(v -> takePhoto());
        setUpImageSend();
        cancelButton.setOnClickListener(v -> resetCamera());

        LinearLayout historyController = view.findViewById(R.id.history_controller);
        historyController.setOnClickListener(v -> {
            // Create new fragment
            ImageHistoryFragment imageHistoryFragment = new ImageHistoryFragment();

            // Get fragment manager
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

            // Start transaction
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentImageContainer, imageHistoryFragment)
                    .addToBackStack(null) // Allow back navigation
                    .commit();

            // Hide camera UI
            previewView.setVisibility(View.GONE);
            view.findViewById(R.id.linear_bottom).setVisibility(View.GONE);
        });

        // Initial load
        loadImageHistory();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }

    private void toggleFlash() {
        if (flashMode == ImageCapture.FLASH_MODE_OFF) {
            flashMode = ImageCapture.FLASH_MODE_ON;
            cameraFlash.setImageResource(R.drawable.camera_flash_on);
        } else {
            flashMode = ImageCapture.FLASH_MODE_OFF;
            cameraFlash.setImageResource(R.drawable.camera_flash_off);
        }
        startCamera();
    }

    private void toggleCamera() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera();
    }

    //Chụp ảnh
    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(
                requireContext().getCacheDir(),
                System.currentTimeMillis() + ".jpg"
        );

        lastCapturedPhotoFile = photoFile;

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        requireActivity().runOnUiThread(() -> {
                            showImagePreview(photoFile);
                        });
                        Log.d(TAG, "đã lưu: " + photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "loi: " + exception.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                        Log.e(TAG, "Loi chup anh", exception);
                    }
                }
        );
    }

    private void showImagePreview(File file) {
        previewView.setVisibility(View.GONE);
        imageViewLayout.setVisibility(View.VISIBLE);
        captureSetting.setVisibility(View.GONE);
        sendController.setVisibility(View.VISIBLE);

        // Dùng Glide để hiển thị ảnh
        ImageView imageView = requireView().findViewById(R.id.image_view);
        Glide.with(requireContext()).load(file).into(imageView);
    }

    private void setUpImageSend() {
        sendButton.setOnClickListener(v -> {
            if (lastCapturedPhotoFile != null) {
                uploadToCloudinary(lastCapturedPhotoFile.getAbsolutePath());
            } else {
                Toast.makeText(requireContext(), "Chưa có ảnh nào để gửi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadToCloudinary(String filePath) {
        String requestedId = MediaManager.get().upload(filePath)
                .option("folder", "image_uploads")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                        Toast.makeText(requireContext(), "Upload started", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d(TAG, "Upload progress: " + requestId + " % (" + bytes + "/" + totalBytes + ")");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        requireActivity().runOnUiThread(() -> {
                            String secureUrl = (String) resultData.get("secure_url");
                            Log.d(TAG, "Upload success: " + requestId + " " + secureUrl);

                            String description = text.getText().toString().trim();
                            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                    : "Unknown User";

                            // Create a new document with a unique ID
                            Map<String, Object> data = new HashMap<>();
                            data.put("image_url", secureUrl);
                            data.put("description", description);
                            data.put("userId", userId);
                            data.put("createdAt", System.currentTimeMillis());

                            db.collection("images").add(data)
                                    .addOnSuccessListener(documentReference -> {
                                        String documentId = documentReference.getId();
                                        Log.d(TAG, "Image uploaded successfully with ID: " + documentId);

                                        // Update the document to include its ID
                                        db.collection("images").document(documentId)
                                                .update("id", documentId)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Document ID added successfully"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error adding document ID", e));

                                        Toast.makeText(requireContext(), "Upload success", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error uploading image: ", e);
                                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                                    });

                            // Reset UI
                            previewView.setVisibility(View.VISIBLE);
                            imageViewLayout.setVisibility(View.GONE);
                            cameraCapture.setVisibility(View.VISIBLE);
                            captureSetting.setVisibility(View.VISIBLE);
                            sendController.setVisibility(View.GONE);
                            text.setText("");
                        });
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();  // Lấy ID người dùng hiện tại

                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        db.collection("images")
                                .whereEqualTo("userId", userId)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        DocumentSnapshot imageDoc = queryDocumentSnapshots.getDocuments().get(0);
                                        String imageUrl = imageDoc.getString("image_url");
                                        String description = imageDoc.getString("description");

                                        // B2: Gửi ảnh cho bạn bè đã accept
                                        db.collection("users")
                                                .document(userId)
                                                .get()
                                                .addOnSuccessListener(documentSnapshot -> {
                                                    if (documentSnapshot.exists()) {
                                                        Map<String, Object> friendsMap = (Map<String, Object>) documentSnapshot.get("friends");
                                                        if (friendsMap != null) {
                                                            for (Map.Entry<String, Object> entry : friendsMap.entrySet()) {
                                                                String friendId = entry.getKey();
                                                                String status = String.valueOf(entry.getValue());
                                                                if ("accepted".equals(status)) {
                                                                    updateFriendWidget(friendId, imageUrl, description);  // ✅ Gửi ảnh
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                    }
                                });

                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "Upload error: " + requestId + " " + error.getDescription());
                                Toast.makeText(requireContext(), "Upload error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Upload rescheduled: " + requestId + " " + error.getDescription());
                    }
                })
                .dispatch();

    }

    public void loadImageHistory() {
        final String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "User ID from SharedPreferences: " + currentUserId);

        if (currentUserId == null || currentUserId.isEmpty()) {
            if (firebaseAuth.getCurrentUser() != null) {
                prefsManager.saveUserSession(currentUserId, firebaseAuth.getCurrentUser().getEmail());
                Log.d(TAG, "Retrieved user ID from Firebase: " + currentUserId);
            } else {
                Log.d(TAG, "No user ID available, loading all images");
                return;
            }
        }

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "User document does not exist for ID: " + currentUserId);
//                        loadAllImages(); // Fallback to loading all images
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
                                List<ImageHistory> imageHistories = new ArrayList<>();
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
//                                loadAllImages(); // Fallback to loading all images
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    loadAllImages(); // Fallback to loading all images
                });
    }

    public void loadAllImages() {
        db.collection("images")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ImageHistory> imageHistories = new ArrayList<>();
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

                    Log.d(TAG, "Loaded all images: " + imageHistories.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all images", e);
                    Toast.makeText(requireContext(), "Failed to load images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (previewView.getVisibility() != View.VISIBLE) {
            previewView.setVisibility(View.VISIBLE);
            if (getView() != null) {
                getView().findViewById(R.id.linear_bottom).setVisibility(View.VISIBLE);
            }
            startCamera();
        }
    }


    private void resetCamera() {
        previewView.setVisibility(View.VISIBLE);
        imageViewLayout.setVisibility(View.GONE);
        captureSetting.setVisibility(View.VISIBLE);
        sendController.setVisibility(View.GONE);
        flashMode = ImageCapture.FLASH_MODE_OFF;
        startCamera();
    }

//    private void updatePreviewScale() {
//        int orientation = getResources().getConfiguration().orientation;
//        int rotation = requireActivity().getWindowManager().getDefaultDisplay().getRotation();
//
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
//        } else {
//            previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Màn hình được xoay ngang
            // Xử lý các thay đổi cần thiết ở đây
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Màn hình được xoay dọc
            // Xử lý các thay đổi cần thiết ở đây
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            if (allPermissionGranted()) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "Chưa thêm quyền", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        }
    }

    private void updateFriendWidget(String friendId, String imageUrl, String description) {
        File storageDir = new File(requireContext().getFilesDir(), "widget_images");
        if (!storageDir.exists()) storageDir.mkdirs();

        File imageFile = new File(storageDir, "widget_" + friendId + ".jpg");

        // Tải ảnh và lưu lại
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, imageFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Lưu đường dẫn ảnh vào SharedPreferences
                requireContext().getSharedPreferences("widget_prefs", requireContext().MODE_PRIVATE)
                        .edit()
                        .putString("widget_image_path_" + friendId, imageFile.getAbsolutePath())
                        .apply();

                // Gửi broadcast để cập nhật widget ngay lập tức
                Intent intent = new Intent(requireContext(), HomeWidget.class);
                intent.setAction("com.example.lt_mobile_nhom4.UPDATE_WIDGET");
                intent.putExtra("friendId", friendId);
                intent.putExtra("description", description);
                requireContext().sendBroadcast(intent);

                Log.d(TAG, "Đã cập nhật widget cho " + friendId);
            } catch (IOException e) {
                Log.e(TAG, "Lỗi tải ảnh widget: " + e.getMessage(), e);
            }
        });
        sendNewImageBroadcastToFriend(friendId, imageUrl, FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
    }

    private void sendNewImageBroadcastToFriend(String friendId, String imageUrl, String senderName) {
        Intent intent = new Intent("com.example.lt_mobile_nhom4.NEW_IMAGE_POSTED");
        intent.putExtra("image_url", imageUrl);
        intent.putExtra("sender_name", senderName);
        intent.putExtra("friend_id", friendId);
        requireContext().sendBroadcast(intent);
    }



    private boolean allPermissionGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}
