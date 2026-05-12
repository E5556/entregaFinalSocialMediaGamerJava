package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.LFGPost;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.LFGProvider;

import java.util.Arrays;
import java.util.List;

public class CreateLFGActivity extends AppCompatActivity {

    private EditText mEditTextGame;
    private EditText mEditTextDescription;
    private EditText mEditTextSlots;
    private Spinner mSpinnerPlatform;
    private Spinner mSpinnerSchedule;

    private final LFGProvider mLFGProvider = new LFGProvider();
    private final AuthProvider mAuthProvider = new AuthProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lfg);

        mEditTextGame        = findViewById(R.id.editTextLFGGame);
        mEditTextDescription = findViewById(R.id.editTextLFGDescription);
        mEditTextSlots       = findViewById(R.id.editTextLFGSlots);
        mSpinnerPlatform     = findViewById(R.id.spinnerLFGPlatform);
        mSpinnerSchedule     = findViewById(R.id.spinnerLFGSchedule);

        setSupportActionBar(findViewById(R.id.toolbarCreateLFG));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        List<String> platforms = Arrays.asList("PC", "PS4", "XBOX", "Nintendo");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, platforms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerPlatform.setAdapter(adapter);

        List<String> schedules = Arrays.asList("Cualquier hora", "Mañana", "Tarde", "Noche", "Madrugada");
        ArrayAdapter<String> scheduleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, schedules);
        scheduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSchedule.setAdapter(scheduleAdapter);

        Button btnPublish = findViewById(R.id.buttonPublishLFG);
        btnPublish.setOnClickListener(v -> publishLFG());
    }

    private void publishLFG() {
        String game = mEditTextGame.getText().toString().trim();
        String description = mEditTextDescription.getText().toString().trim();
        String slotsStr = mEditTextSlots.getText().toString().trim();
        String platform = mSpinnerPlatform.getSelectedItem().toString();
        String schedule = mSpinnerSchedule.getSelectedItem().toString();

        if (game.isEmpty() || description.isEmpty() || slotsStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int slots;
        try {
            slots = Integer.parseInt(slotsStr);
            if (slots < 1 || slots > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Los slots deben ser entre 1 y 10", Toast.LENGTH_SHORT).show();
            return;
        }

        LFGPost lfg = new LFGPost();
        lfg.setIdUser(mAuthProvider.getUid());
        lfg.setGame(game);
        lfg.setPlatform(platform);
        lfg.setDescription(description);
        lfg.setSlotsTotal(slots);
        lfg.setSchedule(schedule);

        mLFGProvider.save(lfg).addOnSuccessListener(v -> {
            Toast.makeText(this, "¡Búsqueda publicada! 👥", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e ->
            Toast.makeText(this, "Error al publicar", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
