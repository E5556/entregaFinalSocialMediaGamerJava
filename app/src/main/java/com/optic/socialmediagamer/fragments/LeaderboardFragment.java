package com.optic.socialmediagamer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private LeaderboardAdapter mAdapter;
    private final UsersProvider mUsersProvider = new UsersProvider();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        mRecyclerView = view.findViewById(R.id.recyclerViewLeaderboard);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView textWeekLabel = view.findViewById(R.id.textViewWeekLabel);
        textWeekLabel.setText(getCurrentWeekLabel());

        loadLeaderboard();
        return view;
    }

    private void loadLeaderboard() {
        mUsersProvider.getTopWeekly(10).get().addOnSuccessListener(snap -> {
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setId(doc.getId());
                    users.add(user);
                }
            }
            mAdapter = new LeaderboardAdapter(users, getContext());
            mRecyclerView.setAdapter(mAdapter);
        });
    }

    private String getCurrentWeekLabel() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        cal.add(Calendar.DAY_OF_MONTH, 6);
        int dayEnd = cal.get(Calendar.DAY_OF_MONTH);
        int monthEnd = cal.get(Calendar.MONTH) + 1;
        return String.format("Semana %02d/%02d — %02d/%02d", day, month, dayEnd, monthEnd);
    }

    // ── Adapter interno ──────────────────────────────────────────────────────

    static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

        private final List<User> mUsers;
        private final android.content.Context mContext;

        LeaderboardAdapter(List<User> users, android.content.Context context) {
            mUsers = users;
            mContext = context;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            User user = mUsers.get(position);
            int rank = position + 1;

            h.textPosition.setText(positionLabel(rank));
            h.textUsername.setText(user.getUsername() != null ? user.getUsername() : "—");
            h.textWeeklyXp.setText(user.getWeeklyXp() + " XP");
            h.textRankEmoji.setText(RankHelper.getRankEmoji(user.getXp()));

            if (user.getImageProfile() != null && !user.getImageProfile().isEmpty()) {
                com.squareup.picasso.Picasso.get()
                        .load(user.getImageProfile())
                        .placeholder(R.drawable.ic_person)
                        .into(h.imageProfile);
            }

            // Podium colors
            if (rank == 1) h.textPosition.setTextColor(android.graphics.Color.parseColor("#FFD700"));
            else if (rank == 2) h.textPosition.setTextColor(android.graphics.Color.parseColor("#C0C0C0"));
            else if (rank == 3) h.textPosition.setTextColor(android.graphics.Color.parseColor("#CD7F32"));
            else h.textPosition.setTextColor(android.graphics.Color.parseColor("#8A8A8A"));
        }

        private String positionLabel(int pos) {
            if (pos == 1) return "🥇";
            if (pos == 2) return "🥈";
            if (pos == 3) return "🥉";
            return "#" + pos;
        }

        @Override
        public int getItemCount() { return mUsers.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView textPosition, textUsername, textWeeklyXp, textRankEmoji;
            de.hdodenhof.circleimageview.CircleImageView imageProfile;

            VH(View v) {
                super(v);
                textPosition  = v.findViewById(R.id.textViewLeaderPosition);
                textUsername  = v.findViewById(R.id.textViewLeaderUsername);
                textWeeklyXp  = v.findViewById(R.id.textViewLeaderWeeklyXp);
                textRankEmoji = v.findViewById(R.id.textViewLeaderRankEmoji);
                imageProfile  = v.findViewById(R.id.imageViewLeaderProfile);
            }
        }
    }
}
