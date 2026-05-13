package com.optic.socialmediagamer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Challenge;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ChallengesProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.ArrayList;
import java.util.List;

public class MyChallengesActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ChallengesAdapter mAdapter;
    private TextView mTextEmpty;
    private final List<Challenge> mChallenges = new ArrayList<>();

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final ChallengesProvider mChallengesProvider = new ChallengesProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();

    private int mCurrentTab = 0; // 0=recibidos 1=enviados

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_challenges);

        setSupportActionBar(findViewById(R.id.toolbarChallenges));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.recyclerViewChallenges);
        mTextEmpty    = findViewById(R.id.textViewChallengesEmpty);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ChallengesAdapter(mChallenges);
        mRecyclerView.setAdapter(mAdapter);

        TabLayout tabs = findViewById(R.id.tabLayoutChallenges);
        tabs.addTab(tabs.newTab().setText("📥 Recibidos"));
        tabs.addTab(tabs.newTab().setText("📤 Enviados"));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                mCurrentTab = tab.getPosition();
                loadChallenges();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadChallenges();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChallenges();
    }

    private void loadChallenges() {
        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        (mCurrentTab == 0 ? mChallengesProvider.getReceived(myId) : mChallengesProvider.getSent(myId))
                .get().addOnSuccessListener(snap -> {
                    mChallenges.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Challenge c = doc.toObject(Challenge.class);
                        if (c != null) { c.setId(doc.getId()); mChallenges.add(c); }
                    }
                    mChallenges.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    mAdapter.notifyDataSetChanged();
                    boolean empty = mChallenges.isEmpty();
                    mTextEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    mRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    // ---- Inner adapter ----

    class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.VH> {
        private final List<Challenge> items;
        ChallengesAdapter(List<Challenge> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Challenge c = items.get(position);
            String myId = mAuthProvider.getUid();
            boolean isChallenger = myId != null && myId.equals(c.getIdChallenger());
            String opponentId = isChallenger ? c.getIdChallenged() : c.getIdChallenger();

            mUsersProvider.getUser(opponentId).addOnSuccessListener(doc -> {
                String username = doc.getString("username") != null ? doc.getString("username") : "gamer";
                h.textOpponent.setText("⚔️ vs @" + username);
            });

            h.textDesc.setText(c.getDescription());
            h.textStatus.setText(statusLabel(c.getStatus()));

            int vc = c.getVotesChallenger() != null ? c.getVotesChallenger().size() : 0;
            int vd = c.getVotesChallenged() != null ? c.getVotesChallenged().size() : 0;
            if ("voting".equals(c.getStatus())) {
                h.textVotes.setText(vc + " vs " + vd + " votos");
            }

            if ("finished".equals(c.getStatus()) && c.getWinnerUsername() != null) {
                h.textWinner.setVisibility(View.VISIBLE);
                h.textWinner.setText("🏆 @" + c.getWinnerUsername());
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MyChallengesActivity.this, ChallengeDetailActivity.class);
                intent.putExtra("challengeId", c.getId());
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView textOpponent, textDesc, textStatus, textVotes, textWinner;
            VH(View v) {
                super(v);
                textOpponent = v.findViewById(R.id.textViewChallengeOpponent);
                textDesc     = v.findViewById(R.id.textViewChallengeDesc);
                textStatus   = v.findViewById(R.id.textViewChallengeStatus);
                textVotes    = v.findViewById(R.id.textViewChallengeVotes);
                textWinner   = v.findViewById(R.id.textViewChallengeWinner);
            }
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "PENDIENTE";
        switch (s) {
            case "accepted": return "ACEPTADO";
            case "rejected": return "RECHAZADO";
            case "voting":   return "VOTANDO";
            case "finished": return "FINALIZADO";
            default:         return "PENDIENTE";
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
