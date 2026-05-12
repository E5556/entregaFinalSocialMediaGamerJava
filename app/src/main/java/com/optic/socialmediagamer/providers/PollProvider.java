package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.models.Poll;

public class PollProvider {

    private final CollectionReference mCollection;

    public PollProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("polls");
    }

    public Task<Void> save(Poll poll) {
        return mCollection.document(poll.getPostId()).set(poll);
    }

    public DocumentReference getByPost(String postId) {
        return mCollection.document(postId);
    }

    public Task<Void> voteA(String postId, String userId) {
        return mCollection.document(postId).update("votesA", FieldValue.arrayUnion(userId));
    }

    public Task<Void> removeVoteA(String postId, String userId) {
        return mCollection.document(postId).update("votesA", FieldValue.arrayRemove(userId));
    }

    public Task<Void> voteB(String postId, String userId) {
        return mCollection.document(postId).update("votesB", FieldValue.arrayUnion(userId));
    }

    public Task<Void> removeVoteB(String postId, String userId) {
        return mCollection.document(postId).update("votesB", FieldValue.arrayRemove(userId));
    }

    public ListenerRegistration listen(String postId, EventListener<DocumentSnapshot> listener) {
        return mCollection.document(postId).addSnapshotListener(listener);
    }
}
