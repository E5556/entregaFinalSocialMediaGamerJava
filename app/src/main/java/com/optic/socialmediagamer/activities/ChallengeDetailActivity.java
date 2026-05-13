package com.optic.socialmediagamer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.squareup.picasso.Picasso;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.ActivityFeedItem;
import com.optic.socialmediagamer.models.Challenge;
import com.optic.socialmediagamer.providers.ActivityFeedProvider;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ChallengesProvider;
import com.optic.socialmediagamer.providers.ClanProvider;
import com.optic.socialmediagamer.providers.ImageProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.utils.FileUtil;

import java.io.File;
import java.util.List;

public class ChallengeDetailActivity extends AppCompatActivity {

    private String mChallengeId;
    private Challenge mChallenge;

    private TextView mTextPlayers, mTextStatus, mTextDescription, mTextWinner;
    private TextView mTextEvidenceChallenger, mTextEvidenceChallenged;
    private EditText mEditEvidence;
    private ImageView mImageEvidencePreview, mImageEvidenceChallengerImg, mImageEvidenceChallengedImg;
    private LinearLayout mLayoutAcceptReject, mLayoutEvidenceButtons;
    private CardView mCardEvidence, mCardVoting;
    private Button mBtnAccept, mBtnReject, mBtnSubmitEvidence, mBtnPickImage;
    private Button mBtnVoteChallenger, mBtnVoteChallenged, mBtnFinish;

    private static final int GALLERY_REQUEST = 77;
    private File mEvidenceImageFile;
    private String mEvidenceImageUrl = null;
    private final ImageProvider mImageProvider = new ImageProvider();

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

        mTextPlayers             = findViewById(R.id.textViewDetailPlayers);
        mTextStatus              = findViewById(R.id.textViewDetailStatus);
        mTextDescription         = findViewById(R.id.textViewDetailDescription);
        mTextWinner              = findViewById(R.id.textViewDetailWinner);
        mTextEvidenceChallenger      = findViewById(R.id.textViewEvidenceChallenger);
        mTextEvidenceChallenged      = findViewById(R.id.textViewEvidenceChallenged);
        mEditEvidence                = findViewById(R.id.editTextEvidence);
        mImageEvidencePreview        = findViewById(R.id.imageViewEvidencePreview);
        mImageEvidenceChallengerImg  = findViewById(R.id.imageViewEvidenceChallengerImg);
        mImageEvidenceChallengedImg  = findViewById(R.id.imageViewEvidenceChallengedImg);
        mLayoutAcceptReject      = findViewById(R.id.layoutAcceptReject);
        mLayoutEvidenceButtons   = findViewById(R.id.layoutEvidenceButtons);
        mCardEvidence            = findViewById(R.id.cardEvidence);
        mCardVoting              = findViewById(R.id.cardVoting);
        mBtnAccept               = findViewById(R.id.buttonAccept);
        mBtnReject               = findViewById(R.id.buttonReject);
        mBtnSubmitEvidence       = findViewById(R.id.buttonSubmitEvidence);
        mBtnPickImage            = findViewById(R.id.buttonPickEvidenceImage);
        mBtnVoteChallenger       = findViewById(R.id.buttonVoteChallenger);
        mBtnVoteChallenged       = findViewById(R.id.buttonVoteChallenged);
        mBtnFinish               = findViewById(R.id.buttonFinish);

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
        // Reset all sections before rendering to avoid stale state on reload
        mLayoutAcceptReject.setVisibility(View.GONE);
        mCardEvidence.setVisibility(View.GONE);
        mCardVoting.setVisibility(View.GONE);
        mTextWinner.setVisibility(View.GONE);
        mTextEvidenceChallenger.setVisibility(View.GONE);
        mTextEvidenceChallenged.setVisibility(View.GONE);
        mEditEvidence.setVisibility(View.GONE);
        mImageEvidencePreview.setVisibility(View.GONE);
        mImageEvidenceChallengerImg.setVisibility(View.GONE);
        mImageEvidenceChallengedImg.setVisibility(View.GONE);
        mLayoutEvidenceButtons.setVisibility(View.GONE);
        mBtnFinish.setVisibility(View.GONE);

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

