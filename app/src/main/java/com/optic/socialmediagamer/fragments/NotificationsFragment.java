package com.optic.socialmediagamer.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.adapters.NotificationsAdapter;
import com.optic.socialmediagamer.models.GroupedNotification;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    View mView;
    RecyclerView mRecyclerView;
    NotificationsAdapter mAdapter;
    NotificationsProvider mNotificationsProvider;
    AuthProvider mAuthProvider;

    // Raw notifications from Firestore
    final List<Notification> mRawNotifications = new ArrayList<>();
    // Grouped list shown in the RecyclerView
    final List<GroupedNotification> mGrouped = new ArrayList<>();

    ListenerRegistration mListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_notifications, container, false);
        mRecyclerView = mView.findViewById(R.id.recyclerViewNotifications);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mNotificationsProvider = new NotificationsProvider();
        mAuthProvider = new AuthProvider();

        mAdapter = new NotificationsAdapter(mGrouped, getContext());
        mRecyclerView.setAdapter(mAdapter);

        TextView markAllRead = mView.findViewById(R.id.textViewMarkAllRead);
        markAllRead.setOnClickListener(v -> {
            String uid = mAuthProvider.getUid();
            if (uid != null) {
                mNotificationsProvider.markAllAsRead(uid);
                for (GroupedNotification g : mGrouped) g.setRead(true);
                mAdapter.notifyDataSetChanged();
            }
        });

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mListener != null) { mListener.remove(); mListener = null; }
        mRawNotifications.clear();
        mGrouped.clear();
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        mListener = mNotificationsProvider.getNotificationsByUser(myId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { Log.e("NOTIF", e.getMessage(), e); return; }
                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        String docId = dc.getDocument().getId();
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Notification notif = dc.getDocument().toObject(Notification.class);
                            notif.setId(docId);
                            boolean exists = false;
                            for (Notification n : mRawNotifications) {
                                if (docId.equals(n.getId())) { exists = true; break; }
                            }
                            if (!exists) mRawNotifications.add(notif);
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            for (int i = 0; i < mRawNotifications.size(); i++) {
                                if (docId.equals(mRawNotifications.get(i).getId())) {
                                    Notification updated = dc.getDocument().toObject(Notification.class);
                                    updated.setId(docId);
                                    mRawNotifications.set(i, updated);
                                    break;
                                }
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            mRawNotifications.removeIf(n -> docId.equals(n.getId()));
                        }
                    }

                    rebuildGrouped();
                });
    }

    private void rebuildGrouped() {
        // Sort raw by timestamp desc
        Collections.sort(mRawNotifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // Group: key = type + "_" + idPost (or just type for follows)
        Map<String, GroupedNotification> groupMap = new LinkedHashMap<>();

        for (Notification n : mRawNotifications) {
            String key = groupKey(n);
            GroupedNotification group = groupMap.get(key);
            if (group == null) {
                group = new GroupedNotification();
                group.setType(n.getType());
                group.setIdPost(n.getIdPost());
                group.setIdFrom(n.getIdFrom());
                group.setBody(n.getBody());
                group.setTimestamp(n.getTimestamp());
                group.setRead(n.isRead());
                group.setCount(0);
                groupMap.put(key, group);
            }
            group.getIds().add(n.getId());
            group.setCount(group.getCount() + 1);
            // Group is unread if any member is unread
            if (!n.isRead()) group.setRead(false);
            // Keep latest timestamp
            if (n.getTimestamp() > group.getTimestamp()) {
                group.setTimestamp(n.getTimestamp());
            }
        }

        // Build body with count suffix for groups > 1
        for (GroupedNotification g : groupMap.values()) {
            if (g.getCount() > 1) {
                int extra = g.getCount() - 1;
                String suffix = extra == 1 ? " y 1 persona más" : " y " + extra + " personas más";
                // Strip original trailing period if any, append suffix
                String base = g.getBody();
                g.setBody(base + suffix);
            }
        }

        mGrouped.clear();
        mGrouped.addAll(groupMap.values());
        // Sort groups by latest timestamp desc
        Collections.sort(mGrouped, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    private String groupKey(Notification n) {
        String type = n.getType() != null ? n.getType() : "other";
        // "post" notifications are not grouped (each is a different follower's post)
        if ("post".equals(type)) return type + "_" + n.getId();
        // "follow" grouped together
        if ("follow".equals(type)) return "follow";
        // "like", "comment" grouped by post
        String idPost = n.getIdPost() != null ? n.getIdPost() : "";
        return type + "_" + idPost;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mListener != null) { mListener.remove(); mListener = null; }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mListener != null) { mListener.remove(); mListener = null; }
    }
}
