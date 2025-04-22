package com.example.lt_mobile_nhom4.components.friendview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lt_mobile_nhom4.R;

import java.io.File;
import java.util.List;

public class PhotoDetailAdapter extends RecyclerView.Adapter<PhotoDetailAdapter.ViewHolder> {

    private final List<Photo> photoList;

    public PhotoDetailAdapter(List<Photo> photoList) {
        this.photoList = photoList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.image_detail);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Photo photo = photoList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(new File(photo.getFilePath()))
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }
}