        // Evidence section (accepted / voting / finished)
        if ("accepted".equals(status) || "voting".equals(status) || "finished".equals(status)) {
            mCardEvidence.setVisibility(View.VISIBLE);

            String evC = mChallenge.getEvidenceChallenger();
            String evD = mChallenge.getEvidenceChallenged();

            if (evC != null && !evC.isEmpty()) {
                mTextEvidenceChallenger.setVisibility(View.VISIBLE);
                renderEvidenceInView(mTextEvidenceChallenger, mImageEvidenceChallengerImg, "@" + mChallengerUsername, evC);
            }
            if (evD != null && !evD.isEmpty()) {
                mTextEvidenceChallenged.setVisibility(View.VISIBLE);
                renderEvidenceInView(mTextEvidenceChallenged, mImageEvidenceChallengedImg, "@" + mChallengedUsername, evD);
            }

            boolean needsEvidence = (isChallenger && (evC == null || evC.isEmpty()))
                    || (isChallenged && (evD == null || evD.isEmpty()));
            if (needsEvidence && "accepted".equals(status)) {
                mEditEvidence.setVisibility(View.VISIBLE);
                mLayoutEvidenceButtons.setVisibility(View.VISIBLE);
                mBtnPickImage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Seleccionar imagen"), GALLERY_REQUEST);
                });
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
                mBtnVoteChallenger.setEnabled(true);
                mBtnVoteChallenged.setEnabled(true);
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
                mBtnFinish.setEnabled(true);
                int totalVotes = vc + vd;
                if (totalVotes > 0) {
                    mBtnFinish.setText("🏆 CERRAR VOTACIÓN Y DECLARAR GANADOR");
                    mBtnFinish.setOnClickListener(v -> declareWinner(vc, vd));
                } else {
                    mBtnFinish.setText("⛔ CERRAR SIN GANADOR");
                    mBtnFinish.setOnClickListener(v -> closeWithoutWinner());
                }
            }
        }
    }

    private void renderEvidenceInView(TextView tv, ImageView imgView, String username, String evidence) {
        // Split off any trailing URL (appended as "text\nurl" on submit)
        String[] parts = evidence.split("\n", 2);
        String text = parts[0].trim();
        String url = parts.length > 1 ? parts[1].trim() : null;

        // If there's no text prefix the whole value may itself be a URL
        if (text.startsWith("https://") || text.startsWith("http://")) {
            url = text;
            text = "";
        }

        if (text.isEmpty()) {
            tv.setText(username + ": [imagen]");
        } else {
            tv.setText(username + ": " + text);
        }

        if (url != null && !url.isEmpty() && imgView != null) {
            imgView.setVisibility(View.VISIBLE);
            Picasso.get().load(url).into(imgView);
        }
    }

    private void submitEvidence(boolean isChallenger, String evC, String evD) {
        String text = mEditEvidence.getText().toString().trim();
        if (text.isEmpty() && mEvidenceImageFile == null) {
            Toast.makeText(this, "Escribe tu evidencia o adjunta una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mEvidenceImageFile != null) {
            mBtnSubmitEvidence.setEnabled(false);
            mBtnSubmitEvidence.setText("Subiendo...");
            mImageProvider.save(this, mEvidenceImageFile).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    mBtnSubmitEvidence.setEnabled(true);
                    mBtnSubmitEvidence.setText("ENVIAR EVIDENCIA");
                    Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    return;
                }
                mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    String combined = text.isEmpty() ? uri.toString() : text + "\n" + uri.toString();
                    saveEvidence(isChallenger, combined, evC, evD);
                });
            });
        } else {
            saveEvidence(isChallenger, text, evC, evD);
        }
    }

    private void saveEvidence(boolean isChallenger, String evidence, String evC, String evD) {
        if (isChallenger) {
            mChallengesProvider.submitEvidenceChallenger(mChallengeId, evidence)
                    .addOnSuccessListener(u -> checkOpenVoting(evidence, evD));
        } else {
            mChallengesProvider.submitEvidenceChallenged(mChallengeId, evidence)
                    .addOnSuccessListener(u -> checkOpenVoting(evC, evidence));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                mEvidenceImageFile = FileUtil.from(this, uri);
                mImageEvidencePreview.setVisibility(View.VISIBLE);
                Picasso.get().load(uri).into(mImageEvidencePreview);
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            }
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

    private void closeWithoutWinner() {
        mChallengesProvider.finishNoWinner(mChallengeId).addOnSuccessListener(u -> {
            Toast.makeText(this, "Votación cerrada. No se definió un ganador.", Toast.LENGTH_LONG).show();
            loadChallenge();
        });
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
            // Activity feed
            ActivityFeedItem feedItem = new ActivityFeedItem();
            feedItem.setIdUser(winnerUid);
            feedItem.setUsername(winnerUsername);
            feedItem.setType("CHALLENGE_WIN");
            feedItem.setMessage("ganó un desafío ⚔️");
            feedItem.setTimestamp(System.currentTimeMillis());
            new ActivityFeedProvider().save(feedItem);
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
