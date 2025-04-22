package com.example.lt_mobile_nhom4.components.friendview;

import java.util.ArrayList;
import java.util.List;

public class FriendData {
    public static List<Friend> getFriends() {
        List<Friend> friends = new ArrayList<>();

        Friend an = new Friend("An", R.drawable.an);
        an.addPhoto(R.drawable.an1);
        an.addPhoto(R.drawable.an2);
        an.addPhoto(R.drawable.an3);
        for (Photo photo : an.getPhotos()) photo.setFriend(an);

        Friend binh = new Friend("Bình", R.drawable.binh);
        binh.addPhoto(R.drawable.binh1);
        binh.addPhoto(R.drawable.binh2);
        for (Photo photo : binh.getPhotos()) photo.setFriend(binh);

        Friend chi = new Friend("Chi", R.drawable.chi);
        chi.addPhoto(R.drawable.chi1);
        chi.addPhoto(R.drawable.chi2);
        chi.addPhoto(R.drawable.chi3);
        for (Photo photo : chi.getPhotos()) photo.setFriend(chi);

        Friend dung = new Friend("Dung", R.drawable.dung);
        dung.addPhoto(R.drawable.dung1);
        dung.addPhoto(R.drawable.dung2);
        for (Photo photo : dung.getPhotos()) photo.setFriend(dung);

        Friend en = new Friend("Én", R.drawable.en);
        en.addPhoto(R.drawable.en1);
        en.addPhoto(R.drawable.en2);
        for (Photo photo : en.getPhotos()) photo.setFriend(en);

        Friend giap = new Friend("Giáp", R.drawable.giap);
        giap.addPhoto(R.drawable.giap1);
        giap.addPhoto(R.drawable.giap2);
        for (Photo photo : giap.getPhotos()) photo.setFriend(giap);

        Friend huy = new Friend("Huy", R.drawable.huy);
        huy.addPhoto(R.drawable.huy1);
        huy.addPhoto(R.drawable.huy2);
        huy.addPhoto(R.drawable.huy3);
        for (Photo photo : huy.getPhotos()) photo.setFriend(huy);

        Friend khanh = new Friend("Khánh", R.drawable.khanh);
        khanh.addPhoto(R.drawable.khanh1);
        khanh.addPhoto(R.drawable.khanh2);
        khanh.addPhoto(R.drawable.khanh3);
        for (Photo photo : khanh.getPhotos()) photo.setFriend(khanh);

        Friend lien = new Friend("Liên", R.drawable.lien);
        lien.addPhoto(R.drawable.lien1);
        lien.addPhoto(R.drawable.lien2);
        lien.addPhoto(R.drawable.lien3);
        for (Photo photo : lien.getPhotos()) photo.setFriend(lien);

        friends.add(an);
        friends.add(binh);
        friends.add(chi);
        friends.add(dung);
        friends.add(en);
        friends.add(giap);
        friends.add(huy);
        friends.add(khanh);
        friends.add(lien);

        return friends;
    }
}
