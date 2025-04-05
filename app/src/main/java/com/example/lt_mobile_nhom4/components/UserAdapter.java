package com.example.lt_mobile_nhom4.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.lt_mobile_nhom4.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList = new ArrayList<>();
    private OnUserClickListener listener;
    private FriendRequestListener friendRequestListener;
    private String currentUserId;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface FriendRequestListener {
        void onSendFriendRequest(User user, int position);
        void onAcceptFriendRequest(User user, int position);
        void onRejectFriendRequest(User user, int position);
        void onCancelFriendRequest(User user, int position);
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setFriendRequestListener(FriendRequestListener listener) {
        this.friendRequestListener = listener;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void setUsers(List<User> users) {
        this.userList = users;
        notifyDataSetChanged();
    }

    public void updateUser(User updatedUser, int position) {
        if (position >= 0 && position < userList.size()) {
            userList.set(position, updatedUser);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView userImageView;
        private TextView usernameTextView;
        private TextView fullNameTextView;
        private Button addFriendButton;
        private Button acceptButton;
        private Button rejectButton;
        private Button cancelRequestButton;
        private TextView friendStatusText;
        private LinearLayout requestButtonsLayout;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userImageView = itemView.findViewById(R.id.userImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            fullNameTextView = itemView.findViewById(R.id.fullNameTextView);
            addFriendButton = itemView.findViewById(R.id.addFriendButton);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            cancelRequestButton = itemView.findViewById(R.id.cancelRequestButton);
            friendStatusText = itemView.findViewById(R.id.friendStatusText);
            requestButtonsLayout = itemView.findViewById(R.id.requestButtonsLayout);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(userList.get(position));
                }
            });

            addFriendButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && friendRequestListener != null) {
                    friendRequestListener.onSendFriendRequest(userList.get(position), position);
                }
            });

            acceptButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && friendRequestListener != null) {
                    friendRequestListener.onAcceptFriendRequest(userList.get(position), position);
                }
            });

            rejectButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && friendRequestListener != null) {
                    friendRequestListener.onRejectFriendRequest(userList.get(position), position);
                }
            });

            cancelRequestButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && friendRequestListener != null) {
                    friendRequestListener.onCancelFriendRequest(userList.get(position), position);
                }
            });
        }

        public void bind(User user, int position) {
            usernameTextView.setText(user.getUsername());
            fullNameTextView.setText(user.getFullName());
            
            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                Glide.with(userImageView.getContext())
                        .load(user.getImageUrl())
                        .apply(RequestOptions.centerCropTransform())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(userImageView);
            } else {
                userImageView.setImageResource(R.drawable.ic_launcher_background);
            }

            // Don't show friend controls for current user
            if (currentUserId != null && currentUserId.equals(user.getId())) {
                addFriendButton.setVisibility(View.GONE);
                cancelRequestButton.setVisibility(View.GONE);
                requestButtonsLayout.setVisibility(View.GONE);
                friendStatusText.setVisibility(View.GONE);
                return;
            }
            
            // Update UI based on friendship status
            User.FriendStatus status = user.getFriendStatus(currentUserId);
            
            switch (status) {
                case ACCEPTED:
                    addFriendButton.setVisibility(View.GONE);
                    cancelRequestButton.setVisibility(View.GONE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    friendStatusText.setVisibility(View.VISIBLE);
                    friendStatusText.setText("Friends");
                    break;
                    
                case PENDING_SENT:
                    addFriendButton.setVisibility(View.GONE);
                    cancelRequestButton.setVisibility(View.VISIBLE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    friendStatusText.setVisibility(View.GONE);
                    break;
                    
                case PENDING_RECEIVED:
                    addFriendButton.setVisibility(View.GONE);
                    cancelRequestButton.setVisibility(View.GONE);
                    requestButtonsLayout.setVisibility(View.VISIBLE);
                    friendStatusText.setVisibility(View.GONE);
                    break;
                    
                default: // NONE
                    addFriendButton.setVisibility(View.VISIBLE);
                    cancelRequestButton.setVisibility(View.GONE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    friendStatusText.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
