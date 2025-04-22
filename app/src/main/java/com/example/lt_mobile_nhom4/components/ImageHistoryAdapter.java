package com.example.lt_mobile_nhom4.components;

import static android.view.View.GONE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lt_mobile_nhom4.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageHistoryAdapter extends RecyclerView.Adapter<ImageHistoryAdapter.ImageViewHolder> {
    private List<ImageHistory> imageHistories = new ArrayList<>();
    private FirebaseFirestore db;

    public ImageHistoryAdapter() {
        db = FirebaseFirestore.getInstance();
    }

    public void setImageHistories(List<ImageHistory> imageHistories) {
        this.imageHistories = imageHistories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_history, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageHistory imageHistory = imageHistories.get(position);

        if (imageHistory.getImageUrl() != null && !imageHistory.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageHistory.getImageUrl())
                    .into(holder.imageView);
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.imageView.setVisibility(View.GONE);
        }

        // Set description
        holder.descriptionText.setText(imageHistory.getDescription());

        db.collection("users").document(imageHistory.getUserId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Set username
                        String username = document.getString("username");
                        holder.usernameText.setText(username != null ? username : "Không tìm thấy người dùng");

                        // Load profile picture
                        String profilePicUrl = "https://t4.ftcdn.net/jpg/02/15/84/43/360_F_215844325_ttX9YiIIyeaR7Ne6EaLLjMAmy4GvPC69.jpg";
                        if (profilePicUrl != null) {
                            Glide.with(holder.itemView.getContext())
                                    .load(profilePicUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.avatar_circle_bg)
                                    .into(holder.userAvatarView);
                        } else {
                            holder.userAvatarView.setImageResource(R.drawable.avatar_circle_bg);
                        }
                    }
                });

        // Set timestamp
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - imageHistory.getTimestamp();
        long hoursDiff = timeDiff / (60 * 60 * 1000);

        SimpleDateFormat sdf;
        if (hoursDiff < 24) {
            // Within 24 hours - show only hour
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            // After 24 hours - show day and month
            sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        }
        String formattedDate = sdf.format(new Date(imageHistory.getTimestamp()));
        holder.timestampText.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return imageHistories.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView userAvatarView;
        TextView descriptionText;
        TextView usernameText;
        TextView timestampText;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.history_image);
            userAvatarView = itemView.findViewById(R.id.image_friend_avatar);
            descriptionText = itemView.findViewById(R.id.history_description);
            usernameText = itemView.findViewById(R.id.text_friend_name);
            timestampText = itemView.findViewById(R.id.history_timestamp);
        }
    }
}