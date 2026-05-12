package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.XPProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyMissionsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private MissionsAdapter mAdapter;
    private final List<MissionItem> mMissions = new ArrayList<>();

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final MissionsProvider mMissionsProvider = new MissionsProvider();
    private final XPProvider mXPProvider = new XPProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_missions);

        setSupportActionBar(findViewById(R.id.toolbarMissions));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView textWeek = findViewById(R.id.textViewMissionWeek);
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(MissionsProvider.getWeekStart());
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        textWeek.setText(sdf.format(new Date(start.getTimeInMillis())) + " – " + sdf.format(new Date(end.getTimeInMillis())));

        mRecyclerView = findViewById(R.id.recyclerViewMissions);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MissionsAdapter(mMissions);
        mRecyclerView.setAdapter(mAdapter);

        buildMissionList();
        loadProgress();
    }

    private void buildMissionList() {
        mMissions.clear();
        mMissions.add(new MissionItem(MissionsProvider.TYPE_POST,    "📝", "Publicar 3 posts",         "Crea 3 publicaciones esta semana",         3,  30));
        mMissions.add(new MissionItem(MissionsProvider.TYPE_LIKE,    "❤️", "Dar 10 likes",             "Dale like a 10 publicaciones",             10, 20));
        mMissions.add(new MissionItem(MissionsProvider.TYPE_COMMENT, "💬", "Comentar 5 veces",         "Escribe 5 comentarios en publicaciones",    5,  25));
        mMissions.add(new MissionItem(MissionsProvider.TYPE_FOLLOW,  "👥", "Seguir a 2 gamers",        "Sigue a 2 nuevos jugadores esta semana",   2,  15));
        mMissions.add(new MissionItem(MissionsProvider.TYPE_LFG,     "🎮", "Unirse a 1 party LFG",    "Únete a una party de LFG",                 1,  20));
    }

    private void loadProgress() {
        String uid = mAuthProvider.getUid();
        if (uid == null) return;

        mMissionsProvider.get(uid).addOnSuccessListener(doc -> {
            Map<String, Long> progress = null;
            Map<String, Boolean> claimed = null;

            if (doc.exists()) {
                long savedWeek = doc.getLong("weekStart") != null ? doc.getLong("weekStart") : 0L;
                if (savedWeek >= MissionsProvider.getWeekStart()) {
                    Object pObj = doc.get("progress");
                    Object cObj = doc.get("claimed");
                    if (pObj instanceof Map) //noinspection unchecked
                        progress = (Map<String, Long>) pObj;
                    if (cObj instanceof Map) //noinspection unchecked
                        claimed = (Map<String, Boolean>) cObj;
                }
            }

            for (MissionItem m : mMissions) {
                m.progress = progress != null && progress.containsKey(m.type)
                        ? progress.get(m.type).intValue() : 0;
                m.claimed = claimed != null && Boolean.TRUE.equals(claimed.get(m.type));
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // ---- Inner model ----

    static class MissionItem {
        final String type, emoji, title, desc;
        final int target, xpReward;
        int progress;
        boolean claimed;

        MissionItem(String type, String emoji, String title, String desc, int target, int xpReward) {
            this.type = type; this.emoji = emoji; this.title = title;
            this.desc = desc; this.target = target; this.xpReward = xpReward;
        }
    }

    // ---- Inner adapter ----

    class MissionsAdapter extends RecyclerView.Adapter<MissionsAdapter.VH> {
        private final List<MissionItem> items;
        MissionsAdapter(List<MissionItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_mission, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MissionItem m = items.get(position);
            h.textEmoji.setText(m.emoji);
            h.textTitle.setText(m.title);
            h.textDesc.setText(m.desc);
            h.textReward.setText("+" + m.xpReward + " XP");

            int capped = Math.min(m.progress, m.target);
            h.progressBar.setMax(m.target);
            h.progressBar.setProgress(capped);
            h.textProgress.setText(capped + "/" + m.target);

            boolean completed = m.progress >= m.target;

            if (m.claimed) {
                h.buttonClaim.setVisibility(View.VISIBLE);
                h.buttonClaim.setText("✅ RECLAMADO");
                h.buttonClaim.setEnabled(false);
                h.buttonClaim.setAlpha(0.5f);
            } else if (completed) {
                h.buttonClaim.setVisibility(View.VISIBLE);
                h.buttonClaim.setText("RECLAMAR +" + m.xpReward + " XP");
                h.buttonClaim.setEnabled(true);
                h.buttonClaim.setAlpha(1f);
                h.buttonClaim.setOnClickListener(v -> {
                    String uid = mAuthProvider.getUid();
                    if (uid == null) return;
                    mXPProvider.addXP(uid, m.xpReward).addOnSuccessListener(u ->
                        mMissionsProvider.claimReward(uid, m.type).addOnSuccessListener(u2 -> {
                            m.claimed = true;
                            notifyItemChanged(h.getAdapterPosition());
                            Toast.makeText(WeeklyMissionsActivity.this,
                                    "¡+" + m.xpReward + " XP ganados! 🎉", Toast.LENGTH_SHORT).show();
                        })
                    );
                });
            } else {
                h.buttonClaim.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView textEmoji, textTitle, textDesc, textReward, textProgress;
            ProgressBar progressBar;
            Button buttonClaim;

            VH(View v) {
                super(v);
                textEmoji    = v.findViewById(R.id.textViewMissionEmoji);
                textTitle    = v.findViewById(R.id.textViewMissionTitle);
                textDesc     = v.findViewById(R.id.textViewMissionDesc);
                textReward   = v.findViewById(R.id.textViewMissionReward);
                progressBar  = v.findViewById(R.id.progressBarMission);
                textProgress = v.findViewById(R.id.textViewMissionProgress);
                buttonClaim  = v.findViewById(R.id.buttonMissionClaim);
            }
        }
    }
}
