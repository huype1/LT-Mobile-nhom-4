package com.example.lt_mobile_nhom4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.cloudinary.android.MediaManager;
import com.example.lt_mobile_nhom4.components.UserSearchFragment;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
//    private String cloudName = "CLOUDINARY_URL=cloudinary://117691381147521:Q5uRqKIvX094XNSXkekVHZIFGqM@dkjha8fug";
    Button logoutButton;
    Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Fix this later
        FirebaseAuth autho = FirebaseAuth.getInstance();
        if (autho.getCurrentUser() == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        initCloudinary();
        logoutButton = findViewById(R.id.logoutButton);
        searchButton = findViewById(R.id.searchButton);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        });

        searchButton.setOnClickListener(v -> {
            openUserSearchFragment();
        });

        // Add this to show camera fragment by default
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

//    private void initCloudinary() {
//        try {
//            Map config = new HashMap();
//            config.put("cloud_name", cloudName);
//            MediaManager.init(this, config);
//        } catch (IllegalStateException e) {
//            Log.d("Cloudinary", "Cloudinary is already initialized");
//        }
//    }
}
