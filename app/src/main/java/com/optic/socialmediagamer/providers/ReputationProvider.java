package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.models.GameRating;

public class ReputationProvider {

    private final CollectionReference mCollection;

    public ReputationProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("game_ratings");
    }

    public Task<Void> save(GameRating rating) {
        DocumentReference ref = mCollection.document();
        rating.setId(ref.getId());
        return ref.set(rating);
    }

    public Task<QuerySnapshot> getByUser(String userId) {
        return mCollection.whereEqualTo("idTo", userId).get();
    }

    /** Returns the existing rating doc if idFrom already rated idTo for game */
    public Task<QuerySnapshot> hasRated(String idFrom, String idTo, String game) {
        return mCollection
                .whereEqualTo("idFrom", idFrom)
                .whereEqualTo("idTo", idTo)
                .whereEqualTo("game", game)
                .get();
    }
}
