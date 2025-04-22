package com.example.lt_mobile_nhom4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

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

import com.cloudinary.android.MediaManager;
import com.example.lt_mobile_nhom4.components.UserSearchFragment;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.example.lt_mobile_nhom4.components.image_view.ImageHistoryFragment;
import com.example.lt_mobile_nhom4.utils.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
//    private String cloudName = "CLOUDINARY_URL=cloudinary://117691381147521:Q5uRqKIvX094XNSXkekVHZIFGqM@dkjha8fug";
//    Button logoutButton;
//    Button searchButton;
    ImageView imgProfile;
    SharedPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initCloudinary();
        prefsManager = SharedPreferencesManager.getInstance(this);
//        logoutButton = findViewById(R.id.logoutButton);
//        searchButton = findViewById(R.id.searchButton);
//        imgProfile = findViewById(R.id.img_profile);

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
