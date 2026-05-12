package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.models.Event;

import java.util.Date;
import java.util.HashMap;

public class EventsProvider {

    private final CollectionReference mCollection;

    public EventsProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("events");
    }

    public Task<Void> save(Event event) {
        String id = mCollection.document().getId();
        event.setId(id);
        event.setTimestamp(new Date().getTime());
        return mCollection.document(id).set(event);
    }

    public Query getUpcoming() {
        long now = new Date().getTime();
        return mCollection.whereGreaterThanOrEqualTo("eventDate", now)
                .orderBy("eventDate", Query.Direction.ASCENDING);
    }

    public Query getAll() {
        return mCollection.orderBy("eventDate", Query.Direction.ASCENDING);
    }

    public Task<Void> delete(String eventId) {
        return mCollection.document(eventId).delete();
    }

    public Task<DocumentSnapshot> getEvent(String eventId) {
        return mCollection.document(eventId).get();
    }

    public Query searchByTitle(String text) {
        return mCollection.orderBy("title")
                .startAt(text)
                .endAt(text + "");
    }

    public Task<Void> rsvp(String eventId, String userId) {
        HashMap<String, Object> update = new HashMap<>();
        update.put("attendees." + userId, true);
        update.put("attendeeCount", FieldValue.increment(1));
        return mCollection.document(eventId).update(update);
    }

    public Task<Void> cancelRsvp(String eventId, String userId) {
        HashMap<String, Object> update = new HashMap<>();
        update.put("attendees." + userId, FieldValue.delete());
        update.put("attendeeCount", FieldValue.increment(-1));
        return mCollection.document(eventId).update(update);
    }
}
