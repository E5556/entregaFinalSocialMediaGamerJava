package com.optic.socialmediagamer.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.models.Tournament;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.TournamentAlertProvider;
import com.optic.socialmediagamer.providers.TournamentProvider;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateTournamentActivity extends AppCompatActivity {

    private EditText mEditGame, mEditFormat;
    private TextView mTextDate;
    private Spinner mSpinnerMax;
    private long mSelectedDateMs = 0;

    private final AuthProvider mAuthProvider = new AuthProvider();
    private final TournamentProvider mTournamentProvider = new TournamentProvider();
    private final TournamentAlertProvider mAlertProvider = new TournamentAlertProvider();
    private final NotificationsProvider mNotificationsProvider = new NotificationsProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_tournament);

        setSupportActionBar(findViewById(R.id.toolbarCreateTournament));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditGame   = findViewById(R.id.editTextTournamentGame);
        mEditFormat = findViewById(R.id.editTextTournamentFormat);
        mTextDate   = findViewById(R.id.textViewTournamentDatePicker);
        mSpinnerMax = findViewById(R.id.spinnerMaxPlayers);

        mSpinnerMax.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Arrays.asList(2, 4, 8, 16)));

        mTextDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d, 20, 0, 0);
                mSelectedDateMs = sel.getTimeInMillis();
                mTextDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(mSelectedDateMs)));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        Button btnCreate = findViewById(R.id.buttonCreateTournament);
        btnCreate.setOnClickListener(v -> saveTournament());
    }

    private void saveTournament() {
        String game   = mEditGame.getText().toString().trim();
        String format = mEditFormat.getText().toString().trim();

        if (game.isEmpty() || format.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSelectedDateMs == 0) {
            Toast.makeText(this, "Selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxPlayers = (int) mSpinnerMax.getSelectedItem();

        Tournament t = new Tournament();
        t.setIdUser(mAuthProvider.getUid());
        t.setGame(game);
        t.setFormat(format);
        t.setDateTimestamp(mSelectedDateMs);
        t.setMaxPlayers(maxPlayers);
        t.setTimestamp(new Date().getTime());

        mTournamentProvider.create(t).addOnSuccessListener(u -> {
            Toast.makeText(this, "¡Torneo creado! ⚔️", Toast.LENGTH_SHORT).show();
            notifySubscribers(game, t.getIdUser());
            finish();
        });
    }

    private void notifySubscribers(String game, String creatorId) {
        mAlertProvider.getSubscribers(game).addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String subscriberId = doc.getString("userId");
                if (subscriberId == null || subscriberId.equals(creatorId)) continue;
                Notification n = new Notification();
                n.setType("tournament_alert");
                n.setIdFrom(creatorId);
                n.setIdTo(subscriberId);
                n.setBody("⚔️ Nuevo torneo de " + game + " disponible. ¡Inscríbete!");
                n.setRead(false);
                n.setTimestamp(System.currentTimeMillis());
                mNotificationsProvider.save(n);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
