package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Challenge;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ChallengesProvider;
import com.optic.socialmediagamer.providers.ClanProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.XPProvider;

import java.util.List;

public class ChallengeDetailActivity extends AppCompatActivity {

    private String mChallengeId;
    private Challenge mChallenge;

    private TextView mTextPlayers, mTextStatus, mTextDescription, mTextWinner;
    private TextView mTextEvidenceChallenger, mTextEvidenceChallenged;
    private EditText mEditEvidence;
    private LinearLayout mLayoutAcceptReject;
    private CardView mCardEvidence, mCardVoting;
    private Button mBtnAccept, mBtnReject, mBtnSubmitEvidence;
    private Button mBtnVoteChallenger, mBtnVoteChallenged, mBtnFinish;

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final ChallengesProvider mChallengesProvider = new ChallengesProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();
    private final XPProvider mXPProvider = new XPProvider();
    private final ClanProvider mClanProvider = new ClanProvider();

    private String mMyId;
    private String mChallengerUsername = "?";
    private String mChallengedUsername = "?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);

        mChallengeId = getIntent().getStringExtra("challengeId");
        mMyId = mAuthProvider.getUid();

        setSupportActionBar(findViewById(R.id.toolbarChallengeDetail));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextPlayers          = findViewById(R.id.textViewDetailPlayers);
        mTextStatus           = findViewById(R.id.textViewDetailStatus);
        mTextDescription      = findViewById(R.id.textViewDetailDescription);
        mTextWinner           = findViewById(R.id.textViewDetailWinner);
        mTextEvidenceChallenger = findViewById(R.id.textViewEvidenceChallenger);
        mTextEvidenceChallenged = findViewById(R.id.textViewEvidenceChallenged);
        mEditEvidence         = findViewById(R.id.editTextEvidence);
        mLayoutAcceptReject   = findViewById(R.id.layoutAcceptReject);
        mCardEvidence         = findViewById(R.id.cardEvidence);
        mCardVoting           = findViewById(R.id.cardVoting);
        mBtnAccept            = findViewById(R.id.buttonAccept);
        mBtnReject            = findViewById(R.id.buttonReject);
        mBtnSubmitEvidence    = findViewById(R.id.buttonSubmitEvidence);
        mBtnVoteChallenger    = findViewById(R.id.buttonVoteChallenger);
        mBtnVoteChallenged    = findViewById(R.id.buttonVoteChallenged);
        mBtnFinish            = findViewById(R.id.buttonFinish);

