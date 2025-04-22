package com.example.lt_mobile_nhom4.components.friendview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private final Friend friend;
    private final List<Photo> photoList;

    public PhotoAdapter(Context context, Friend friend, List<Photo> photoList) {
        this.context = context;
        this.friend = friend;
        this.photoList = photoList;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photoList.get(position);
        holder.imageView.setImageResource(photo.getResId());

        // 👉 Khi bấm vào ảnh, truyền cả friend và selected_index
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PhotoDetailActivity.class);
            intent.putExtra("friend", friend);
            intent.putExtra("selected_index", position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_item);
        }
    }
}
