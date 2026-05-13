package com.optic.socialmediagamer.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.GroupChatActivity;
import com.optic.socialmediagamer.models.LFGPost;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.LFGProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.ReputationProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.models.GameRating;

import java.util.ArrayList;
import java.util.List;

public class LFGListAdapter extends RecyclerView.Adapter<LFGListAdapter.ViewHolder> {

    private final Context context;
    private final List<LFGPost> items;
    private final LFGProvider mLFGProvider;
    private final AuthProvider mAuthProvider;
    private final UsersProvider mUsersProvider;
    private final ReputationProvider mReputationProvider;

    public LFGListAdapter(List<LFGPost> items, Context context) {
        this.items = items;
        this.context = context;
        mLFGProvider = new LFGProvider();
        mAuthProvider = new AuthProvider();
        mUsersProvider = new UsersProvider();
        mReputationProvider = new ReputationProvider();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LFGPost lfg = items.get(position);
        String lfgId = lfg.getId();
        String myId  = mAuthProvider.getUid();

        holder.textViewGame.setText(lfg.getGame());
        holder.textViewPlatform.setText(lfg.getPlatform());
        holder.textViewDescription.setText(lfg.getDescription());

        if (lfg.getSchedule() != null && !lfg.getSchedule().isEmpty()
                && !"Cualquier hora".equals(lfg.getSchedule())) {
            holder.textViewSchedule.setVisibility(View.VISIBLE);
            holder.textViewSchedule.setText("🕐 " + lfg.getSchedule());
        } else {
            holder.textViewSchedule.setVisibility(View.GONE);
        }

        int joined = lfg.getPlayers() != null ? lfg.getPlayers().size() : 0;
        int total  = lfg.getSlotsTotal();
        holder.textViewSlots.setText("👥 " + joined + "/" + total + " jugadores");

        boolean isFull    = joined >= total;
        boolean hasJoined = lfg.getPlayers() != null && myId != null && lfg.getPlayers().contains(myId);

        if (isFull && !hasJoined) {
            holder.buttonJoin.setText("PARTY LLENA");
            holder.buttonJoin.setEnabled(false);
            holder.buttonJoin.setAlpha(0.5f);
        } else if (hasJoined) {
            holder.buttonJoin.setText("SALIR DE LA PARTY");
            holder.buttonJoin.setEnabled(true);
            holder.buttonJoin.setAlpha(1f);
        } else {
            holder.buttonJoin.setText("UNIRSE A LA PARTY");
            holder.buttonJoin.setEnabled(true);
            holder.buttonJoin.setAlpha(1f);
        }

        if (lfg.getIdUser() != null) {
            mUsersProvider.getUser(lfg.getIdUser()).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String username = doc.getString("username");
                    holder.textViewAuthor.setText("@" + (username != null ? username : "gamer"));
                }
            });
        }

        loadPlayerNames(lfg.getPlayers(), holder.textViewPlayers);

        holder.buttonJoin.setOnClickListener(v -> {
            if (com.optic.socialmediagamer.utils.GuestGuard.check(context)) return;
            if (hasJoined) {
                mLFGProvider.leaveParty(lfgId, myId).addOnSuccessListener(u -> {
                    Toast.makeText(context, "Saliste de la party", Toast.LENGTH_SHORT).show();
                    showRatingDialog(lfg.getIdUser(), lfg.getGame(), myId);
                });
            } else if (!isFull) {
                mLFGProvider.joinParty(lfgId, myId).addOnSuccessListener(u -> {
                    Toast.makeText(context, "¡Entraste a la party! 🎮", Toast.LENGTH_SHORT).show();
                    new MissionsProvider().incrementProgress(myId, MissionsProvider.TYPE_LFG);
                });
            }
        });

        holder.buttonGroupChat.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra("groupId", "lfg_" + lfgId);
            intent.putExtra("groupTitle", "💬 " + lfg.getGame());
            context.startActivity(intent);
        });
    }

    private void loadPlayerNames(List<String> playerIds, TextView target) {
        if (playerIds == null || playerIds.isEmpty()) {
            target.setVisibility(View.GONE);
            return;
        }
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : playerIds) tasks.add(mUsersProvider.getUser(uid));
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            List<String> names = new ArrayList<>();
            for (Object obj : results) {
                DocumentSnapshot doc = (DocumentSnapshot) obj;
                if (doc.exists()) {
                    String u = doc.getString("username");
                    names.add("@" + (u != null ? u : "gamer"));
                }
            }
            if (names.isEmpty()) {
                target.setVisibility(View.GONE);
            } else {
                target.setVisibility(View.VISIBLE);
                target.setText("🎮 " + String.join("  ·  ", names));
            }
        });
    }

    private void showRatingDialog(String creatorId, String game, String myId) {
        if (creatorId == null || creatorId.equals(myId)) return;

        mReputationProvider.hasRated(myId, creatorId, game).addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) return; // ya calificó

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("⭐ Califica tu experiencia en la party de " + game);

            android.view.View dialogView = android.view.LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, null);

            // Use a simple array of labels for rating dialog
            String[] options = {"⭐ Mala (1)", "⭐⭐ Regular (2)", "⭐⭐⭐ Buena (3)", "⭐⭐⭐⭐ Muy buena (4)", "⭐⭐⭐⭐⭐ Excelente (5)"};
            builder.setItems(options, (d, which) -> {
                float score = which + 1f;
                GameRating rating = new GameRating();
                rating.setIdFrom(myId);
                rating.setIdTo(creatorId);
                rating.setGame(game);
                rating.setCommunication(score);
                rating.setSkill(score);
                rating.setAttitude(score);
                rating.setTimestamp(System.currentTimeMillis());
                mReputationProvider.save(rating).addOnSuccessListener(u ->
                        Toast.makeText(context, "¡Gracias por tu valoración! ⭐", Toast.LENGTH_SHORT).show());
            });
            builder.setNegativeButton("Omitir", null);
            builder.show();
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lfg, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewGame, textViewPlatform, textViewDescription;
        TextView textViewSlots, textViewAuthor, textViewPlayers, textViewSchedule;
        Button buttonJoin, buttonGroupChat;

        public ViewHolder(View view) {
            super(view);
            textViewGame        = view.findViewById(R.id.textViewLFGGame);
            textViewPlatform    = view.findViewById(R.id.textViewLFGPlatform);
            textViewDescription = view.findViewById(R.id.textViewLFGDescription);
            textViewSlots       = view.findViewById(R.id.textViewLFGSlots);
            textViewAuthor      = view.findViewById(R.id.textViewLFGAuthor);
            textViewPlayers     = view.findViewById(R.id.textViewLFGPlayers);
            textViewSchedule    = view.findViewById(R.id.textViewLFGSchedule);
            buttonJoin          = view.findViewById(R.id.buttonJoinParty);
            buttonGroupChat     = view.findViewById(R.id.buttonLFGGroupChat);
        }
    }
}
