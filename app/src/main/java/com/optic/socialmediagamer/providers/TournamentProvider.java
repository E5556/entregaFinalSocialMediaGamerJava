package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.models.Tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentProvider {

    private final CollectionReference mCollection;

    public TournamentProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("tournaments");
    }

    public Task<Void> create(Tournament t) {
        DocumentReference ref = mCollection.document();
        t.setId(ref.getId());
        return ref.set(t);
    }

    public Query getAll() {
        return mCollection.orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<com.google.firebase.firestore.DocumentSnapshot> getById(String id) {
        return mCollection.document(id).get();
    }

    public Task<Void> join(String tournamentId, String uid) {
        return mCollection.document(tournamentId).update("players", FieldValue.arrayUnion(uid));
    }

    public Task<Void> leave(String tournamentId, String uid) {
        return mCollection.document(tournamentId).update("players", FieldValue.arrayRemove(uid));
    }

    /** Shuffles players, pairs them into bracket, sets status = started */
    public Task<Void> start(String tournamentId, List<String> players) {
        List<String> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        List<String> bracket = new ArrayList<>();
        for (int i = 0; i + 1 < shuffled.size(); i += 2) {
            bracket.add(shuffled.get(i) + "|" + shuffled.get(i + 1));
        }
        // If odd player count, last player gets a bye
        if (shuffled.size() % 2 != 0) {
            bracket.add(shuffled.get(shuffled.size() - 1) + "|BYE");
        }

        Map<String, Object> update = new HashMap<>();
        update.put("bracket", bracket);
        update.put("status", "started");
        return mCollection.document(tournamentId).update(update);
    }

    public Task<Void> setWinner(String tournamentId, String winnerUid, String winnerUsername) {
        Map<String, Object> update = new HashMap<>();
        update.put("winner", winnerUid);
        update.put("winnerUsername", winnerUsername);
        update.put("status", "finished");
        return mCollection.document(tournamentId).update(update);
    }
}
