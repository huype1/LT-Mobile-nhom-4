package com.example.lt_mobile_nhom4;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lt_mobile_nhom4.components.FriendListFragment;
import com.example.lt_mobile_nhom4.components.UserSearchFragment;
import com.example.lt_mobile_nhom4.components.auth.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    LinearLayout deleteAccountButton;
    private static final int REQUEST_PICK_WIDGET = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002; // Added constant
    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String KEY_AVATAR_URI = "avatarUri";
    private AppWidgetHost appWidgetHost;
    private ImageView imgCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_fragment);

        RelativeLayout profileLayout = findViewById(R.id.profileLayout); // Reference to the profile layout
        if (profileLayout == null) {
            Log.e("ProfileActivity", "profileLayout is null. Check layout ID.");
            return; // Exit if layout is not found
        }

        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        LinearLayout linearAddUtilities = findViewById(R.id.add_widget);

        appWidgetHost = new AppWidgetHost(this, 1);

        imgCapture = findViewById(R.id.img_capture);
        ImageView imgChangeAvatar = findViewById(R.id.img_change_avatar);

        // Load saved avatar URI
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedAvatarUri = prefs.getString(KEY_AVATAR_URI, null);
        if (savedAvatarUri != null) {
            imgCapture.setImageURI(Uri.parse(savedAvatarUri)); // Load saved avatar
        }

        imgChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE); // Launch gallery
        });

        LinearLayout linearNew = findViewById(R.id.linear_new);
        linearNew.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.findFragmentByTag("RegisterFragment") == null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(android.R.id.content, new RegisterFragment(), "RegisterFragment");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        });

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            openUserSearchFragment();
        });

        Button editInfoButton = findViewById(R.id.editInfo);
        editInfoButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.findFragmentByTag("UpdateUserInfoFragment") == null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(android.R.id.content, new UpdateUserInfoFragment(), "UpdateUserInfoFragment");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        Button friendListButton = findViewById(R.id.friendList);
        friendListButton.setOnClickListener(v -> {
            openFriendListFragment();
        });

        Button userGuideButton = findViewById(R.id.userGuideButton);
        userGuideButton.setOnClickListener(v -> {
            showUserGuide();
        });

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_account)
                    .setMessage(R.string.delete_account_confirm)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            deleteFirestoreData(user.getUid());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        linearAddUtilities.setOnClickListener(v -> {
            int appWidgetId = appWidgetHost.allocateAppWidgetId(); // Dùng AppWidgetHost, không phải AppWidgetManager
            ComponentName componentName = new ComponentName(this, HomeWidget.class);
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // Có thể thêm extras nếu cần, ví dụ để chọn widget nào khác
            startActivityForResult(intent, REQUEST_PICK_WIDGET);
        });

    }

    private void openUserSearchFragment() {
        // Hide only the profile content view.
        View profileContent = findViewById(R.id.profileContentScrollView);
        profileContent.setVisibility(View.GONE);

        UserSearchFragment searchFragment = new UserSearchFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, searchFragment, "UserSearchFragment");
        transaction.addToBackStack(null);
        transaction.commit();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                profileContent.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openFriendListFragment() {
        // Hide only the profile content view.
        View profileContent = findViewById(R.id.profileContentScrollView);
        profileContent.setVisibility(View.GONE);

        FriendListFragment friendListFragment = new FriendListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, friendListFragment, "FriendListFragment");
        transaction.addToBackStack(null);
        transaction.commit();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                profileContent.setVisibility(View.VISIBLE);
            }
        });
    }

    private void deleteFirestoreData(String uid) {
        FirebaseFirestore db =  MyApplication.getFirestore();
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("ProfileActivity", "Firestore data deleted successfully.");
                deleteFirebaseAuthAccount();
            } else {
                Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("ProfileActivity", "Error deleting Firestore document", e);
            Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteFirebaseAuthAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AuthActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showUserGuide() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.user_guide_title)
            .setMessage(R.string.user_guide_content)
            .setPositiveButton(R.string.ok, null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                imgCapture.setImageURI(selectedImage); // Hiển thị ảnh tạm thời
                uploadAvatarToCloudinary(selectedImage); // Upload ảnh lên Cloudinary
            }
        }

        if (requestCode == REQUEST_PICK_WIDGET && resultCode == RESULT_OK) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_home);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }

    private void uploadAvatarToCloudinary(Uri imageUri) {
        if (imageUri == null) {
            Log.e("ProfileActivity", "Image URI is null");
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
                        updateAvatarUrlInFirestore(avatarUrl); // Cập nhật URL vào Firestore
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

    private void updateAvatarUrlInFirestore(String avatarUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", avatarUrl);

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d("ProfileActivity", "Avatar URL updated successfully");
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.person_24px)
                    .into(imgCapture); // Hiển thị ảnh mới
            })
            .addOnFailureListener(e -> Log.e("ProfileActivity", "Failed to update avatar URL", e));
    }
}

