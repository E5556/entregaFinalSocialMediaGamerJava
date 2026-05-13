package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.WeeklyChallenge;
import com.optic.socialmediagamer.models.WeeklyChallengeEntry;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.WeeklyChallengeProvider;
import com.optic.socialmediagamer.providers.XPProvider;

import java.util.List;

public class WeeklyChallengeActivity extends AppCompatActivity {

    TextView mTextTitle, mTextDescription, mTextStatus, mTextWinner;
    Button mButtonSubmit, mButtonCreateChallenge;
    LinearLayout mLayoutEntries, mLayoutAdminCreate;

    WeeklyChallengeProvider mProvider;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    XPProvider mXPProvider;

    WeeklyChallenge mChallenge;
    String mChallengeId;

    // Hardcoded admin UID — replace with your own Firebase UID to control who can create challenges
    private static final String ADMIN_UID = "REPLACE_WITH_ADMIN_UID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_challenge);

        Toolbar toolbar = findViewById(R.id.toolbarWeeklyChallenge);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mTextTitle          = findViewById(R.id.textViewChallengeTitle);
        mTextDescription    = findViewById(R.id.textViewChallengeDescription);
        mTextStatus         = findViewById(R.id.textViewChallengeStatus);
        mTextWinner         = findViewById(R.id.textViewChallengeWinner);
        mButtonSubmit       = findViewById(R.id.buttonSubmitEntry);
        mButtonCreateChallenge = findViewById(R.id.buttonCreateWeeklyChallenge);
        mLayoutEntries      = findViewById(R.id.layoutChallengeEntries);
        mLayoutAdminCreate  = findViewById(R.id.layoutAdminCreate);

        mProvider      = new WeeklyChallengeProvider();
        mAuthProvider  = new AuthProvider();
        mUsersProvider = new UsersProvider();
        mXPProvider    = new XPProvider();

        mButtonSubmit.setOnClickListener(v -> showSubmitDialog());
        mButtonCreateChallenge.setOnClickListener(v -> showCreateChallengeDialog());

