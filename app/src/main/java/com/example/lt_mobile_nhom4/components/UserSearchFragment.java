package com.example.lt_mobile_nhom4.components;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lt_mobile_nhom4.MyApplication;
import com.example.lt_mobile_nhom4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
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

        firestore = MyApplication.getFirestore();
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

        createNotificationChannel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
            userAdapter.setCurrentUserId(currentUserId);

            String searchText = searchEditText.getText().toString().trim().toLowerCase();
            if (searchText.length() >= 2) {
                searchUsers(searchText);
            }
        }
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
                                
                                Map<String, String> friends = new HashMap<>();
                                if (document.contains("friends")) {
                                    Map<String, Object> friendsData = (Map<String, Object>) document.get("friends");

                                    Log.d(TAG, "Friends data: " + document.get("friends") + " " + friendsData);;
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
                            setupFriendStatusListener(matchedUsers);  // Add this line
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
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), User.FriendStatus.PENDING_SENT.getValue()
            );

            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, User.FriendStatus.PENDING_RECEIVED.getValue()
            );

            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request sent to " + user.getUsername(), Toast.LENGTH_SHORT).show();

            sendFriendRequestReceivedNotification(user.getUsername());  // Gửi thông báo cho người nhận yêu cầu

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
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), User.FriendStatus.ACCEPTED.getValue()
            );

            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, User.FriendStatus.ACCEPTED.getValue()
            );

            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "You are now friends with " + user.getUsername(), Toast.LENGTH_SHORT).show();

            sendFriendRequestAcceptedNotification(user.getUsername());  // Gửi thông báo cho người gửi yêu cầu

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
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), FieldValue.delete()
            );
            
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, FieldValue.delete()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
            
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
            transaction.update(
                    firestore.collection("users").document(currentUserId),
                    "friends." + user.getId(), FieldValue.delete()
            );
            
            transaction.update(
                    firestore.collection("users").document(user.getId()),
                    "friends." + currentUserId, FieldValue.delete()
            );
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            searchProgressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Friend request canceled", Toast.LENGTH_SHORT).show();
            
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

    private void createNotificationChannel() {
        if (hasNotificationPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Friend Requests";
                String description = "Notifications for friend requests";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("friend_requests", name, importance);
                channel.setDescription(description);

                NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        } else {
            requestNotificationPermission();
        }
    }

    private void sendFriendRequestReceivedNotification(String friendName) {
        try {
            String title = getString(R.string.friend_request_received_title);
            String content = String.format(getString(R.string.friend_request_received_content), friendName);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "friend_requests")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify(1, builder.build());

        } catch (SecurityException e) {
            Log.e("NotificationError", "Notification permission not granted: " + e.getMessage());
        }
    }

    private void sendFriendRequestAcceptedNotification(String friendName) {
        try {
            String title = getString(R.string.friend_request_accepted_title);
            String content = String.format(getString(R.string.friend_request_accepted_content), friendName);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "friend_requests")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify(2, builder.build());

        } catch (SecurityException e) {
            Log.e("NotificationError", "Notification permission not granted: " + e.getMessage());
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }


    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }
    private void setupFriendStatusListener(List<User> users) {
        if (currentUserId == null) return;

        // Get a snapshot of the current user document for real-time updates
        firestore.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    Map<String, Object> friendsData =
                            snapshot.contains("friends") ?
                                    (Map<String, Object>) snapshot.get("friends") :
                                    new HashMap<>();

                    // Update local users with latest friend status
                    for (int i = 0; i < users.size(); i++) {
                        User user = users.get(i);
                        if (friendsData.containsKey(user.getId())) {
                            String status = (String) friendsData.get(user.getId());
                            user.setFriendStatus(currentUserId,
                                    User.FriendStatus.fromString(status));
                            userAdapter.updateUser(user, i);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createNotificationChannel();
            } else {
                Toast.makeText(requireContext(), "Permission denied to send notifications", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
