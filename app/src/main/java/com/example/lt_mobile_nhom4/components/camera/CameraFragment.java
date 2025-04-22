package com.example.lt_mobile_nhom4.components.camera;

import static com.bumptech.glide.request.RequestOptions.option;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.lt_mobile_nhom4.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        REQUIRED_PERMISSIONS = permissions.toArray(new String[0]);
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
        return view;
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Khởi tạo camera lỗi", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    1001
            );
        }
        cameraExecutor = Executors.newSingleThreadExecutor();

        cameraFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlash();
            }
        });

        cameraFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCamera();
            }
        });

        cameraCapture.setOnClickListener(v -> takePhoto());
        setUpImageSend();
        cancelButton.setOnClickListener(v -> resetCamera());
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
                            previewView.setVisibility(View.GONE);
                            imageViewLayout.setVisibility(View.VISIBLE);

                            ImageView imageView = requireView().findViewById(R.id.image_view);
                            imageView.setImageURI(Uri.fromFile(photoFile));

                            captureSetting.setVisibility(View.GONE);
                            sendController.setVisibility(View.VISIBLE);

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
                        double progress = (double) bytes / totalBytes * 100;
                        Log.d(TAG, "Upload progress: " + requestId + " % (" + bytes + "/" + totalBytes + ")");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String secureUrl = (String) resultData.get("secure_url");
                                Log.d(TAG, "Upload success: " + requestId + " " + secureUrl);
                                previewView.setVisibility(View.VISIBLE);
                                imageViewLayout.setVisibility(View.GONE);
                                cameraCapture.setVisibility(View.VISIBLE);
                                sendController.setVisibility(View.GONE);

                                Toast.makeText(requireContext(), "Upload success: " + secureUrl, Toast.LENGTH_SHORT).show();
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

    private void resetCamera() {
        previewView.setVisibility(View.VISIBLE);
        imageViewLayout.setVisibility(View.GONE);
        captureSetting.setVisibility(View.VISIBLE);
        sendController.setVisibility(View.GONE);
        startCamera();
    }

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

    private boolean allPermissionGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}