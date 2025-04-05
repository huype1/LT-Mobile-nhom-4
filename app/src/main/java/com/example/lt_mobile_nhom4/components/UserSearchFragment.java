package com.example.lt_mobile_nhom4.components;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSearchFragment extends Fragment implements UserAdapter.FriendRequestListener {

    private static final String TAG = "UserSearchFragment";
    private EditText searchEditText;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private ProgressBar searchProgressBar;
    private TextView noResultsTextView;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        searchEditText = view.findViewById(R.id.searchEditText);
        userRecyclerView = view.findViewById(R.id.userRecyclerView);
        searchProgressBar = view.findViewById(R.id.searchProgressBar);
        noResultsTextView = view.findViewById(R.id.noResultsTextView);

        userAdapter = new UserAdapter();
        userAdapter.setCurrentUserId(currentUserId);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        userRecyclerView.setAdapter(userAdapter);

        userAdapter.setOnUserClickListener(user -> {
            Toast.makeText(requireContext(), "Selected user: " + user.getUsername(), Toast.LENGTH_SHORT).show();
        });
        
        userAdapter.setFriendRequestListener(this);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString().trim().toLowerCase();
                if (searchText.length() >= 2) {
                    searchUsers(searchText);
                } else if (searchText.isEmpty()) {
                    userAdapter.setUsers(new ArrayList<>());
                    noResultsTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void searchUsers(String searchQuery) {
        searchProgressBar.setVisibility(View.VISIBLE);
        noResultsTextView.setVisibility(View.GONE);

        firestore.collection("users")
                .orderBy("username")
                .get()
                .addOnCompleteListener(task -> {
                    searchProgressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        List<User> matchedUsers = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Get user data
                            String username = document.getString("username");
                            String fullName = document.getString("full-name");
                            String phoneNumber = document.getString("phone-number");
                            
                            if ((username != null && username.toLowerCase().contains(searchQuery)) ||
                                (fullName != null && fullName.toLowerCase().contains(searchQuery)) ||
                                (phoneNumber != null && phoneNumber.contains(searchQuery))) {
                                
                                User user = new User();
                                user.setId(document.getString("id"));
                                user.setUsername(username);
                                user.setFullName(fullName);
                                user.setPhoneNumber(phoneNumber);
                                user.setEmail(document.getString("email"));
                                user.setImageUrl(document.getString("image-url"));
                                user.setCreatedAt(document.getString("created-at"));
                                
                                // Get friends data
                                Map<String, String> friends = new HashMap<>();
                                if (document.contains("friends")) {
                                    Map<String, Object> friendsData = (Map<String, Object>) document.get("friends");
                                    if (friendsData != null) {
                                        for (Map.Entry<String, Object> entry : friendsData.entrySet()) {
                                            friends.put(entry.getKey(), (String) entry.getValue());
                                        }
                                    }
                                }
                                user.setFriends(friends);
                                
                                matchedUsers.add(user);
                            }
                        }
                        
                        if (matchedUsers.isEmpty()) {
                            noResultsTextView.setVisibility(View.VISIBLE);
                        } else {
                            userAdapter.setUsers(matchedUsers);
                        }
                        
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(requireContext(), "Error searching for users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSendFriendRequest(User user, int position) {
        if (currentUserId == null) return;
        
        searchProgressBar.setVisibility(View.VISIBLE);
        
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Update current user document - add sent request
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), User.FriendStatus.PENDING_SENT.getValue()
            );
            
            // Update other user document - add received request
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, User.FriendStatus.PENDING_RECEIVED.getValue()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request sent to " + user.getUsername(), Toast.LENGTH_SHORT).show();
            
            // Update local user object to reflect the change
            user.setFriendStatus(currentUserId, User.FriendStatus.PENDING_SENT);
            userAdapter.updateUser(user, position);
            
        }).addOnFailureListener(e -> {
            searchProgressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error sending friend request: ", e);
            Toast.makeText(requireContext(), "Failed to send friend request", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onAcceptFriendRequest(User user, int position) {
        if (currentUserId == null) return;
        
        searchProgressBar.setVisibility(View.VISIBLE);
        
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Update current user document - accept request
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), User.FriendStatus.ACCEPTED.getValue()
            );
            
            // Update other user document - accept request
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, User.FriendStatus.ACCEPTED.getValue()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "You are now friends with " + user.getUsername(), Toast.LENGTH_SHORT).show();
            
            // Update local user object to reflect the change
            user.setFriendStatus(currentUserId, User.FriendStatus.ACCEPTED);
            userAdapter.updateUser(user, position);
            
        }).addOnFailureListener(e -> {
            searchProgressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error accepting friend request: ", e);
            Toast.makeText(requireContext(), "Failed to accept friend request", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRejectFriendRequest(User user, int position) {
        if (currentUserId == null) return;
        
        searchProgressBar.setVisibility(View.VISIBLE);
        
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Remove friend entry from current user document
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), FieldValue.delete()
            );
            
            // Remove friend entry from other user document
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, FieldValue.delete()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
            
            // Update local user object to reflect the change
            if (user.getFriends() != null) {
                user.getFriends().remove(currentUserId);
            }
            userAdapter.updateUser(user, position);
            
        }).addOnFailureListener(e -> {
            searchProgressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error rejecting friend request: ", e);
            Toast.makeText(requireContext(), "Failed to reject friend request", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCancelFriendRequest(User user, int position) {
        if (currentUserId == null) return;
        
        searchProgressBar.setVisibility(View.VISIBLE);
        
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Remove friend entry from current user document
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), FieldValue.delete()
            );
            
            // Remove friend entry from other user document
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, FieldValue.delete()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request canceled", Toast.LENGTH_SHORT).show();
            
            // Update local user object to reflect the change
            if (user.getFriends() != null) {
                user.getFriends().remove(currentUserId);
            }
            userAdapter.updateUser(user, position);
            
        }).addOnFailureListener(e -> {
            searchProgressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error canceling friend request: ", e);
            Toast.makeText(requireContext(), "Failed to cancel friend request", Toast.LENGTH_SHORT).show();
        });
    }
}
