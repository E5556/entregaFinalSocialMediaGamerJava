package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MissionsProvider {

    public static final String TYPE_POST    = "POST";
    public static final String TYPE_LIKE    = "LIKE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_FOLLOW  = "FOLLOW";
    public static final String TYPE_LFG     = "LFG";

    private final FirebaseFirestore mFirestore;

    public MissionsProvider() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    public DocumentReference getDocRef(String userId) {
        return mFirestore.collection("weekly_missions").document(userId);
    }

    public Task<DocumentSnapshot> get(String userId) {
        return getDocRef(userId).get();
    }

    /** Increments progress for a mission type; resets all progress if it's a new week. */
    public Task<Void> incrementProgress(String userId, String type) {
        long currentWeekStart = getWeekStart();
        DocumentReference ref = getDocRef(userId);

        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            long savedWeekStart = snap.getLong("weekStart") != null ? snap.getLong("weekStart") : 0L;

            Map<String, Object> update = new HashMap<>();

            if (savedWeekStart < currentWeekStart) {
                // New week: reset progress and claimed maps
                Map<String, Long> freshProgress = new HashMap<>();
                freshProgress.put(TYPE_POST, 0L);
                freshProgress.put(TYPE_LIKE, 0L);
                freshProgress.put(TYPE_COMMENT, 0L);
                freshProgress.put(TYPE_FOLLOW, 0L);
                freshProgress.put(TYPE_LFG, 0L);
                freshProgress.put(type, 1L);

                Map<String, Boolean> freshClaimed = new HashMap<>();
                freshClaimed.put(TYPE_POST, false);
                freshClaimed.put(TYPE_LIKE, false);
                freshClaimed.put(TYPE_COMMENT, false);
                freshClaimed.put(TYPE_FOLLOW, false);
                freshClaimed.put(TYPE_LFG, false);

                update.put("weekStart", currentWeekStart);
                update.put("progress", freshProgress);
                update.put("claimed", freshClaimed);
                transaction.set(ref, update);
            } else {
                update.put("progress." + type, FieldValue.increment(1));
                transaction.set(ref, update, SetOptions.merge());
            }
            return null;
        });
    }

    /** Marks a mission as claimed (called after reward is granted). */
    public Task<Void> claimReward(String userId, String type) {
        Map<String, Object> update = new HashMap<>();
        update.put("claimed." + type, true);
        return getDocRef(userId).set(update, SetOptions.merge());
    }

    public static long getWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