        loadChallenge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChallenge();
    }

    private void loadChallenge() {
        mProvider.getThisWeek().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                showNoChallengeState();
                return;
            }
            mChallenge = doc.toObject(WeeklyChallenge.class);
            if (mChallenge == null) { showNoChallengeState(); return; }
            mChallengeId = doc.getId();
            renderChallenge();
        });
    }

    private void showNoChallengeState() {
        mTextTitle.setText("⏳ Sin reto esta semana");
        mTextDescription.setText("Aún no se ha publicado el reto de esta semana.");
        mTextStatus.setVisibility(View.GONE);
        mTextWinner.setVisibility(View.GONE);
        mButtonSubmit.setVisibility(View.GONE);
        mLayoutEntries.removeAllViews();

        boolean isAdmin = ADMIN_UID.equals(mAuthProvider.getUid());
        mLayoutAdminCreate.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private void renderChallenge() {
        String myId = mAuthProvider.getUid();
        boolean isAdmin = ADMIN_UID.equals(myId);
        String status = mChallenge.getStatus() != null ? mChallenge.getStatus() : "active";

        mTextTitle.setText("🎯 " + mChallenge.getTitle());
        mTextDescription.setText(mChallenge.getDescription());

        switch (status) {
            case "active":
                mTextStatus.setText("✅ ACTIVO — Envía tu participación");
                mTextStatus.setTextColor(getColor(R.color.color_primary));
                mButtonSubmit.setVisibility(View.VISIBLE);
                break;
            case "voting":
                mTextStatus.setText("🗳️ VOTACIÓN — Vota por tu favorito");
                mTextStatus.setTextColor(getColor(android.R.color.holo_orange_light));
                mButtonSubmit.setVisibility(View.GONE);
                break;
            case "finished":
                mTextStatus.setText("🏆 FINALIZADO");
                mTextStatus.setTextColor(getColor(android.R.color.darker_gray));
                mButtonSubmit.setVisibility(View.GONE);
                if (mChallenge.getWinnerUsername() != null) {
                    mTextWinner.setVisibility(View.VISIBLE);
                    mTextWinner.setText("🏆 Ganador: @" + mChallenge.getWinnerUsername() + " · +50 XP");
                }
                break;
        }

        mLayoutAdminCreate.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        loadEntries(myId, status, isAdmin);
    }

    private void loadEntries(String myId, String status, boolean isAdmin) {
        mLayoutEntries.removeAllViews();
        mProvider.getEntries(mChallengeId).addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            if (docs.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("Aún no hay participaciones. ¡Sé el primero!");
                tv.setTextColor(getColor(R.color.color_text_secondary));
                tv.setPadding(16, 16, 16, 16);
                mLayoutEntries.addView(tv);
                return;
            }

            // Find max votes to declare winner if admin in voting phase
            int maxVotes = 0;
            String topEntryId = null, topUserId = null, topUsername = null;

            for (DocumentSnapshot doc : docs) {
                WeeklyChallengeEntry entry = doc.toObject(WeeklyChallengeEntry.class);
                if (entry == null) continue;
                int voteCount = entry.getVotes() != null ? entry.getVotes().size() : 0;
                if (voteCount > maxVotes) {
                    maxVotes = voteCount;
                    topEntryId = entry.getId();
                    topUserId = entry.getIdUser();
                    topUsername = entry.getUsername();
                }
                addEntryRow(entry, myId, status);
            }

            // Admin: declare winner button in voting phase
            if (isAdmin && "voting".equals(status) && topUserId != null) {
                final String finalTopId = topUserId;
                final String finalTopName = topUsername;
                Button btnWinner = new Button(this);
                btnWinner.setText("🏆 DECLARAR GANADOR (" + topUsername + ")");
                btnWinner.setTextSize(12);
                btnWinner.setBackgroundTintList(getColorStateList(R.color.color_primary));
                btnWinner.setTextColor(getColor(R.color.color_background));
                btnWinner.setOnClickListener(v -> {
                    mProvider.setWinner(mChallengeId, finalTopId, finalTopName)
                            .addOnSuccessListener(u -> {
                                mXPProvider.addXP(finalTopId, 50);
                                Toast.makeText(this, "🏆 ¡@" + finalTopName + " gana el reto! +50 XP", Toast.LENGTH_LONG).show();
                                loadChallenge();
                            });
                });
                mLayoutEntries.addView(btnWinner);
            }

            // Admin: open voting button in active phase
            if (isAdmin && "active".equals(status)) {
                Button btnVoting = new Button(this);
                btnVoting.setText("🗳️ ABRIR VOTACIÓN");
                btnVoting.setTextSize(12);
                btnVoting.setBackgroundTintList(getColorStateList(android.R.color.holo_orange_dark));
                btnVoting.setTextColor(getColor(R.color.color_background));
                btnVoting.setOnClickListener(v -> {
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("status", "voting");
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("weekly_challenges")
                            .document(mChallengeId)
                            .update(update)
                            .addOnSuccessListener(u -> { Toast.makeText(this, "Votación abierta", Toast.LENGTH_SHORT).show(); loadChallenge(); });
                });
                mLayoutEntries.addView(btnVoting);
            }
        });
    }

    private void addEntryRow(WeeklyChallengeEntry entry, String myId, String status) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(getDrawable(R.color.color_surface));
        card.setPadding(16, 14, 16, 14);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvUser = new TextView(this);
        tvUser.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvUser.setText("@" + entry.getUsername());
        tvUser.setTextColor(getColor(R.color.color_primary));
        tvUser.setTextSize(13);
        tvUser.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        header.addView(tvUser);

        int voteCount = entry.getVotes() != null ? entry.getVotes().size() : 0;
        boolean hasVoted = entry.getVotes() != null && entry.getVotes().contains(myId);
        boolean isOwn = myId.equals(entry.getIdUser());

        TextView tvVotes = new TextView(this);
        tvVotes.setText("❤️ " + voteCount);
        tvVotes.setTextColor(getColor(R.color.color_text_secondary));
        tvVotes.setTextSize(13);
        header.addView(tvVotes);
        card.addView(header);

        TextView tvEvidence = new TextView(this);
        tvEvidence.setText(entry.getEvidence());
        tvEvidence.setTextColor(getColor(R.color.color_text_primary));
        tvEvidence.setTextSize(13);
        tvEvidence.setPadding(0, 6, 0, 8);
        card.addView(tvEvidence);

        // Vote button — only in voting phase, not for own entry
        if ("voting".equals(status) && !isOwn) {
            Button btnVote = new Button(this);
            btnVote.setText(hasVoted ? "✅ VOTADO" : "❤️ VOTAR");
            btnVote.setTextSize(11);
            btnVote.setBackgroundTintList(getColorStateList(
                    hasVoted ? R.color.color_surface_2 : R.color.color_primary));
            btnVote.setTextColor(getColor(hasVoted ? R.color.color_text_secondary : R.color.color_background));
            btnVote.setOnClickListener(v -> {
                if (hasVoted) {
                    mProvider.unvote(mChallengeId, entry.getId(), myId)
                            .addOnSuccessListener(u -> loadChallenge());
                } else {
                    mProvider.vote(mChallengeId, entry.getId(), myId)
                            .addOnSuccessListener(u -> loadChallenge());
                }
            });
            card.addView(btnVote);
        }

        mLayoutEntries.addView(card);

        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 4, 0, 0);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(getColor(R.color.color_divider));
        mLayoutEntries.addView(divider);
    }

    private void showSubmitDialog() {
        EditText et = new EditText(this);
        et.setHint("Describe tu participación (clip, logro, screenshot...)");
        et.setMinLines(3);
        et.setPadding(24, 16, 24, 16);

        new AlertDialog.Builder(this)
                .setTitle("🎯 Enviar participación")
                .setView(et)
                .setPositiveButton("ENVIAR", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) return;
                    mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(doc -> {
                        String username = doc.exists() && doc.getString("username") != null
                                ? doc.getString("username") : "Gamer";
                        WeeklyChallengeEntry entry = new WeeklyChallengeEntry();
                        entry.setIdUser(mAuthProvider.getUid());
                        entry.setUsername(username);
                        entry.setEvidence(text);
                        entry.setTimestamp(System.currentTimeMillis());
                        mProvider.submitEntry(mChallengeId, entry)
                                .addOnSuccessListener(u -> {
                                    Toast.makeText(this, "¡Participación enviada!", Toast.LENGTH_SHORT).show();
                                    loadChallenge();
                                });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCreateChallengeDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Título del reto (ej: Mejor clip de la semana)");
        layout.addView(etTitle);

        EditText etDesc = new EditText(this);
        etDesc.setHint("Descripción / instrucciones");
        etDesc.setMinLines(2);
        layout.addView(etDesc);

        new AlertDialog.Builder(this)
                .setTitle("🎯 Crear Reto de la Semana")
                .setView(layout)
                .setPositiveButton("PUBLICAR", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc  = etDesc.getText().toString().trim();
                    if (title.isEmpty()) return;
                    mProvider.createWeeklyChallenge(title, desc)
                            .addOnSuccessListener(u -> {
                                Toast.makeText(this, "Reto publicado", Toast.LENGTH_SHORT).show();
                                loadChallenge();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
