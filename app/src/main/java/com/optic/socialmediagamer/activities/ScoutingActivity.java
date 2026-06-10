package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScoutingActivity extends AppCompatActivity {

    public static final String EXTRA_CLAN_ID   = "clanId";
    public static final String EXTRA_CLAN_NAME = "clanName";

    Toolbar mToolbar;
    RecyclerView mRecyclerView;
    ProgressBar mProgressBar;
    TextView mTextViewEmpty;

    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    NotificationsProvider mNotificationsProvider;
    FirebaseFirestore mFirestore;

    String mClanId;
    String mClanName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scouting);

        mToolbar      = findViewById(R.id.toolbarScouting);
        mRecyclerView = findViewById(R.id.recyclerViewScouting);
        mProgressBar  = findViewById(R.id.progressBarScouting);
        mTextViewEmpty = findViewById(R.id.textViewScoutingEmpty);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());
        mToolbar.setTitle("🔍 Scouting de Jugadores");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuthProvider          = new AuthProvider();
        mUsersProvider         = new UsersProvider();
        mNotificationsProvider = new NotificationsProvider();
        mFirestore             = FirebaseFirestore.getInstance();

        mClanId   = getIntent().getStringExtra(EXTRA_CLAN_ID);
        mClanName = getIntent().getStringExtra(EXTRA_CLAN_NAME);

        loadCandidates();
    }

    private void loadCandidates() {
        // 1. Load all active clan members
        mFirestore.collection("clans")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(clansSnap -> {
                    Set<String> inClan = new HashSet<>();
                    for (DocumentSnapshot doc : clansSnap.getDocuments()) {
                        List<String> members = (List<String>) doc.get("members");
                        if (members != null) inClan.addAll(members);
                        String leader = doc.getString("idLeader");
                        if (leader != null) inClan.add(leader);
                    }

                    // 2. Load top 50 users by XP
                    mUsersProvider.getTopWeekly(50).get().addOnSuccessListener(usersSnap -> {
                        List<DocumentSnapshot> candidates = new ArrayList<>();
                        for (DocumentSnapshot u : usersSnap.getDocuments()) {
                            if (!inClan.contains(u.getId())) candidates.add(u);
                        }

                        if (candidates.isEmpty()) {
                            showEmpty();
                            return;
                        }

                        // 3. Load Gamer Scores for top 20 candidates in parallel
                        int limit = Math.min(candidates.size(), 20);
                        List<DocumentSnapshot> topCandidates = candidates.subList(0, limit);
                        List<Task<QuerySnapshot>> scoreTasks = new ArrayList<>();
                        for (DocumentSnapshot u : topCandidates) {
                            scoreTasks.add(mFirestore.collection("game_ratings")
                                    .whereEqualTo("idTo", u.getId()).get());
                        }

                        Tasks.whenAllSuccess(scoreTasks).addOnSuccessListener(results -> {
                            List<ScoutCandidate> scored = new ArrayList<>();
                            for (int i = 0; i < topCandidates.size(); i++) {
                                DocumentSnapshot u = topCandidates.get(i);
                                QuerySnapshot ratings = (QuerySnapshot) results.get(i);
                                double avgScore = computeAvgScore(ratings);
                                long xp = u.getLong("xp") != null ? u.getLong("xp") : 0L;
                                // Composite: 60% XP (normalized to 0-100 at 5000XP) + 40% gamer score
                                double xpNorm = Math.min(xp / 50.0, 100.0);
                                double composite = xpNorm * 0.6 + avgScore * 20 * 0.4;
                                scored.add(new ScoutCandidate(u, avgScore, composite));
                            }
                            Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
                            showResults(scored);
                        });
                    });
                })
                .addOnFailureListener(e -> showEmpty());
    }

    private double computeAvgScore(QuerySnapshot snap) {
        if (snap == null || snap.isEmpty()) return 0.0;
        double sum = 0;
        int count = 0;
        for (DocumentSnapshot d : snap.getDocuments()) {
            Long skill = d.getLong("skill");
            Long comm  = d.getLong("communication");
            Long att   = d.getLong("attitude");
            if (skill != null && comm != null && att != null) {
                sum += (skill + comm + att) / 3.0;
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private void showResults(List<ScoutCandidate> candidates) {
        mProgressBar.setVisibility(View.GONE);
        if (candidates.isEmpty()) { showEmpty(); return; }
        mRecyclerView.setVisibility(View.VISIBLE);
        mRecyclerView.setAdapter(new ScoutAdapter(candidates));
    }

    private void showEmpty() {
        mProgressBar.setVisibility(View.GONE);
        mTextViewEmpty.setVisibility(View.VISIBLE);
    }

    private void sendInvite(ScoutCandidate candidate) {
        String myId = mAuthProvider.getUid();
        String targetId = candidate.user.getId();
        String targetUsername = candidate.user.getString("username");
        String clanName = mClanName != null ? mClanName : "el clan";

        Notification notif = new Notification();
        notif.setType("clan_invite");
        notif.setIdFrom(myId);
        notif.setIdTo(targetId);
        notif.setBody("¡Fuiste scouted! Te invitan a unirte a " + clanName);
        notif.setRead(false);
        notif.setTimestamp(new Date().getTime());
        mNotificationsProvider.save(notif).addOnSuccessListener(v ->
            Toast.makeText(this,
                "Invitación enviada a @" + targetUsername, Toast.LENGTH_SHORT).show());
    }

    static class ScoutCandidate {
        DocumentSnapshot user;
        double gamerScore;
        double score;
        ScoutCandidate(DocumentSnapshot u, double gs, double s) { user = u; gamerScore = gs; score = s; }
    }

    private class ScoutAdapter extends RecyclerView.Adapter<ScoutAdapter.VH> {
        List<ScoutCandidate> list;
        ScoutAdapter(List<ScoutCandidate> l) { this.list = l; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scout_candidate, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ScoutCandidate c = list.get(pos);
            String username = c.user.getString("username");
            long xp = c.user.getLong("xp") != null ? c.user.getLong("xp") : 0L;

            h.textRank.setText(RankHelper.getRankEmoji(xp));
            h.textUsername.setText("@" + (username != null ? username : "?"));
            h.textXP.setText(xp + " XP · " + RankHelper.getRankName(xp));

            if (c.gamerScore > 0) {
                h.textScore.setText(String.format("⭐ %.1f", c.gamerScore));
            } else {
                h.textScore.setText("⭐ Sin valoraciones");
            }

            h.btnInvite.setOnClickListener(v -> sendInvite(c));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView textRank, textUsername, textXP, textScore;
            Button btnInvite;
            VH(@NonNull View v) {
                super(v);
                textRank     = v.findViewById(R.id.textViewScoutRank);
                textUsername = v.findViewById(R.id.textViewScoutUsername);
                textXP       = v.findViewById(R.id.textViewScoutXP);
                textScore    = v.findViewById(R.id.textViewScoutGamerScore);
                btnInvite    = v.findViewById(R.id.buttonInviteScout);
            }
        }
    }
}
