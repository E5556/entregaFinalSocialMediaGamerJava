package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.optic.socialmediagamer.models.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UsersProvider {

    private CollectionReference mCollection;  /// metodo de firebase Se hace referencia a la coleccion de usuarios que quiere llamas

    public UsersProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("Users");
    }

    public Task<DocumentSnapshot> getUser(String id) {
        return mCollection.document(id).get();
    }

    public Task<Void> create(User user) {
        return mCollection.document(user.getId()).set(user);
    }

    public Query getAll() {
        return mCollection.orderBy("username", Query.Direction.ASCENDING);
    }

    public Query searchByUsername(String text) {
        return mCollection
                .orderBy("username")
                .startAt(text)
                .endAt(text + "\uf8ff");
    }

    public Task<Void> saveToken(String userId, String token) {
        Map<String, Object> map = new HashMap<>();
        map.put("fcmToken", token);
        return mCollection.document(userId).set(map, SetOptions.merge());
    }

    public Task<Void> setNowPlaying(String userId, String game) {
        Map<String, Object> map = new HashMap<>();
        map.put("nowPlaying", game);
        return mCollection.document(userId).set(map, SetOptions.merge());
    }

    public Task<Void> clearNowPlaying(String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("nowPlaying", "");
        return mCollection.document(userId).set(map, SetOptions.merge());
    }

    public Task<Void> update(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("phone", user.getPhone());
        map.put("bio", user.getBio() != null ? user.getBio() : "");
        map.put("timestamp", new Date().getTime());
        map.put("image_profile", user.getImageProfile());
        map.put("image_cover", user.getImageCover());
        return mCollection.document(user.getId()).update(map);
    }
}
