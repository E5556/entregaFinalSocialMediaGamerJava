package com.optic.socialmediagamer.utils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PresenceManager {

    private static final String PATH = "presence";

    private final DatabaseReference mRef;
    private final String mUserId;

    public PresenceManager(String userId) {
        this.mUserId = userId;
        this.mRef    = FirebaseDatabase.getInstance().getReference(PATH).child(userId);
    }

    public void connect() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                boolean connected = Boolean.TRUE.equals(snap.getValue(Boolean.class));
                if (connected) {
                    Map<String, Object> online = new HashMap<>();
                    online.put("online", true);
                    online.put("lastSeen", ServerValue.TIMESTAMP);
                    mRef.setValue(online);
                    mRef.onDisconnect().setValue(buildOffline());
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    public void disconnect() {
        mRef.setValue(buildOffline());
    }

    private Map<String, Object> buildOffline() {
        Map<String, Object> m = new HashMap<>();
        m.put("online", false);
        m.put("lastSeen", ServerValue.TIMESTAMP);
        return m;
    }

    /** Listens to another user's online status. Callback receives true/false. */
    public static DatabaseReference listenStatus(String userId, OnlineCallback cb) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(PATH).child(userId).child("online");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                Boolean val = snap.getValue(Boolean.class);
                cb.onStatus(Boolean.TRUE.equals(val));
            }
            @Override
            public void onCancelled(DatabaseError error) { cb.onStatus(false); }
        });
        return ref;
    }

    public interface OnlineCallback {
        void onStatus(boolean isOnline);
    }
}
