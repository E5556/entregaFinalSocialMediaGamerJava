package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.models.Collection;

import java.util.Date;

public class CollectionsProvider {

    private final CollectionReference mCollection;

    public CollectionsProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("collections");
    }

    public Task<Void> create(String userId, String name) {
        String id = mCollection.document().getId();
        Collection col = new Collection(userId, name, new Date().getTime());
        col.setId(id);
        return mCollection.document(id).set(col);
    }

    public Query getByUser(String userId) {
        return mCollection.whereEqualTo("idUser", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<DocumentSnapshot> get(String collectionId) {
        return mCollection.document(collectionId).get();
    }

    public Task<Void> addPost(String collectionId, String postId) {
        return mCollection.document(collectionId)
                .update("postIds", FieldValue.arrayUnion(postId));
    }

    public Task<Void> removePost(String collectionId, String postId) {
        return mCollection.document(collectionId)
                .update("postIds", FieldValue.arrayRemove(postId));
    }

    public Task<Void> delete(String collectionId) {
        return mCollection.document(collectionId).delete();
    }
}
