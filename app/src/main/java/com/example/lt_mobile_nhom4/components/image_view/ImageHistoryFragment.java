package com.example.lt_mobile_nhom4.components.image_view;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.components.ImageHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageHistoryFragment extends Fragment {
    private ViewPager2 viewPager;
    private ImageHistoryPagerAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

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

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Load image histories from Firestore
        loadImageHistory();

        return view;
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

}
