package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Tournament;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ClanProvider;
import com.optic.socialmediagamer.providers.TournamentProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TournamentDetailActivity extends AppCompatActivity {

    private String mTournamentId;
    private Tournament mTournament;

    private TextView mTextGame, mTextStatus, mTextFormat, mTextDate, mTextPlayers, mTextWinner;
    private TextView mTextBracketHeader;
    private LinearLayout mLayoutBracket, mLayoutParticipants;
    private Button mButtonJoin, mButtonStart, mButtonDeclareWinner;

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final TournamentProvider mTournamentProvider = new TournamentProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();
    private final ClanProvider mClanProvider = new ClanProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_detail);

        mTournamentId = getIntent().getStringExtra("tournamentId");

        setSupportActionBar(findViewById(R.id.toolbarTournamentDetail));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextGame          = findViewById(R.id.textViewDetailGame);
        mTextStatus        = findViewById(R.id.textViewDetailStatus);
        mTextFormat        = findViewById(R.id.textViewDetailFormat);
        mTextDate          = findViewById(R.id.textViewDetailDate);
        mTextPlayers       = findViewById(R.id.textViewDetailPlayers);
        mTextWinner        = findViewById(R.id.textViewDetailWinner);
        mTextBracketHeader = findViewById(R.id.textViewBracketHeader);
        mLayoutBracket     = findViewById(R.id.layoutBracket);
        mLayoutParticipants = findViewById(R.id.layoutParticipants);
        mButtonJoin        = findViewById(R.id.buttonJoinTournament);
        mButtonStart       = findViewById(R.id.buttonStartTournament);
        mButtonDeclareWinner = findViewById(R.id.buttonDeclareWinner);

        loadTournament();
    }

    private void loadTournament() {
        mTournamentProvider.getById(mTournamentId).addOnSuccessListener(doc -> {
            mTournament = doc.toObject(Tournament.class);
            if (mTournament == null) return;
            mTournament.setId(doc.getId());
            renderTournament();
        });
    }

    private void renderTournament() {
        String myId = mAuthProvider.getUid();
        String status = mTournament.getStatus();
        boolean isCreator = mTournament.getIdUser() != null && mTournament.getIdUser().equals(myId);
        boolean hasJoined = mTournament.getPlayers() != null && mTournament.getPlayers().contains(myId);
        int joined = mTournament.getPlayers() != null ? mTournament.getPlayers().size() : 0;

        if (getSupportActionBar() != null) getSupportActionBar().setTitle("⚔️ " + mTournament.getGame());
        mTextGame.setText(mTournament.getGame());
        mTextFormat.setText(mTournament.getFormat());
        mTextPlayers.setText("👥 " + joined + "/" + mTournament.getMaxPlayers() + " jugadores");

        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date(mTournament.getDateTimestamp()));
        mTextDate.setText("📅 " + dateStr);

        switch (status) {
            case "open":     mTextStatus.setText("ABIERTO");   mTextStatus.setTextColor(Color.parseColor("#00F0FF")); break;
            case "started":  mTextStatus.setText("EN JUEGO");  mTextStatus.setTextColor(Color.parseColor("#FF6B00")); break;
            case "finished": mTextStatus.setText("FINALIZADO"); mTextStatus.setTextColor(Color.GRAY); break;
        }

        // Winner banner
        if ("finished".equals(status) && mTournament.getWinnerUsername() != null) {
            mTextWinner.setText("🏆 Ganador: @" + mTournament.getWinnerUsername());
            mTextWinner.setVisibility(View.VISIBLE);
        }

        // Action buttons
        if ("open".equals(status) && !isCreator) {
            mButtonJoin.setVisibility(View.VISIBLE);
            if (hasJoined) {
                mButtonJoin.setText("SALIR DEL TORNEO");
                mButtonJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                mButtonJoin.setOnClickListener(v -> leaveTournament());
            } else if (joined < mTournament.getMaxPlayers()) {
                mButtonJoin.setText("⚔️ INSCRIBIRSE");
                mButtonJoin.setOnClickListener(v -> joinTournament());
            } else {
                mButtonJoin.setText("TORNEO LLENO");
                mButtonJoin.setEnabled(false);
                mButtonJoin.setAlpha(0.5f);
            }
        }

        if (isCreator && "open".equals(status) && joined >= 2) {
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonStart.setOnClickListener(v -> startTournament());
        }

        if (isCreator && "started".equals(status)) {
            mButtonDeclareWinner.setVisibility(View.VISIBLE);
            mButtonDeclareWinner.setOnClickListener(v -> showDeclareWinnerDialog());
        }

        // Bracket
        if (!"open".equals(status) && mTournament.getBracket() != null && !mTournament.getBracket().isEmpty()) {
            mTextBracketHeader.setVisibility(View.VISIBLE);
            renderBracket();
        }

        // Participants
        renderParticipants();
    }

    private void renderBracket() {
        mLayoutBracket.removeAllViews();
        List<String> bracket = mTournament.getBracket();
        for (int i = 0; i < bracket.size(); i++) {
            String[] pair = bracket.get(i).split("\\|");
            String uid1 = pair[0];
            String uid2 = pair.length > 1 ? pair[1] : "BYE";

            fetchPairNames(uid1, uid2, i + 1);
        }
    }

    private void fetchPairNames(String uid1, String uid2, int matchNum) {
        mUsersProvider.getUser(uid1).addOnSuccessListener(doc1 -> {
            String name1 = doc1.exists() && doc1.getString("username") != null
                    ? "@" + doc1.getString("username") : uid1;
            if ("BYE".equals(uid2)) {
                addMatchRow(matchNum, name1, "BYE (pase directo)");
                return;
            }
            mUsersProvider.getUser(uid2).addOnSuccessListener(doc2 -> {
                String name2 = doc2.exists() && doc2.getString("username") != null
                        ? "@" + doc2.getString("username") : uid2;
                addMatchRow(matchNum, name1, name2);
            });
        });
    }

    private void addMatchRow(int matchNum, String p1, String p2) {
        TextView tv = new TextView(this);
        tv.setText("  Match " + matchNum + ":  " + p1 + "  ⚔️  " + p2);
        tv.setTextColor(Color.parseColor("#E0E0E0"));
        tv.setTextSize(13f);
        tv.setPadding(0, 6, 0, 6);
        mLayoutBracket.addView(tv);
    }

    private void renderParticipants() {
        mLayoutParticipants.removeAllViews();
        List<String> players = mTournament.getPlayers();
        if (players == null || players.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Sin inscriptos aún");
            tv.setTextColor(Color.GRAY);
            tv.setTextSize(13f);
            mLayoutParticipants.addView(tv);
            return;
        }
        for (String uid : players) {
            mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
                String username = doc.exists() && doc.getString("username") != null
                        ? "@" + doc.getString("username") : uid;
                TextView tv = new TextView(this);
                tv.setText("  • " + username);
                tv.setTextColor(Color.parseColor("#E0E0E0"));
                tv.setTextSize(13f);
                tv.setPadding(0, 4, 0, 4);
                mLayoutParticipants.addView(tv);
            });
        }
    }

    private void joinTournament() {
        mTournamentProvider.join(mTournamentId, mAuthProvider.getUid())
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "¡Inscrito en el torneo! ⚔️", Toast.LENGTH_SHORT).show();
                    loadTournament();
                });
    }

    private void leaveTournament() {
        mTournamentProvider.leave(mTournamentId, mAuthProvider.getUid())
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "Saliste del torneo", Toast.LENGTH_SHORT).show();
                    loadTournament();
                });
    }

    private void startTournament() {
        mTournamentProvider.start(mTournamentId, mTournament.getPlayers())
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "¡Torneo iniciado! ⚔️", Toast.LENGTH_SHORT).show();
                    loadTournament();
                });
    }

    private void showDeclareWinnerDialog() {
        List<String> players = mTournament.getPlayers();
        if (players == null || players.isEmpty()) return;

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : players) tasks.add(mUsersProvider.getUser(uid));

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            String[] usernames = new String[results.size()];
            String[] uids = players.toArray(new String[0]);
            for (int i = 0; i < results.size(); i++) {
                DocumentSnapshot doc = (DocumentSnapshot) results.get(i);
                usernames[i] = doc.exists() && doc.getString("username") != null
                        ? "@" + doc.getString("username") : uids[i];
            }

            new AlertDialog.Builder(this)
                    .setTitle("🏆 ¿Quién ganó el torneo?")
                    .setItems(usernames, (d, which) -> {
                        String winnerUid = uids[which];
                        String winnerName = usernames[which].replace("@", "");
                        mTournamentProvider.setWinner(mTournamentId, winnerUid, winnerName)
                                .addOnSuccessListener(u -> {
                                    Toast.makeText(this, "🏆 ¡" + usernames[which] + " ganó el torneo!", Toast.LENGTH_LONG).show();
                                    mClanProvider.awardClanXPForUser(winnerUid, 50);
                                    loadTournament();
                                });
                    }).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
