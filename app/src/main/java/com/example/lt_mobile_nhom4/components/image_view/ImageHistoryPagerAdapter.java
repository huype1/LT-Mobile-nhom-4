package com.example.lt_mobile_nhom4.components.image_view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.components.ImageHistory;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.NonNull;

public class ImageHistoryPagerAdapter extends RecyclerView.Adapter<ImageHistoryPagerAdapter.PagerViewHolder> {
    private List<ImageHistory> imageHistories = new ArrayList<>();
    private FirebaseFirestore db;

    public ImageHistoryPagerAdapter() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void setImageHistories(List<ImageHistory> imageHistories) {
        this.imageHistories = imageHistories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_history_full, parent, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new PagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
        ImageHistory imageHistory = imageHistories.get(position);

        // Load main image
        if (imageHistory.getImageUrl() != null && !imageHistory.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageHistory.getImageUrl())
                    .fitCenter()
                    .into(holder.imageView);
        }

        // Set description
        if (imageHistory.getDescription() != null && !imageHistory.getDescription().isEmpty()) {
            holder.descriptionText.setText(imageHistory.getDescription());
            holder.descriptionText.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionText.setVisibility(View.GONE);
        }

        // Load user data
        if (imageHistory.getUserId() != null) {
            db.collection("users").document(imageHistory.getUserId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            holder.userName.setText(username != null ? username : "Unknown User");

                            String profilePicUrl = document.getString("imageUrl");
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(profilePicUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.avatar_circle_bg)
                                        .into(holder.userAvatar);
                            } else {
                                holder.userAvatar.setImageResource(R.drawable.avatar_circle_bg);
                            }
                        }
                    });
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - imageHistory.getTimestamp();
        long hoursDiff = timeDiff / (60 * 60 * 1000);

        SimpleDateFormat sdf = hoursDiff < 24
            ? new SimpleDateFormat("HH:mm", Locale.getDefault())
            : new SimpleDateFormat("dd/MM", Locale.getDefault());

        holder.timestamp.setText(sdf.format(new Date(imageHistory.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return imageHistories.size();
    }

    static class PagerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView descriptionText;
        ImageView userAvatar;
        TextView userName;
        TextView timestamp;

        PagerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            userAvatar = itemView.findViewById(R.id.image_friend_avatar);
            userName = itemView.findViewById(R.id.text_friend_name);
            timestamp = itemView.findViewById(R.id.history_timestamp);
        }
    }
}