        loadChallenge();
    }

    private void loadChallenge() {
        mChallengesProvider.get(mChallengeId).addOnSuccessListener(doc -> {
            mChallenge = doc.toObject(Challenge.class);
            if (mChallenge == null) return;
            mChallenge.setId(doc.getId());
            loadUsernames();
        });
    }

    private void loadUsernames() {
        mUsersProvider.getUser(mChallenge.getIdChallenger()).addOnSuccessListener(d -> {
            mChallengerUsername = d.getString("username") != null ? d.getString("username") : "?";
            mUsersProvider.getUser(mChallenge.getIdChallenged()).addOnSuccessListener(d2 -> {
                mChallengedUsername = d2.getString("username") != null ? d2.getString("username") : "?";
                renderChallenge();
            });
        });
    }

    private void renderChallenge() {
        String status = mChallenge.getStatus();
        boolean isChallenger = mMyId != null && mMyId.equals(mChallenge.getIdChallenger());
        boolean isChallenged = mMyId != null && mMyId.equals(mChallenge.getIdChallenged());

        mTextPlayers.setText("@" + mChallengerUsername + "  ⚔️  @" + mChallengedUsername);
        mTextDescription.setText(mChallenge.getDescription());
        mTextStatus.setText(statusLabel(status));

        if ("finished".equals(status) && mChallenge.getWinnerUsername() != null) {
            mTextWinner.setVisibility(View.VISIBLE);
            mTextWinner.setText("🏆 Ganador: @" + mChallenge.getWinnerUsername());
        }

        // Accept/Reject (challenged, pending)
        if ("pending".equals(status) && isChallenged) {
            mLayoutAcceptReject.setVisibility(View.VISIBLE);
            mBtnAccept.setOnClickListener(v -> mChallengesProvider.accept(mChallengeId)
                    .addOnSuccessListener(u -> { Toast.makeText(this, "¡Desafío aceptado! ⚔️", Toast.LENGTH_SHORT).show(); loadChallenge(); }));
            mBtnReject.setOnClickListener(v -> mChallengesProvider.reject(mChallengeId)
                    .addOnSuccessListener(u -> { Toast.makeText(this, "Desafío rechazado", Toast.LENGTH_SHORT).show(); finish(); }));
        }

        // Evidence section (accepted)
        if ("accepted".equals(status) || "voting".equals(status) || "finished".equals(status)) {
            mCardEvidence.setVisibility(View.VISIBLE);

            String evC = mChallenge.getEvidenceChallenger();
            String evD = mChallenge.getEvidenceChallenged();

            if (evC != null && !evC.isEmpty()) {
                mTextEvidenceChallenger.setVisibility(View.VISIBLE);
                mTextEvidenceChallenger.setText("@" + mChallengerUsername + ": " + evC);
            }
            if (evD != null && !evD.isEmpty()) {
                mTextEvidenceChallenged.setVisibility(View.VISIBLE);
                mTextEvidenceChallenged.setText("@" + mChallengedUsername + ": " + evD);
            }

            // Show evidence input if this user hasn't submitted yet
            boolean needsEvidence = (isChallenger && (evC == null || evC.isEmpty()))
                    || (isChallenged && (evD == null || evD.isEmpty()));
            if (needsEvidence && "accepted".equals(status)) {
                mEditEvidence.setVisibility(View.VISIBLE);
                mBtnSubmitEvidence.setVisibility(View.VISIBLE);
                mBtnSubmitEvidence.setOnClickListener(v -> submitEvidence(isChallenger, evC, evD));
            }
        }

        // Voting section
        if ("voting".equals(status) || "finished".equals(status)) {
            mCardVoting.setVisibility(View.VISIBLE);
            int vc = mChallenge.getVotesChallenger() != null ? mChallenge.getVotesChallenger().size() : 0;
            int vd = mChallenge.getVotesChallenged() != null ? mChallenge.getVotesChallenged().size() : 0;

            mBtnVoteChallenger.setText("@" + mChallengerUsername + "\n" + vc + " votos");
            mBtnVoteChallenged.setText("@" + mChallengedUsername + "\n" + vd + " votos");

            boolean canVote = !isChallenger && !isChallenged && "voting".equals(status);
            if (canVote) {
                mBtnVoteChallenger.setOnClickListener(v -> mChallengesProvider.voteChallenger(mChallengeId, mMyId)
                        .addOnSuccessListener(u -> { Toast.makeText(this, "Votaste por @" + mChallengerUsername, Toast.LENGTH_SHORT).show(); loadChallenge(); }));
                mBtnVoteChallenged.setOnClickListener(v -> mChallengesProvider.voteChallenged(mChallengeId, mMyId)
                        .addOnSuccessListener(u -> { Toast.makeText(this, "Votaste por @" + mChallengedUsername, Toast.LENGTH_SHORT).show(); loadChallenge(); }));
            } else {
                mBtnVoteChallenger.setEnabled(false);
                mBtnVoteChallenged.setEnabled(false);
            }

            if (isChallenger && "voting".equals(status)) {
                mBtnFinish.setVisibility(View.VISIBLE);
                mBtnFinish.setOnClickListener(v -> declareWinner(vc, vd));
            }
        }
    }

    private void submitEvidence(boolean isChallenger, String evC, String evD) {
        String text = mEditEvidence.getText().toString().trim();
        if (text.isEmpty()) { Toast.makeText(this, "Escribe tu evidencia", Toast.LENGTH_SHORT).show(); return; }

        if (isChallenger) {
            mChallengesProvider.submitEvidenceChallenger(mChallengeId, text).addOnSuccessListener(u -> checkOpenVoting(text, evD));
        } else {
            mChallengesProvider.submitEvidenceChallenged(mChallengeId, text).addOnSuccessListener(u -> checkOpenVoting(evC, text));
        }
    }

    private void checkOpenVoting(String evC, String evD) {
        boolean bothReady = evC != null && !evC.isEmpty() && evD != null && !evD.isEmpty();
        if (bothReady) {
            mChallengesProvider.openVoting(mChallengeId).addOnSuccessListener(u -> {
                Toast.makeText(this, "¡Evidencias listas! Votación abierta 🗳️", Toast.LENGTH_SHORT).show();
                loadChallenge();
            });
        } else {
            Toast.makeText(this, "Evidencia enviada. Esperando al otro jugador.", Toast.LENGTH_SHORT).show();
            loadChallenge();
        }
    }

    private void declareWinner(int vc, int vd) {
        String winnerUid, winnerUsername;
        if (vc >= vd) {
            winnerUid = mChallenge.getIdChallenger();
            winnerUsername = mChallengerUsername;
        } else {
            winnerUid = mChallenge.getIdChallenged();
            winnerUsername = mChallengedUsername;
        }
        mChallengesProvider.finish(mChallengeId, winnerUid, winnerUsername).addOnSuccessListener(u -> {
            mXPProvider.addXP(winnerUid, 30);
            mClanProvider.awardClanXPForUser(winnerUid, 30);
            Toast.makeText(this, "🏆 ¡@" + winnerUsername + " ganó el desafío! +30 XP", Toast.LENGTH_LONG).show();
            loadChallenge();
        });
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
