package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.optic.socialmediagamer.models.Notification;

import java.util.List;

public class NotificationsProvider {

    CollectionReference mCollection;

    public NotificationsProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("notifications");
    }

    public Task<Void> save(Notification notification) {
        String id = mCollection.document().getId();
        notification.setId(id);
        return mCollection.document(id).set(notification);
    }

    public Query getNotificationsByUser(String idTo) {
        // Sin orderBy para evitar índice compuesto. Se ordena en el cliente.
        return mCollection.whereEqualTo("idTo", idTo);
    }

    public Task<Void> markAsRead(String notificationId) {
        return mCollection.document(notificationId).update("read", true);
    }

    public void markMultipleAsRead(List<String> ids) {
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (String id : ids) {
            batch.update(mCollection.document(id), "read", true);
        }
        batch.commit();
    }

    public Query getUnreadCount(String idTo) {
        // Single-field query to avoid composite index; filter read=false client-side in HomeActivity
        return mCollection.whereEqualTo("idTo", idTo);
    }

    public void markAllAsRead(String idTo) {
        mCollection.whereEqualTo("idTo", idTo).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean read = doc.getBoolean("read");
                        if (read == null || !read) {
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }
}
