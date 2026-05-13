package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Badge;
import com.optic.socialmediagamer.models.Season;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.BadgeProvider;
import com.optic.socialmediagamer.providers.SeasonProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SeasonActivity extends AppCompatActivity {

    // Replace with your Firebase UID to control who can close seasons
    private static final String ADMIN_UID = "REPLACE_WITH_ADMIN_UID";

    TextView mTextSeasonLabel, mTextSeasonSubtitle;
    Button mButtonCloseSeason;
    LinearLayout mLayoutLeaderboard, mLayoutHallOfFame;

    SeasonProvider mSeasonProvider;
    BadgeProvider mBadgeProvider;
    XPProvider mXPProvider;
    AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_season);

        Toolbar toolbar = findViewById(R.id.toolbarSeason);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mTextSeasonLabel    = findViewById(R.id.textViewSeasonLabel);
        mTextSeasonSubtitle = findViewById(R.id.textViewSeasonSubtitle);
        mButtonCloseSeason  = findViewById(R.id.buttonCloseSeason);
        mLayoutLeaderboard  = findViewById(R.id.layoutSeasonLeaderboard);
        mLayoutHallOfFame   = findViewById(R.id.layoutHallOfFame);

        mSeasonProvider = new SeasonProvider();
        mBadgeProvider  = new BadgeProvider();
        mXPProvider     = new XPProvider();
        mAuthProvider   = new AuthProvider();

        String currentMonth = new SimpleDateFormat("MMMM yyyy", new Locale("es")).format(new Date());
        mTextSeasonLabel.setText("🏅 Temporada " + capitalize(currentMonth));
        mTextSeasonSubtitle.setText("El jugador con más XP este mes gana el badge de Campeón de Temporada.");

        boolean isAdmin = ADMIN_UID.equals(mAuthProvider.getUid());
        mButtonCloseSeason.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        mButtonCloseSeason.setOnClickListener(v -> confirmCloseSeason());

        loadLeaderboard();
        loadHallOfFame();
    }

    private void loadLeaderboard() {
        mLayoutLeaderboard.removeAllViews();
        mSeasonProvider.getSeasonLeaderboard().get().addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            if (docs.isEmpty()) {
                addEmptyRow(mLayoutLeaderboard, "Aún nadie ha ganado XP este mes.");
                return;
            }
            for (int i = 0; i < docs.size(); i++) {
                DocumentSnapshot doc = docs.get(i);
                String username  = doc.getString("username") != null ? doc.getString("username") : "—";
                long xp          = doc.getLong("xp") != null ? doc.getLong("xp") : 0L;
                long seasonXp    = doc.getLong("seasonXp") != null ? doc.getLong("seasonXp") : 0L;
                addLeaderboardRow(i + 1, username, xp, seasonXp);
            }
        });
    }

    private void addLeaderboardRow(int pos, String username, long xp, long seasonXp) {
        String medal = pos == 1 ? "🥇" : pos == 2 ? "🥈" : pos == 3 ? "🥉" : pos + ".";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);

        TextView tvPos = new TextView(this);
        tvPos.setText(medal + "  ");
        tvPos.setTextSize(18);
        tvPos.setMinWidth(64);
        row.addView(tvPos);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText("@" + username + "  " + RankHelper.getRankEmoji(xp));
        tvName.setTextColor(getColor(R.color.color_text_primary));
        tvName.setTextSize(14);
        tvName.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        info.addView(tvName);

        TextView tvRank = new TextView(this);
        tvRank.setText(RankHelper.getRankName(xp) + "  ·  " + xp + " XP total");
        tvRank.setTextColor(getColor(R.color.color_text_secondary));
        tvRank.setTextSize(11);
        info.addView(tvRank);

        row.addView(info);

        TextView tvSeasonXp = new TextView(this);
        tvSeasonXp.setText(seasonXp + " XP");
        tvSeasonXp.setTextColor(getColor(R.color.color_primary));
        tvSeasonXp.setTextSize(14);
        tvSeasonXp.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(tvSeasonXp);

        mLayoutLeaderboard.addView(row);
        addDivider(mLayoutLeaderboard);
    }

    private void loadHallOfFame() {
        mLayoutHallOfFame.removeAllViews();
        mSeasonProvider.getPastSeasons().addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            if (docs.isEmpty()) {
                addEmptyRow(mLayoutHallOfFame, "Aún no hay temporadas cerradas.");
                return;
            }
            for (DocumentSnapshot doc : docs) {
                Season season = doc.toObject(Season.class);
                if (season == null) continue;
                addHallRow(season);
            }
        });
    }

    private void addHallRow(Season season) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvLabel.setText("🏅 " + season.getLabel());
        tvLabel.setTextColor(getColor(R.color.color_text_primary));
        tvLabel.setTextSize(13);
        tvLabel.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(tvLabel);

        TextView tvChamp = new TextView(this);
        tvChamp.setText("@" + season.getChampionUsername() + " · " + season.getSeasonXp() + " XP");
        tvChamp.setTextColor(getColor(R.color.color_primary));
        tvChamp.setTextSize(12);
        tvChamp.setTypeface(android.graphics.Typeface.MONOSPACE);
        row.addView(tvChamp);

        mLayoutHallOfFame.addView(row);
        addDivider(mLayoutHallOfFame);
    }

    private void confirmCloseSeason() {
        mSeasonProvider.getSeasonLeaderboard().get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                Toast.makeText(this, "No hay jugadores esta temporada", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentSnapshot top = snap.getDocuments().get(0);
            String champId   = top.getId();
            String champName = top.getString("username") != null ? top.getString("username") : champId;
            long champXp     = top.getLong("seasonXp") != null ? top.getLong("seasonXp") : 0L;

            new AlertDialog.Builder(this)
                    .setTitle("🏅 Cerrar temporada")
                    .setMessage("¿Declarar a @" + champName + " (" + champXp + " XP) como Campeón de Temporada?")
                    .setPositiveButton("CERRAR TEMPORADA", (d, w) -> {
                        mSeasonProvider.closeSeason(champId, champName, champXp)
                                .addOnSuccessListener(u -> {
                                    mBadgeProvider.awardIfMissing(champId, Badge.CAMPEON_TEMPORADA);
                                    mXPProvider.addXP(champId, 200);
                                    Toast.makeText(this, "🏅 ¡@" + champName + " es Campeón de Temporada! +200 XP", Toast.LENGTH_LONG).show();
                                    loadLeaderboard();
                                    loadHallOfFame();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void addEmptyRow(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(R.color.color_text_secondary));
        tv.setPadding(16, 20, 16, 20);
        parent.addView(tv);
    }

    private void addDivider(LinearLayout parent) {
        View div = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        div.setLayoutParams(lp);
        div.setBackgroundColor(getColor(R.color.color_divider));
        parent.addView(div);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
