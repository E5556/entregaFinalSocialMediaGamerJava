package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.optic.socialmediagamer.models.WeeklyChallenge;
import com.optic.socialmediagamer.models.WeeklyChallengeEntry;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WeeklyChallengeProvider {

    private final CollectionReference mCollection;

    public WeeklyChallengeProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("weekly_challenges");
    }

    public long getWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Creates or overwrites the challenge for this week. */
    public Task<Void> createWeeklyChallenge(String title, String description) {
        String docId = String.valueOf(getWeekStart());
        WeeklyChallenge wc = new WeeklyChallenge();
        wc.setId(docId);
        wc.setTitle(title);
        wc.setDescription(description);
        wc.setWeekStart(getWeekStart());
        wc.setStatus("active");
        return mCollection.document(docId).set(wc);
    }

    public Task<com.google.firebase.firestore.DocumentSnapshot> getThisWeek() {
        return mCollection.document(String.valueOf(getWeekStart())).get();
    }

    public Task<QuerySnapshot> getEntries(String challengeId) {
        return mCollection.document(challengeId)
                .collection("entries")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get();
    }

    public Task<Void> submitEntry(String challengeId, WeeklyChallengeEntry entry) {
        CollectionReference entries = mCollection.document(challengeId).collection("entries");
        DocumentReference ref = entries.document();
        entry.setId(ref.getId());
        return ref.set(entry);
    }

    public Task<Void> vote(String challengeId, String entryId, String userId) {
        return mCollection.document(challengeId)
                .collection("entries")
                .document(entryId)
                .update("votes", FieldValue.arrayUnion(userId));
    }

    public Task<Void> unvote(String challengeId, String entryId, String userId) {
        return mCollection.document(challengeId)
                .collection("entries")
                .document(entryId)
                .update("votes", FieldValue.arrayRemove(userId));
    }

    public Task<Void> setWinner(String challengeId, String winnerId, String winnerUsername) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "finished");
        update.put("winnerId", winnerId);
        update.put("winnerUsername", winnerUsername);
        return mCollection.document(challengeId).set(update, SetOptions.merge());
    }
}
