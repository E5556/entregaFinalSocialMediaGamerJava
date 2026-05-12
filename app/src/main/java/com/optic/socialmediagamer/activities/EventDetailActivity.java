package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.EventsProvider;
import com.optic.socialmediagamer.providers.PredictionsProvider;
import com.optic.socialmediagamer.providers.XPProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventDetailActivity extends AppCompatActivity {

    private TextView mTextViewTitle, mTextViewGame, mTextViewPlatform,
            mTextViewDate, mTextViewDescription, mTextViewAttendeeCount;
    private Button mBtnLink, mBtnRSVP, mBtnGroupChat, mBtnSetupPrediction;
    private CardView mCardPrediction;
    private TextView mTextPredictionQuestion, mTextPredictionStatus, mTextPredictionResult;
    private LinearLayout mLayoutPredictionOptions;

    private AuthProvider mAuthProvider;
    private EventsProvider mEventsProvider;
    private PredictionsProvider mPredictionsProvider;
    private XPProvider mXPProvider;

    private String mEventId;
    private String mExternalLink;
    private String mEventCreatorId;
    private boolean mIsAttending = false;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy - HH:mm", new Locale("es", "CO"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        mEventId = getIntent().getStringExtra("idEvent");

        mTextViewTitle         = findViewById(R.id.textViewEventDetailTitle);
        mTextViewGame          = findViewById(R.id.textViewEventDetailGame);
        mTextViewPlatform      = findViewById(R.id.textViewEventDetailPlatform);
        mTextViewDate          = findViewById(R.id.textViewEventDetailDate);
        mTextViewDescription   = findViewById(R.id.textViewEventDetailDescription);
        mTextViewAttendeeCount = findViewById(R.id.textViewEventDetailAttendeeCount);
        mBtnLink               = findViewById(R.id.btnEventDetailLink);
        mBtnRSVP               = findViewById(R.id.btnEventDetailRSVP);
        mBtnGroupChat          = findViewById(R.id.btnEventGroupChat);
        mBtnSetupPrediction    = findViewById(R.id.btnSetupPrediction);
        mCardPrediction        = findViewById(R.id.cardPrediction);
        mTextPredictionQuestion = findViewById(R.id.textViewPredictionQuestion);
        mTextPredictionStatus  = findViewById(R.id.textViewPredictionStatus);
        mTextPredictionResult  = findViewById(R.id.textViewPredictionResult);
        mLayoutPredictionOptions = findViewById(R.id.layoutPredictionOptions);

        mAuthProvider        = new AuthProvider();
        mEventsProvider      = new EventsProvider();
        mPredictionsProvider = new PredictionsProvider();
        mXPProvider          = new XPProvider();

        loadEvent();

        mBtnRSVP.setOnClickListener(v -> toggleRSVP());
        mBtnGroupChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupChatActivity.class);
            intent.putExtra("groupId", "event_" + mEventId);
            intent.putExtra("groupTitle", "💬 Chat del evento");
            startActivity(intent);
        });
        mBtnLink.setOnClickListener(v -> {
            if (mExternalLink != null && !mExternalLink.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mExternalLink)));
            }
        });
        mBtnSetupPrediction.setOnClickListener(v -> showSetupPredictionDialog());
    }

    private void loadEvent() {
        String myId = mAuthProvider.getUid();
        mEventsProvider.getEvent(mEventId).addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            mEventCreatorId    = doc.getString("idUser");
            String title       = doc.getString("title");
            String game        = doc.getString("game");
            String platform    = doc.getString("platform");
            String description = doc.getString("description");
            mExternalLink      = doc.getString("externalLink");
            long eventDate     = doc.getLong("eventDate") != null ? doc.getLong("eventDate") : 0;
            long attendeeCount = doc.getLong("attendeeCount") != null ? doc.getLong("attendeeCount") : 0;

            mTextViewTitle.setText(title != null ? title : "");
            mTextViewGame.setText("🎮 " + (game != null ? game : ""));
            mTextViewPlatform.setText(platform != null ? platform : "");
            mTextViewDescription.setText(description != null ? description : "");
            mTextViewAttendeeCount.setText(attendeeCount + " asistentes");

            if (eventDate > 0) mTextViewDate.setText("📅 " + sdf.format(new Date(eventDate)));
            if (mExternalLink != null && !mExternalLink.isEmpty()) mBtnLink.setVisibility(View.VISIBLE);

            boolean isCreator = mEventCreatorId != null && mEventCreatorId.equals(myId);
            if (isCreator) mBtnSetupPrediction.setVisibility(View.VISIBLE);

            if (myId != null) {
                Object attending = doc.get("attendees." + myId);
                mIsAttending = attending != null;
                updateRSVPButton();
            }

            loadPrediction(myId, isCreator);
        });
    }

    private void loadPrediction(String myId, boolean isCreator) {
        mPredictionsProvider.get(mEventId).addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            String question = doc.getString("question");
            List<String> options = (List<String>) doc.get("options");
            boolean resolved = Boolean.TRUE.equals(doc.getBoolean("resolved"));
            Long correctOpt = doc.getLong("correctOption");
            int correctOption = correctOpt != null ? correctOpt.intValue() : -1;
            Map<String, Object> votes = (Map<String, Object>) doc.get("votes");
            Long myVoteLong = votes != null && myId != null ? (Long) votes.get(myId) : null;
            int myVote = myVoteLong != null ? myVoteLong.intValue() : -1;

            if (question == null || options == null) return;

            mCardPrediction.setVisibility(View.VISIBLE);
            mBtnSetupPrediction.setVisibility(View.GONE);
            mTextPredictionQuestion.setText(question);
            mTextPredictionStatus.setText(resolved ? "RESUELTA" : "ABIERTA");

            mLayoutPredictionOptions.removeAllViews();

            for (int i = 0; i < options.size(); i++) {
                final int idx = i;
                String opt = options.get(i);

                // Count votes for this option
                int voteCount = 0;
                if (votes != null) {
                    for (Object v : votes.values()) {
                        if (v instanceof Long && ((Long) v).intValue() == idx) voteCount++;
                    }
                }
                int totalVotes = votes != null ? votes.size() : 0;
                int pct = totalVotes > 0 ? (voteCount * 100 / totalVotes) : 0;

                View optView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
                TextView tv = optView.findViewById(android.R.id.text1);

                String prefix = "";
                if (resolved && idx == correctOption) prefix = "✅ ";
                else if (resolved && idx != correctOption) prefix = "❌ ";
                else if (myVote == idx) prefix = "● ";

                tv.setText(prefix + opt + "  (" + pct + "% · " + voteCount + " votos)");
                tv.setTextColor(resolved && idx == correctOption ? Color.parseColor("#00F0FF") : Color.parseColor("#E0E0E0"));
                tv.setPadding(0, 10, 0, 10);
                tv.setTextSize(13f);

                if (!resolved && myId != null) {
                    tv.setClickable(true);
                    tv.setFocusable(true);
                    tv.setOnClickListener(v -> {
                        mPredictionsProvider.vote(mEventId, myId, idx)
                                .addOnSuccessListener(u -> {
                                    Toast.makeText(this, "Voto registrado ⚡", Toast.LENGTH_SHORT).show();
                                    loadPrediction(myId, isCreator);
                                });
                    });
                }

                // Creator resolve option
                if (isCreator && !resolved) {
                    tv.setOnLongClickListener(v -> {
                        new AlertDialog.Builder(this)
                                .setTitle("¿Marcar como opción correcta?")
                                .setMessage("\"" + opt + "\"")
                                .setPositiveButton("Sí, resolver", (d, w) -> resolvePrediction(idx, votes))
                                .setNegativeButton("Cancelar", null)
                                .show();
                        return true;
                    });
                }

                mLayoutPredictionOptions.addView(tv);
            }

            if (resolved && myVote == correctOption && myVote >= 0) {
                mTextPredictionResult.setVisibility(View.VISIBLE);
                mTextPredictionResult.setText("🎉 ¡Acertaste la predicción! +15 XP");
            } else if (resolved) {
                mTextPredictionResult.setVisibility(View.VISIBLE);
                mTextPredictionResult.setText(myVote >= 0 ? "No acertaste esta vez 😢" : "No participaste en la predicción");
            }
        });
    }

    private void resolvePrediction(int correctOption, Map<String, Object> votes) {
        mPredictionsProvider.resolve(mEventId, correctOption).addOnSuccessListener(u -> {
            Toast.makeText(this, "¡Predicción resuelta! ⚡", Toast.LENGTH_SHORT).show();
            // Award XP to correct voters
            if (votes != null) {
                for (Map.Entry<String, Object> entry : votes.entrySet()) {
                    if (entry.getValue() instanceof Long && ((Long) entry.getValue()).intValue() == correctOption) {
                        mXPProvider.addXP(entry.getKey(), 15);
                    }
                }
            }
            loadEvent();
        });
    }

    private void showSetupPredictionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚡ Configurar predicción");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etQuestion = new EditText(this);
        etQuestion.setHint("Pregunta (ej: ¿Quién ganará el torneo?)");
        layout.addView(etQuestion);

        EditText etOpt1 = new EditText(this); etOpt1.setHint("Opción A"); layout.addView(etOpt1);
        EditText etOpt2 = new EditText(this); etOpt2.setHint("Opción B"); layout.addView(etOpt2);
        EditText etOpt3 = new EditText(this); etOpt3.setHint("Opción C (opcional)"); layout.addView(etOpt3);
        EditText etOpt4 = new EditText(this); etOpt4.setHint("Opción D (opcional)"); layout.addView(etOpt4);

        builder.setView(layout);
        builder.setPositiveButton("CREAR", (d, w) -> {
            String question = etQuestion.getText().toString().trim();
            String opt1 = etOpt1.getText().toString().trim();
            String opt2 = etOpt2.getText().toString().trim();
            if (question.isEmpty() || opt1.isEmpty() || opt2.isEmpty()) {
                Toast.makeText(this, "Completa la pregunta y al menos 2 opciones", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> options = new ArrayList<>();
            options.add(opt1); options.add(opt2);
            if (!etOpt3.getText().toString().trim().isEmpty()) options.add(etOpt3.getText().toString().trim());
            if (!etOpt4.getText().toString().trim().isEmpty()) options.add(etOpt4.getText().toString().trim());

            mPredictionsProvider.setup(mEventId, question, options).addOnSuccessListener(u -> {
                Toast.makeText(this, "¡Predicción creada! ⚡", Toast.LENGTH_SHORT).show();
                loadEvent();
            });
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void toggleRSVP() {
        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        mBtnRSVP.setEnabled(false);
        if (mIsAttending) {
            mEventsProvider.cancelRsvp(mEventId, myId).addOnSuccessListener(v -> {
                mIsAttending = false;
                updateRSVPButton();
                decrementCount();
                mBtnRSVP.setEnabled(true);
                Toast.makeText(this, "Cancelaste tu asistencia", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> mBtnRSVP.setEnabled(true));
        } else {
            mEventsProvider.rsvp(mEventId, myId).addOnSuccessListener(v -> {
                mIsAttending = true;
                updateRSVPButton();
                incrementCount();
                mBtnRSVP.setEnabled(true);
                Toast.makeText(this, "Te apuntaste al evento", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> mBtnRSVP.setEnabled(true));
        }
    }

    private void updateRSVPButton() {
        mBtnRSVP.setText(mIsAttending ? "CANCELAR ASISTENCIA" : "APUNTARSE");
    }

    private void incrementCount() {
        try {
            long c = Long.parseLong(mTextViewAttendeeCount.getText().toString().replace(" asistentes", "").trim());
            mTextViewAttendeeCount.setText((c + 1) + " asistentes");
        } catch (NumberFormatException ignored) {}
    }

    private void decrementCount() {
        try {
            long c = Long.parseLong(mTextViewAttendeeCount.getText().toString().replace(" asistentes", "").trim());
            mTextViewAttendeeCount.setText(Math.max(0, c - 1) + " asistentes");
        } catch (NumberFormatException ignored) {}
    }
}
