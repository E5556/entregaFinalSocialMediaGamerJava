package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.adapters.LFGListAdapter;
import com.optic.socialmediagamer.models.LFGPost;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.LFGProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchPlayActivity extends AppCompatActivity {

    private EditText mEditTextGame;
    private Spinner mSpinnerPlatform;
    private Spinner mSpinnerSchedule;
    private RecyclerView mRecyclerView;
    private LFGListAdapter mAdapter;
    private TextView mTextNoResults;

    private final List<LFGPost> mResults = new ArrayList<>();
    private final LFGProvider mLFGProvider = new LFGProvider();
    private final AuthProvider mAuthProvider = new AuthProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_play);

        mEditTextGame   = findViewById(R.id.editTextMatchGame);
        mSpinnerPlatform = findViewById(R.id.spinnerMatchPlatform);
        mSpinnerSchedule = findViewById(R.id.spinnerMatchSchedule);
        mRecyclerView   = findViewById(R.id.recyclerViewMatchResults);
        mTextNoResults  = findViewById(R.id.textViewMatchNoResults);

        setSupportActionBar(findViewById(R.id.toolbarMatchPlay));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Spinner plataforma
        List<String> platforms = Arrays.asList("Cualquier plataforma", "PC", "PS4", "XBOX", "Nintendo");
        mSpinnerPlatform.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, platforms));

        // Spinner horario
        List<String> schedules = Arrays.asList("Cualquier hora", "Mañana", "Tarde", "Noche", "Madrugada");
        mSpinnerSchedule.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, schedules));

        // Pre-llenar con nowPlaying del usuario
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(doc -> {
            String nowPlaying = doc.getString("nowPlaying");
            if (nowPlaying != null && !nowPlaying.isEmpty()) {
                mEditTextGame.setText(nowPlaying);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new LFGListAdapter(mResults, this);
        mRecyclerView.setAdapter(mAdapter);

        Button btnSearch = findViewById(R.id.buttonMatchSearch);
        btnSearch.setOnClickListener(v -> findMatches());
    }

    private void findMatches() {
        String gameInput = mEditTextGame.getText().toString().trim().toLowerCase();
        String platform  = mSpinnerPlatform.getSelectedItem().toString();
        String schedule  = mSpinnerSchedule.getSelectedItem().toString();
        String myId      = mAuthProvider.getUid();

        boolean anyPlatform  = "Cualquier plataforma".equals(platform);
        boolean anySchedule  = "Cualquier hora".equals(schedule);

        mLFGProvider.getAll().get().addOnSuccessListener(snap -> {
            mResults.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                LFGPost post = doc.toObject(LFGPost.class);
                if (post == null) continue;
                post.setId(doc.getId());

                // Filtro: juego (contains, ignorando mayúsculas)
                if (!gameInput.isEmpty()) {
                    String postGame = post.getGame() != null ? post.getGame().toLowerCase() : "";
                    if (!postGame.contains(gameInput)) continue;
                }

                // Filtro: plataforma
                if (!anyPlatform && !platform.equalsIgnoreCase(post.getPlatform())) continue;

                // Filtro: horario
                if (!anySchedule) {
                    String postSchedule = post.getSchedule() != null ? post.getSchedule() : "Cualquier hora";
                    if (!schedule.equalsIgnoreCase(postSchedule) && !"Cualquier hora".equalsIgnoreCase(postSchedule)) continue;
                }

                // Filtro: con slots disponibles
                int joined = post.getPlayers() != null ? post.getPlayers().size() : 0;
                if (joined >= post.getSlotsTotal()) continue;

                // Filtro: no unido todavía
                if (myId != null && post.getPlayers() != null && post.getPlayers().contains(myId)) continue;

                mResults.add(post);
            }

            mAdapter.notifyDataSetChanged();
            mTextNoResults.setVisibility(mResults.isEmpty()
                    ? android.view.View.VISIBLE : android.view.View.GONE);
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
