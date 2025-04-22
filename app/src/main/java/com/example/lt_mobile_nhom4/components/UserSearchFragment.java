package com.example.lt_mobile_nhom4.components;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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

        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    Map<String, Object> currentUserFriends = new HashMap<>();
                    if (userSnapshot.contains("friends")) {
                        currentUserFriends = (Map<String, Object>) userSnapshot.get("friends");
                    }

                    final Map<String, Object> friendsData = currentUserFriends;

                    firestore.collection("users")
                            .orderBy("username")
                            .get()
                            .addOnCompleteListener(task -> {
                                searchProgressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
                                    List<User> matchedUsers = new ArrayList<>();

                                    for (QueryDocumentSnapshot document : task.getResult()) {


                                        String userId = document.getString("id");
                                        String username = document.getString("username");
                                        String fullName = document.getString("full-name");
                                        String phoneNumber = document.getString("phone-number");

                                        if ((username != null && username.toLowerCase().contains(searchQuery)) ||
                                                (fullName != null && fullName.toLowerCase().contains(searchQuery)) ||
                                                (phoneNumber != null && phoneNumber.contains(searchQuery))) {

                                            User user = new User();
                                            user.setId(userId);
                                            user.setUsername(username);
                                            user.setFullName(fullName);
                                            user.setPhoneNumber(phoneNumber);
                                            user.setEmail(document.getString("email"));
                                            user.setImageUrl(document.getString("image-url"));
                                            user.setCreatedAt(document.getString("created-at"));

                                            // Set up friend status
                                            Map<String, String> friends = new HashMap<>();
                                            if (document.contains("friends")) {
                                                Map<String, Object> userFriendsData = (Map<String, Object>) document.get("friends");
                                                if (userFriendsData != null) {
                                                    for (Map.Entry<String, Object> entry : userFriendsData.entrySet()) {
                                                        friends.put(entry.getKey(), (String) entry.getValue());
                                                    }
                                                }
                                            }
                                            user.setFriends(friends);

                                            // Set the correct friend status based on current user's friends data
                                            if (friendsData.containsKey(userId)) {
                                                String status = (String) friendsData.get(userId);
                                                user.setFriendStatus(currentUserId, User.FriendStatus.fromString(status));
                                            }

                                            matchedUsers.add(user);
                                        }
                                    }

                                    if (matchedUsers.isEmpty()) {
                                        noResultsTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        userAdapter.setUsers(matchedUsers);
                                        setupFriendStatusListener(matchedUsers);
                                    }

                                } else {
                                    Log.e(TAG, "Error getting documents: ", task.getException());
                                    Toast.makeText(requireContext(), "Error searching for users", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    searchProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching current user data: ", e);
                    Toast.makeText(requireContext(), "Error accessing user data", Toast.LENGTH_SHORT).show();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasNotificationPermission()) {
            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel existingChannel = notificationManager.getNotificationChannel("friend_requests");
            if (existingChannel == null) {
                CharSequence name = "Friend Requests";
                String description = "Notifications for friend requests";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("friend_requests", name, importance);
                channel.setDescription(description);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }



    @SuppressLint("MissingPermission")
    private void sendFriendRequestReceivedNotification(String friendName) {
        if (!hasNotificationPermission()) return;  // <- Chỉ gửi nếu có quyền

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "friend_requests")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Friend Request Received")
                .setContentText(friendName + " sent you a friend request.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(requireContext()).notify(1001, builder.build());
    }


    @SuppressLint("MissingPermission")
    private void sendFriendRequestAcceptedNotification(String friendName) {
        if (!hasNotificationPermission()) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "friend_requests")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Friend Request Accepted")
                .setContentText(friendName + " accepted your friend request.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(requireContext()).notify(1002, builder.build());
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
    private Map<String, String> previousFriendStatuses = new HashMap<>();
    private void setupFriendStatusListener(List<User> users) {
        if (currentUserId == null) return;

        firestore.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    Map<String, Object> friendsData = snapshot.contains("friends")
                            ? (Map<String, Object>) snapshot.get("friends")
                            : new HashMap<>();

                    for (int i = 0; i < users.size(); i++) {
                        User user = users.get(i);
                        String userId = user.getId();
                        String newStatus = friendsData.containsKey(userId) ? (String) friendsData.get(userId) : null;
                        String oldStatus = previousFriendStatuses.get(userId);

                        // So sánh và gửi thông báo phù hợp
                        if (newStatus != null && !newStatus.equals(oldStatus)) {
                            if ("pending_received".equals(newStatus) && (oldStatus == null || !"pending_received".equals(oldStatus))) {
                                sendFriendRequestReceivedNotification(user.getUsername());
                            } else if ("accepted".equals(newStatus) && "pending_sent".equals(oldStatus)) {
                                sendFriendRequestAcceptedNotification(user.getUsername());
                            }
                        }

                        // Cập nhật lại trạng thái đã lưu
                        if (newStatus != null) {
                            previousFriendStatuses.put(userId, newStatus);
                        } else {
                            previousFriendStatuses.remove(userId);
                        }

                        // Cập nhật UI nếu có thay đổi
                        user.setFriendStatus(currentUserId, User.FriendStatus.fromString(newStatus));
                        userAdapter.updateUser(user, i);
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
