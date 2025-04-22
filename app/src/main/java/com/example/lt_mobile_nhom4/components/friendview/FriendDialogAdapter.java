package com.example.lt_mobile_nhom4.components.friendview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.lt_mobile_nhom4.R;

import java.text.Normalizer;
import java.util.List;

public class FriendDialogAdapter extends BaseAdapter {
    private Context context;
    private List<Friend> friendList;

    public FriendDialogAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList = friendList;
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


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Friend friend = friendList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dialog_item_friend, parent, false);
        }

        ImageView imageAvatar = convertView.findViewById(R.id.imageAvatar);
        TextView textName = convertView.findViewById(R.id.textFriendName);
        ImageView imageArrow = convertView.findViewById(R.id.imageArrow);

        String rawName = friend.getName().toLowerCase();
        String imageName = removeAccent(rawName);
        int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());

        if (resId != 0) {
            imageAvatar.setImageResource(resId);
        } else {
            imageAvatar.setImageResource(R.drawable.person_24px);
        }

        textName.setText(friend.getName());

        return convertView;
    }
}