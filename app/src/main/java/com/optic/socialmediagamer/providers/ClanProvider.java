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
import java.util.List;
import java.util.Map;

public class ClanProvider {

    private final CollectionReference mCollection;

    public ClanProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("clans");
    }

    public Task<Void> create(Clan clan) {
        DocumentReference ref = mCollection.document();
        clan.setId(ref.getId());
        clan.setActive(true);
        return ref.set(clan);
    }

    public Task<DocumentSnapshot> get(String clanId) {
        return mCollection.document(clanId).get();
    }

    public Task<QuerySnapshot> getByMember(String userId) {
        return mCollection.whereArrayContains("members", userId)
                .whereEqualTo("active", true).get();
    }

    public Query getAll() {
        // filter active client-side to avoid composite index requirement
        return mCollection.orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getArchived() {
        return mCollection.orderBy("dissolvedAt", Query.Direction.DESCENDING);
    }

    public Task<Void> requestJoin(String clanId, String userId) {
        return mCollection.document(clanId).update("pendingMembers", FieldValue.arrayUnion(userId));
    }

    public Task<Void> approveJoin(String clanId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("members", FieldValue.arrayUnion(userId));
        update.put("pendingMembers", FieldValue.arrayRemove(userId));
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> rejectJoin(String clanId, String userId) {
        return mCollection.document(clanId).update("pendingMembers", FieldValue.arrayRemove(userId));
    }

    public Task<Void> join(String clanId, String userId) {
        return mCollection.document(clanId).update("members", FieldValue.arrayUnion(userId));
    }

    public Task<Void> leave(String clanId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("members", FieldValue.arrayRemove(userId));
        update.put("officers", FieldValue.arrayRemove(userId));
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> kick(String clanId, String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("members", FieldValue.arrayRemove(userId));
        update.put("officers", FieldValue.arrayRemove(userId));
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> updateInfo(String clanId, String description) {
        Map<String, Object> update = new HashMap<>();
        update.put("description", description);
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> dissolve(String clanId) {
        Map<String, Object> update = new HashMap<>();
        update.put("active", false);
        update.put("dissolvedAt", System.currentTimeMillis());
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Task<Void> transferLeadership(String clanId, String newLeaderId) {
        return mCollection.document(clanId).update("idLeader", newLeaderId);
    }

    public Task<Void> delete(String clanId) {
        return mCollection.document(clanId).delete();
    }

    public Task<Void> promoteToOfficer(String clanId, String userId) {
        return mCollection.document(clanId).update("officers", FieldValue.arrayUnion(userId));
    }

    public Task<Void> demoteToMember(String clanId, String userId) {
        return mCollection.document(clanId).update("officers", FieldValue.arrayRemove(userId));
    }

    public Task<Void> addClanXP(String clanId, long amount) {
        Map<String, Object> update = new HashMap<>();
        update.put("clanXp", FieldValue.increment(amount));
        return mCollection.document(clanId).set(update, SetOptions.merge());
    }

    public Query getRanking() {
        return mCollection.orderBy("clanXp", Query.Direction.DESCENDING).limit(20);
    }

    /** Awards clanXp to the clan of a given user, if they belong to one. */
    public void awardClanXPForUser(String userId, long amount) {
        getByMember(userId).addOnSuccessListener(snap -> {
            List<com.google.firebase.firestore.DocumentSnapshot> docs = snap.getDocuments();
            if (!docs.isEmpty()) {
                String clanId = docs.get(0).getId();
                addClanXP(clanId, amount);
            }
        });
    }
}
