package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class TournamentAlertProvider {

    private final CollectionReference mCollection;

    public TournamentAlertProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("tournament_subscriptions");
    }

    private String docId(String userId, String game) {
        return userId + "_" + game.toLowerCase().replaceAll("\\s+", "_");
    }

    public Task<Void> subscribe(String userId, String game) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("game", game.toLowerCase().trim());
        data.put("gameDisplay", game.trim());
        data.put("timestamp", System.currentTimeMillis());
        return mCollection.document(docId(userId, game)).set(data);
    }

    public Task<Void> unsubscribe(String userId, String game) {
        return mCollection.document(docId(userId, game)).delete();
    }

    public Task<QuerySnapshot> getSubscriptions(String userId) {
        return mCollection.whereEqualTo("userId", userId).get();
    }

    public Task<QuerySnapshot> getSubscribers(String game) {
        return mCollection.whereEqualTo("game", game.toLowerCase().trim()).get();
    }
}
