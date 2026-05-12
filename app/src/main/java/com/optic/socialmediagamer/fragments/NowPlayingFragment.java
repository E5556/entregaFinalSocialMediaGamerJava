package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.ChatActivity;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.FollowProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NowPlayingFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private NowPlayingAdapter mAdapter;
    private TextView mTextEmpty, mTextCount;
    private final List<NowPlayingItem> mItems = new ArrayList<>();

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final FollowProvider mFollowProvider = new FollowProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mRecyclerView = view.findViewById(R.id.recyclerViewNowPlaying);
        mTextEmpty    = view.findViewById(R.id.textViewNowPlayingEmpty);
        mTextCount    = view.findViewById(R.id.textViewNowPlayingCount);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new NowPlayingAdapter(mItems);
        mRecyclerView.setAdapter(mAdapter);

        loadNowPlaying();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNowPlaying();
    }

    private void loadNowPlaying() {
        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        mFollowProvider.getFollowing(myId).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                showEmpty();
                return;
            }

            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                String followedId = doc.getString("idFollowed");
                if (followedId != null) tasks.add(mUsersProvider.getUser(followedId));
            }

            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                mItems.clear();
                for (Object obj : results) {
                    DocumentSnapshot userDoc = (DocumentSnapshot) obj;
                    if (!userDoc.exists()) continue;
                    String nowPlaying = userDoc.getString("nowPlaying");
                    if (nowPlaying == null || nowPlaying.isEmpty()) continue;

                    NowPlayingItem item = new NowPlayingItem();
                    item.uid       = userDoc.getId();
                    item.username  = userDoc.getString("username");
                    item.avatar    = userDoc.getString("image_profile");
                    item.nowPlaying = nowPlaying;
                    mItems.add(item);
                }

                mAdapter.notifyDataSetChanged();
                mTextCount.setText(mItems.size() + " jugando");

                if (mItems.isEmpty()) showEmpty();
                else {
                    mTextEmpty.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showEmpty() {
        mTextEmpty.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mTextCount.setText("0 jugando");
    }

    // ---- Inner model ----

    static class NowPlayingItem {
        String uid, username, avatar, nowPlaying;
    }

    // ---- Inner adapter ----

    class NowPlayingAdapter extends RecyclerView.Adapter<NowPlayingAdapter.VH> {
        private final List<NowPlayingItem> items;
        NowPlayingAdapter(List<NowPlayingItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_now_playing, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            NowPlayingItem item = items.get(position);
            h.textUsername.setText("@" + (item.username != null ? item.username : "gamer"));
            h.textGame.setText("jugando " + item.nowPlaying);

            if (item.avatar != null && !item.avatar.isEmpty()) {
                Picasso.get().load(item.avatar).into(h.circleImage);
            }

            h.imageChat.setOnClickListener(v -> {
                if (getActivity() == null) return;
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("idReceiver", item.uid);
                intent.putExtra("username", item.username != null ? item.username : "gamer");
                intent.putExtra("imageProfile", item.avatar != null ? item.avatar : "");
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView circleImage;
            TextView textUsername, textGame;
            ImageView imageChat;
            VH(View v) {
                super(v);
                circleImage   = v.findViewById(R.id.circleImageNowPlaying);
                textUsername  = v.findViewById(R.id.textViewNowPlayingUsername);
                textGame      = v.findViewById(R.id.textViewNowPlayingGame);
                imageChat     = v.findViewById(R.id.imageViewNowPlayingChat);
            }
        }
    }
}
