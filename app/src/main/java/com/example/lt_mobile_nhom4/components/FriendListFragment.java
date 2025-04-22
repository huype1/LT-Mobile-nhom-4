package com.example.lt_mobile_nhom4.components;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lt_mobile_nhom4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendListFragment extends Fragment {

    private RecyclerView friendRecyclerView;
    private UserAdapter userAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private TextView noFriendsTextView;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        friendRecyclerView = view.findViewById(R.id.friendRecyclerView);
        progressBar = view.findViewById(R.id.friendListProgressBar);
        noFriendsTextView = view.findViewById(R.id.noFriendsTextView);

        userAdapter = new UserAdapter();
        userAdapter.setCurrentUserId(currentUserId);
        friendRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        friendRecyclerView.setAdapter(userAdapter);

        fetchFriendList();
    }

    private void fetchFriendList() {
        if (currentUserId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        noFriendsTextView.setVisibility(View.GONE); // Ensure it's hidden initially

        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists() && documentSnapshot.contains("friends")) {
                        List<User> friendList = new ArrayList<>();
                        List<String> friendIds = new ArrayList<>(documentSnapshot.getData().keySet());

                        if (friendIds.isEmpty()) {
                            noFriendsTextView.setVisibility(View.VISIBLE); // Show if no friends
                            return;
                        }

                        for (String friendId : friendIds) {
                            firestore.collection("users").document(friendId).get()
                                    .addOnSuccessListener(friendSnapshot -> {
                                        if (friendSnapshot.exists()) {
                                            User friend = friendSnapshot.toObject(User.class);
                                            friendList.add(friend);
                                            userAdapter.setUsers(friendList);

                                            // Hide "no friends" text if at least one friend is found
                                            noFriendsTextView.setVisibility(View.GONE);
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("FriendListFragment", "Error fetching friend data: ", e));
                        }
                    } else {
                        noFriendsTextView.setVisibility(View.VISIBLE); // Show if no friends field
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("FriendListFragment", "Error fetching friend list: ", e);
                    Toast.makeText(requireContext(), "Failed to fetch friend list", Toast.LENGTH_SHORT).show();
                });
    }
}
