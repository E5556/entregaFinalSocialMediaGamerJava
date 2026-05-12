package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.CreateTournamentActivity;
import com.optic.socialmediagamer.activities.TournamentAlertsActivity;
import com.optic.socialmediagamer.activities.TournamentDetailActivity;
import com.optic.socialmediagamer.models.Tournament;
import com.optic.socialmediagamer.providers.TournamentProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TournamentsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TournamentAdapter mAdapter;
    private final List<Tournament> mTournaments = new ArrayList<>();
    private final TournamentProvider mProvider = new TournamentProvider();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tournaments, container, false);

        mRecyclerView = view.findViewById(R.id.recyclerViewTournaments);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new TournamentAdapter(mTournaments);
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fabAlerts = view.findViewById(R.id.fabTournamentAlerts);
        fabAlerts.setOnClickListener(v -> startActivity(new Intent(getActivity(), TournamentAlertsActivity.class)));

        FloatingActionButton fab = view.findViewById(R.id.fabCreateTournament);
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), CreateTournamentActivity.class)));

        loadTournaments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTournaments();
    }

    private void loadTournaments() {
        mProvider.getAll().get().addOnSuccessListener(snap -> {
            mTournaments.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tournament t = doc.toObject(Tournament.class);
                if (t != null) { t.setId(doc.getId()); mTournaments.add(t); }
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    // ---- Inner adapter ----

    class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.VH> {
        private final List<Tournament> items;
        TournamentAdapter(List<Tournament> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tournament, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Tournament t = items.get(position);
            h.textGame.setText(t.getGame());
            h.textFormat.setText(t.getFormat());

            int joined = t.getPlayers() != null ? t.getPlayers().size() : 0;
            h.textPlayers.setText("👥 " + joined + "/" + t.getMaxPlayers() + " jugadores");

            String dateStr = new SimpleDateFormat("dd/MM", Locale.getDefault())
                    .format(new Date(t.getDateTimestamp()));
            h.textDate.setText("📅 " + dateStr);

            switch (t.getStatus() != null ? t.getStatus() : "open") {
                case "open":     h.textStatus.setText("ABIERTO"); break;
                case "started":  h.textStatus.setText("EN JUEGO"); break;
                case "finished": h.textStatus.setText("FINALIZADO"); break;
            }

            if ("finished".equals(t.getStatus()) && t.getWinnerUsername() != null) {
                h.textWinner.setVisibility(View.VISIBLE);
                h.textWinner.setText("🏆 @" + t.getWinnerUsername());
            } else {
                h.textWinner.setVisibility(View.GONE);
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TournamentDetailActivity.class);
                intent.putExtra("tournamentId", t.getId());
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView textGame, textFormat, textPlayers, textDate, textStatus, textWinner;
            VH(View v) {
                super(v);
                textGame    = v.findViewById(R.id.textViewTournamentGame);
                textFormat  = v.findViewById(R.id.textViewTournamentFormat);
                textPlayers = v.findViewById(R.id.textViewTournamentPlayers);
                textDate    = v.findViewById(R.id.textViewTournamentDate);
                textStatus  = v.findViewById(R.id.textViewTournamentStatus);
                textWinner  = v.findViewById(R.id.textViewTournamentWinner);
            }
        }
    }
}
