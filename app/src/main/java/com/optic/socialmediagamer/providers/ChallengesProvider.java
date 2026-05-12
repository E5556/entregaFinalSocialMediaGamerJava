package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.optic.socialmediagamer.models.Challenge;

import java.util.HashMap;
import java.util.Map;

public class ChallengesProvider {

    private final CollectionReference mCollection;

    public ChallengesProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("challenges");
    }

    public Task<Void> send(Challenge c) {
        DocumentReference ref = mCollection.document();
        c.setId(ref.getId());
        return ref.set(c);
    }

    public Task<com.google.firebase.firestore.DocumentSnapshot> get(String id) {
        return mCollection.document(id).get();
    }

    public Query getSent(String userId) {
        return mCollection.whereEqualTo("idChallenger", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getReceived(String userId) {
        return mCollection.whereEqualTo("idChallenged", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getForVoting() {
        return mCollection.whereEqualTo("status", "voting")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<Void> accept(String id) {
        return mCollection.document(id).update("status", "accepted");
    }

    public Task<Void> reject(String id) {
        return mCollection.document(id).update("status", "rejected");
    }

    public Task<Void> submitEvidenceChallenger(String id, String evidence) {
        Map<String, Object> update = new HashMap<>();
        update.put("evidenceChallenger", evidence);
        // If both have evidence move to voting automatically — handled in Activity
        return mCollection.document(id).set(update, SetOptions.merge());
    }

    public Task<Void> submitEvidenceChallenged(String id, String evidence) {
        Map<String, Object> update = new HashMap<>();
        update.put("evidenceChallenged", evidence);
        return mCollection.document(id).set(update, SetOptions.merge());
    }

    public Task<Void> openVoting(String id) {
        return mCollection.document(id).update("status", "voting");
    }

    public Task<Void> voteChallenger(String challengeId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("votesChallenger", FieldValue.arrayUnion(userId));
        update.put("votesChallenged", FieldValue.arrayRemove(userId));
        return mCollection.document(challengeId).set(update, SetOptions.merge());
    }

    public Task<Void> voteChallenged(String challengeId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("votesChallenged", FieldValue.arrayUnion(userId));
        update.put("votesChallenger", FieldValue.arrayRemove(userId));
        return mCollection.document(challengeId).set(update, SetOptions.merge());
    }

    public Task<Void> finish(String challengeId, String winnerUid, String winnerUsername) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "finished");
        update.put("winner", winnerUid);
        update.put("winnerUsername", winnerUsername);
        return mCollection.document(challengeId).update(update);
    }
}
