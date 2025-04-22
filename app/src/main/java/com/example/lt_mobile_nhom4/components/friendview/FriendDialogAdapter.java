package com.example.lt_mobile_nhom4.components.friendview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.models.FriendModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

@Setter
public class FriendDialogAdapter extends BaseAdapter {
    private Context context;
    private List<FriendModel> friendList;

    public FriendDialogAdapter(Context context) {
        this.context = context;
        this.friendList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return friendList.size();
    }

    @Override
    public Object getItem(int i) {
        return friendList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public static String removeAccent(String s) {
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public void setFriendList(List<FriendModel> friendList) {
        this.friendList = friendList;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FriendModel friend = friendList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        }

        ImageView friendAvatar = convertView.findViewById(R.id.friendAvatar);
        TextView friendUsername = convertView.findViewById(R.id.friendUsername);
        TextView friendFullName = convertView.findViewById(R.id.friendFullName);

        // Set user data
        friendUsername.setText("@" + friend.getUsername());
        friendFullName.setText(friend.getFullName());

        // Load avatar with Glide if available
        if (friend.getAvatarUrl() != null && !friend.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                .load(friend.getAvatarUrl())
                .placeholder(R.drawable.person_24px)
                .into(friendAvatar);
        } else {
            friendAvatar.setImageResource(R.drawable.person_24px);
        }

        return convertView;
    }
}
