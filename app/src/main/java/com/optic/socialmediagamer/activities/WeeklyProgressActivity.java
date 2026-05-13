package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeeklyProgressActivity extends AppCompatActivity {

    TextView mTextWeekRange;
    TextView mTextXPValue, mTextXPNextRank;
    ProgressBar mProgressBarXP;
    TextView mTextStreakValue;
    TextView mTextPostsValue;
    TextView mTextLikesValue;
    TextView mTextMissionsValue;
    TextView mTextRankChange;
    LinearLayout mLayoutMissions;

    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    PostProvider mPostProvider;
    MissionsProvider mMissionsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_progress);

        Toolbar toolbar = findViewById(R.id.toolbarWeeklyProgress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mTextWeekRange    = findViewById(R.id.textViewWeekRange);
        mTextXPValue      = findViewById(R.id.textViewWeeklyXPValue);
        mTextXPNextRank   = findViewById(R.id.textViewWeeklyNextRank);
        mProgressBarXP    = findViewById(R.id.progressBarWeeklyXP);
        mTextStreakValue  = findViewById(R.id.textViewStreakValue);
        mTextPostsValue   = findViewById(R.id.textViewWeeklyPostsValue);
        mTextLikesValue   = findViewById(R.id.textViewWeeklyLikesValue);
        mTextMissionsValue = findViewById(R.id.textViewMissionsValue);
        mTextRankChange   = findViewById(R.id.textViewRankChange);
        mLayoutMissions   = findViewById(R.id.layoutMissionsProgress);

        mAuthProvider    = new AuthProvider();
        mUsersProvider   = new UsersProvider();
        mPostProvider    = new PostProvider();
        mMissionsProvider = new MissionsProvider();

        // Week range label (Mon – Sun)
        Calendar mon = Calendar.getInstance();
        mon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar sun = (Calendar) mon.clone();
        sun.add(Calendar.DAY_OF_YEAR, 6);
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", new Locale("es"));
        mTextWeekRange.setText(fmt.format(mon.getTime()) + " – " + fmt.format(sun.getTime()));

        loadData();
    }

    private void loadData() {
        String uid = mAuthProvider.getUid();
        long weekStart = MissionsProvider.getWeekStart();

        Tasks.whenAllSuccess(
                mUsersProvider.getUser(uid),
                mPostProvider.getPostByUser(uid).get(),
                mMissionsProvider.get(uid)
        ).addOnSuccessListener(results -> {
            DocumentSnapshot userDoc     = (DocumentSnapshot) results.get(0);
            com.google.firebase.firestore.QuerySnapshot postsSnap =
                    (com.google.firebase.firestore.QuerySnapshot) results.get(1);
            DocumentSnapshot missionsDoc = (DocumentSnapshot) results.get(2);

            // XP
            long totalXp   = userDoc.getLong("xp") != null ? userDoc.getLong("xp") : 0L;
            long weeklyXp  = userDoc.getLong("weeklyXp") != null ? userDoc.getLong("weeklyXp") : 0L;
            long streak    = userDoc.getLong("currentStreak") != null ? userDoc.getLong("currentStreak") : 0L;

            mTextXPValue.setText("+" + weeklyXp + " XP esta semana");
            mProgressBarXP.setProgress(RankHelper.getProgressPercent(totalXp));
            long next = RankHelper.getNextRankXP(totalXp);
            mTextXPNextRank.setText(next == -1 ? "¡Rango máximo!" : "Siguiente rango: " + next + " XP");
            mTextRankChange.setText(RankHelper.getRankEmoji(totalXp) + " " + RankHelper.getRankName(totalXp)
                    + "  ·  " + totalXp + " XP total");

            // Streak
            mTextStreakValue.setText(streak > 0 ? "🔥 " + streak + " días" : "Sin racha activa");

            // Posts this week (filter client-side by timestamp)
            long likesThisWeek = 0;
            int postsThisWeek = 0;
            List<DocumentSnapshot> postDocs = postsSnap.getDocuments();
            for (DocumentSnapshot p : postDocs) {
                Long ts = p.getLong("timestamp");
                if (ts != null && ts >= weekStart) {
                    postsThisWeek++;
                    Long likes = p.getLong("likeCount");
                    if (likes != null) likesThisWeek += likes;
                }
            }
            mTextPostsValue.setText(String.valueOf(postsThisWeek));
            mTextLikesValue.setText(String.valueOf(likesThisWeek));

            // Missions
            int claimed = 0;
            if (missionsDoc.exists()) {
                java.util.Map<String, Object> claimedMap =
                        (java.util.Map<String, Object>) missionsDoc.get("claimed");
                if (claimedMap != null) {
                    for (Object val : claimedMap.values()) {
                        if (Boolean.TRUE.equals(val)) claimed++;
                    }
                }
            }
            mTextMissionsValue.setText(claimed + " / 5 misiones completadas");
            renderMissionsProgress(missionsDoc);
        });
    }

    private void renderMissionsProgress(DocumentSnapshot missionsDoc) {
        String[][] missions = {
                {MissionsProvider.TYPE_POST,    "📝 Publicar 3 posts",   "3"},
                {MissionsProvider.TYPE_LIKE,    "❤️ Dar 10 likes",       "10"},
                {MissionsProvider.TYPE_COMMENT, "💬 Comentar 5 veces",   "5"},
                {MissionsProvider.TYPE_FOLLOW,  "👥 Seguir 2 gamers",    "2"},
                {MissionsProvider.TYPE_LFG,     "🎮 Unirse a 1 party",   "1"},
        };

        mLayoutMissions.removeAllViews();

        java.util.Map<String, Object> progress = null;
        java.util.Map<String, Object> claimed  = null;
        if (missionsDoc.exists()) {
            progress = (java.util.Map<String, Object>) missionsDoc.get("progress");
            claimed  = (java.util.Map<String, Object>) missionsDoc.get("claimed");
        }

        for (String[] m : missions) {
            String type  = m[0];
            String label = m[1];
            int goal     = Integer.parseInt(m[2]);

            long prog = 0;
            if (progress != null && progress.get(type) instanceof Long) {
                prog = (Long) progress.get(type);
            }
            boolean done = claimed != null && Boolean.TRUE.equals(claimed.get(type));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 8);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvLabel = new TextView(this);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tvLabel.setText(label);
            tvLabel.setTextColor(getColor(done ? R.color.color_primary : R.color.color_text_primary));
            tvLabel.setTextSize(13);
            header.addView(tvLabel);

            int capped = (int) Math.min(prog, goal);
            TextView tvCount = new TextView(this);
            tvCount.setText((done ? "✅ " : "") + capped + "/" + goal);
            tvCount.setTextColor(getColor(done ? R.color.color_primary : R.color.color_text_secondary));
            tvCount.setTextSize(12);
            header.addView(tvCount);
            row.addView(header);

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(goal);
            pb.setProgress(capped);
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 12);
            pbLp.setMargins(0, 4, 0, 0);
            pb.setLayoutParams(pbLp);
            row.addView(pb);

            mLayoutMissions.addView(row);
        }
    }
}
