package com.optic.socialmediagamer.activities;

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
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.TournamentAlertProvider;

import java.util.List;

public class TournamentAlertsActivity extends AppCompatActivity {

    EditText mEditTextGame;
    Button mButtonSubscribe;
    LinearLayout mLayoutSubscriptions;
    TextView mTextViewEmpty;

    TournamentAlertProvider mAlertProvider;
    AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_alerts);

        Toolbar toolbar = findViewById(R.id.toolbarTournamentAlerts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mEditTextGame        = findViewById(R.id.editTextAlertGame);
        mButtonSubscribe     = findViewById(R.id.buttonSubscribeAlert);
        mLayoutSubscriptions = findViewById(R.id.layoutSubscriptions);
        mTextViewEmpty       = findViewById(R.id.textViewAlertsEmpty);

        mAlertProvider = new TournamentAlertProvider();
        mAuthProvider  = new AuthProvider();

        mButtonSubscribe.setOnClickListener(v -> subscribeToGame());
        loadSubscriptions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubscriptions();
    }

    private void subscribeToGame() {
        String game = mEditTextGame.getText().toString().trim();
        if (game.isEmpty()) {
            Toast.makeText(this, "Escribe el nombre del juego", Toast.LENGTH_SHORT).show();
            return;
        }
        String myId = mAuthProvider.getUid();
        mAlertProvider.subscribe(myId, game).addOnSuccessListener(u -> {
            mEditTextGame.setText("");
            Toast.makeText(this, "Suscrito a torneos de " + game, Toast.LENGTH_SHORT).show();
            loadSubscriptions();
        });
    }

    private void loadSubscriptions() {
        mLayoutSubscriptions.removeAllViews();
        mAlertProvider.getSubscriptions(mAuthProvider.getUid()).addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            mTextViewEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
            for (DocumentSnapshot doc : docs) {
                String gameDisplay = doc.getString("gameDisplay");
                if (gameDisplay == null) gameDisplay = doc.getString("game");
                addSubscriptionRow(gameDisplay, doc.getString("game"));
            }
        });
    }

    private void addSubscriptionRow(String gameDisplay, String gameKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 14, 16, 14);

        TextView tvGame = new TextView(this);
        tvGame.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvGame.setText("🔔 " + gameDisplay);
        tvGame.setTextColor(getColor(R.color.color_text_primary));
        tvGame.setTextSize(14);
        row.addView(tvGame);

        TextView tvRemove = new TextView(this);
        tvRemove.setText("CANCELAR");
        tvRemove.setTextColor(getColor(R.color.color_primary));
        tvRemove.setTextSize(11);
        tvRemove.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tvRemove.setOnClickListener(v -> {
            mAlertProvider.unsubscribe(mAuthProvider.getUid(), gameKey)
                    .addOnSuccessListener(u -> {
                        Toast.makeText(this, "Suscripción cancelada", Toast.LENGTH_SHORT).show();
                        loadSubscriptions();
                    });
        });
        row.addView(tvRemove);

        mLayoutSubscriptions.addView(row);

        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(getColor(R.color.color_divider));
        mLayoutSubscriptions.addView(divider);
    }
}
