package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.models.Reaction;

import java.util.Date;

public class ReactionsProvider {

    private final CollectionReference mCollection;

    public ReactionsProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("reactions");
    }

    public Task<Void> react(String idUser, String idPost, String type) {
        String id = idUser + "_" + idPost + "_" + type;
        Reaction reaction = new Reaction();
        reaction.setIdUser(idUser);
        reaction.setIdPost(idPost);
        reaction.setType(type);
        reaction.setTimestamp(new Date().getTime());
        return mCollection.document(id).set(reaction);
    }

    public Task<Void> unreact(String idUser, String idPost, String type) {
        String id = idUser + "_" + idPost + "_" + type;
        return mCollection.document(id).delete();
    }

    public DocumentReference getReaction(String idUser, String idPost, String type) {
        String id = idUser + "_" + idPost + "_" + type;
        return mCollection.document(id);
    }

    // Solo filtra por idPost para evitar índice compuesto en Firestore
    public Query getReactionsByPost(String idPost) {
        return mCollection.whereEqualTo("idPost", idPost);
    }

    public ListenerRegistration listenReactionsByPost(String idPost, EventListener<QuerySnapshot> listener) {
        return mCollection.whereEqualTo("idPost", idPost).addSnapshotListener(listener);
    }
}
