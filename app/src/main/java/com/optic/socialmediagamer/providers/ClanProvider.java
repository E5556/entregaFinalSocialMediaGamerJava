package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.optic.socialmediagamer.models.Clan;

import java.util.HashMap;
import java.util.Map;

public class ClanProvider {

    private final CollectionReference mCollection;

    public ClanProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("clans");
    }

    public Task<Void> create(Clan clan) {
        DocumentReference ref = mCollection.document();
        clan.setId(ref.getId());
        return ref.set(clan);
    }

    public Task<DocumentSnapshot> get(String clanId) {
        return mCollection.document(clanId).get();
    }

    public Task<QuerySnapshot> getByMember(String userId) {
        return mCollection.whereArrayContains("members", userId).get();
    }

    public Query getAll() {
        return mCollection.orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<Void> join(String clanId, String userId) {
        return mCollection.document(clanId).update("members", FieldValue.arrayUnion(userId));
    }

    public Task<Void> leave(String clanId, String userId) {
        return mCollection.document(clanId).update("members", FieldValue.arrayRemove(userId));
    }

    public Task<Void> kick(String clanId, String userId) {
        return mCollection.document(clanId).update("members", FieldValue.arrayRemove(userId));
    }

    public Task<Void> updateInfo(String clanId, String description) {
        Map<String, Object> update = new HashMap<>();
        update.put("description", description);
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> delete(String clanId) {
        return mCollection.document(clanId).delete();
    }
}
