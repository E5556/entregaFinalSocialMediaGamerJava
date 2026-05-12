package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredictionsProvider {

    private final FirebaseFirestore mFirestore;

    public PredictionsProvider() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    private DocumentReference ref(String eventId) {
        return mFirestore.collection("event_predictions").document(eventId);
    }

    public Task<DocumentSnapshot> get(String eventId) {
        return ref(eventId).get();
    }

    /** Creator sets the prediction question and options (up to 4). */
    public Task<Void> setup(String eventId, String question, List<String> options) {
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("options", options);
        data.put("correctOption", -1);
        data.put("resolved", false);
        return ref(eventId).set(data, SetOptions.merge());
    }

    /** User votes for an option index. votes map: {uid → optionIndex}. */
    public Task<Void> vote(String eventId, String userId, int optionIndex) {
        Map<String, Object> update = new HashMap<>();
        update.put("votes." + userId, optionIndex);
        return ref(eventId).set(update, SetOptions.merge());
    }

    /** Creator marks the correct option; returns list of winning UIDs to award XP. */
    public Task<Void> resolve(String eventId, int correctOption) {
        Map<String, Object> update = new HashMap<>();
        update.put("correctOption", correctOption);
        update.put("resolved", true);
        return ref(eventId).update(update);
    }
}
