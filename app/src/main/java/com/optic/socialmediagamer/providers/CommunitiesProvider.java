package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.models.Community;

import java.util.HashMap;
import java.util.Map;

public class CommunitiesProvider {

    private final FirebaseFirestore mFirestore;

    public CommunitiesProvider() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    public Task<DocumentReference> save(Community community) {
        return mFirestore.collection("communities").add(community);
    }

    public Query getAll() {
        return mFirestore.collection("communities").orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query searchByName(String text) {
        return mFirestore.collection("communities")
                .orderBy("name")
                .startAt(text)
                .endAt(text + "");
    }

    public Task<Void> join(String communityId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("members." + userId, true);
        update.put("memberCount", FieldValue.increment(1));
        return mFirestore.collection("communities").document(communityId).update(update);
    }

    public Task<Void> leave(String communityId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("members." + userId, FieldValue.delete());
        update.put("memberCount", FieldValue.increment(-1));
        return mFirestore.collection("communities").document(communityId).update(update);
    }

    public Task<com.google.firebase.firestore.DocumentSnapshot> getCommunity(String communityId) {
        return mFirestore.collection("communities").document(communityId).get();
    }

    public Query getPostsByCommunity(String communityId) {
        return mFirestore.collection("communityPosts")
                .whereEqualTo("idCommunity", communityId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<DocumentReference> savePost(com.optic.socialmediagamer.models.Post post, String communityId) {
        Map<String, Object> data = new HashMap<>();
        data.put("idCommunity", communityId);
        data.put("idUser", post.getIdUser());
        data.put("title", post.getTitle());
        data.put("description", post.getDescription());
        data.put("image1", post.getImage1());
        data.put("timestamp", post.getTimestamp());
        return mFirestore.collection("communityPosts").add(data);
    }
}
