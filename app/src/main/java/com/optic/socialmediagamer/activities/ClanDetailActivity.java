package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Clan;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ClanProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.FCMSender;

import java.util.List;

public class ClanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CLAN_ID = "clanId";

    Toolbar mToolbar;
    TextView mTextViewName, mTextViewTag, mTextViewDescription, mTextViewMemberCount;
    TextView mTextViewDissolvedBanner;
    Button mButtonJoinLeave, mButtonEditDescription, mButtonDeleteClan, mButtonSaveDescription;
    Button mButtonTransferLeadership, mButtonScouting;
    LinearLayout mLayoutMembers, mLayoutEditDescription, mLayoutPending;
    EditText mEditTextNewDescription;

    ClanProvider mClanProvider;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    NotificationsProvider mNotificationsProvider;

    String mClanId;
    Clan mClan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clan_detail);

        mToolbar = findViewById(R.id.toolbarClanDetail);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mTextViewName            = findViewById(R.id.textViewClanName);
        mTextViewTag             = findViewById(R.id.textViewClanTag);
        mTextViewDescription     = findViewById(R.id.textViewClanDescription);
        mTextViewMemberCount     = findViewById(R.id.textViewMemberCount);
        mTextViewDissolvedBanner = findViewById(R.id.textViewDissolvedBanner);
        mButtonJoinLeave         = findViewById(R.id.buttonJoinLeave);
        mButtonEditDescription   = findViewById(R.id.buttonEditDescription);
        mButtonDeleteClan        = findViewById(R.id.buttonDeleteClan);
        mButtonTransferLeadership = findViewById(R.id.buttonTransferLeadership);
        mButtonScouting           = findViewById(R.id.buttonScouting);
        mLayoutMembers           = findViewById(R.id.layoutMembers);
        mLayoutEditDescription   = findViewById(R.id.layoutEditDescription);
        mLayoutPending           = findViewById(R.id.layoutPending);
        mEditTextNewDescription  = findViewById(R.id.editTextNewDescription);
        mButtonSaveDescription   = findViewById(R.id.buttonSaveDescription);

        mClanProvider          = new ClanProvider();
        mAuthProvider          = new AuthProvider();
        mUsersProvider         = new UsersProvider();
        mNotificationsProvider = new NotificationsProvider();

        mClanId = getIntent().getStringExtra(EXTRA_CLAN_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mClanId != null) loadClan();
    }

    private void loadClan() {
        mClanProvider.get(mClanId).addOnSuccessListener(doc -> {
            if (!doc.exists()) { finish(); return; }
            mClan = doc.toObject(Clan.class);
            if (mClan == null) { finish(); return; }
            mClan.setId(doc.getId());
            renderClan();
        });
    }

    private void renderClan() {
        String myId = mAuthProvider.getUid();
        boolean isLeader = myId != null && myId.equals(mClan.getIdLeader());
        boolean isMember = mClan.getMembers() != null && mClan.getMembers().contains(myId);
        boolean isPending = mClan.getPendingMembers() != null && mClan.getPendingMembers().contains(myId);
        boolean isActive = mClan.isActive();

        mToolbar.setTitle("🛡️ " + mClan.getName());
        mTextViewName.setText(mClan.getName());
        mTextViewTag.setText("[" + mClan.getTag() + "]");
        mTextViewDescription.setText(mClan.getDescription() != null ? mClan.getDescription() : "");
        int memberCount = mClan.getMembers() != null ? mClan.getMembers().size() : 0;
        mTextViewMemberCount.setText(memberCount + " miembro" + (memberCount != 1 ? "s" : ""));

        // Dissolved banner
        if (!isActive) {
            mTextViewDissolvedBanner.setVisibility(View.VISIBLE);
            mTextViewDissolvedBanner.setText("⛔ CLAN DISUELTO");
            // Hide all action buttons for dissolved clan
            mButtonJoinLeave.setVisibility(View.GONE);
            mButtonEditDescription.setVisibility(View.GONE);
            mButtonDeleteClan.setVisibility(View.GONE);
            mButtonTransferLeadership.setVisibility(View.GONE);
            loadMembers(false);
            return;
        }
        mTextViewDissolvedBanner.setVisibility(View.GONE);

        // Join / Leave / Pending state
        if (!isLeader) {
            mButtonJoinLeave.setVisibility(View.VISIBLE);
            if (isMember) {
                mButtonJoinLeave.setText("SALIR DEL CLAN");
                mButtonJoinLeave.setBackgroundTintList(getColorStateList(R.color.color_text_secondary));
                mButtonJoinLeave.setOnClickListener(v -> leaveClan());
            } else if (isPending) {
                mButtonJoinLeave.setText("⏳ SOLICITUD ENVIADA");
                mButtonJoinLeave.setEnabled(false);
                mButtonJoinLeave.setBackgroundTintList(getColorStateList(R.color.color_surface_2));
            } else {
                mButtonJoinLeave.setText("SOLICITAR UNIRSE");
                mButtonJoinLeave.setEnabled(true);
                mButtonJoinLeave.setBackgroundTintList(getColorStateList(R.color.color_primary));
                mButtonJoinLeave.setOnClickListener(v -> requestJoinClan());
            }
        } else {
            mButtonJoinLeave.setVisibility(View.GONE);
        }

        // Leader-only buttons
        mButtonEditDescription.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        mButtonDeleteClan.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        mButtonTransferLeadership.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        mButtonScouting.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        mButtonScouting.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ScoutingActivity.class);
            intent.putExtra(ScoutingActivity.EXTRA_CLAN_ID, mClanId);
            intent.putExtra(ScoutingActivity.EXTRA_CLAN_NAME, mClan.getName());
            startActivity(intent);
        });

        mButtonEditDescription.setOnClickListener(v -> {
            mEditTextNewDescription.setText(mClan.getDescription());
            mLayoutEditDescription.setVisibility(
                    mLayoutEditDescription.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        mButtonSaveDescription.setOnClickListener(v -> {
            String newDesc = mEditTextNewDescription.getText().toString().trim();
            mClanProvider.updateInfo(mClanId, newDesc).addOnSuccessListener(u -> {
                mLayoutEditDescription.setVisibility(View.GONE);
                Toast.makeText(this, "Descripción actualizada", Toast.LENGTH_SHORT).show();
                loadClan();
            });
        });

        mButtonDeleteClan.setOnClickListener(v -> confirmDissolveClan());
        mButtonTransferLeadership.setOnClickListener(v -> showTransferLeadershipDialog());

        // Pending join requests (leader only)
        loadPendingRequests(isLeader);
        loadMembers(isLeader);
    }

    private void requestJoinClan() {
        if (com.optic.socialmediagamer.utils.GuestGuard.check(this)) return;
        String myId = mAuthProvider.getUid();
        mClanProvider.requestJoin(mClanId, myId).addOnSuccessListener(u -> {
            Toast.makeText(this, "Solicitud enviada al líder ⏳", Toast.LENGTH_SHORT).show();
            // Notify leader
            mUsersProvider.getUser(myId).addOnSuccessListener(meDoc -> {
                String myUsername = meDoc.getString("username") != null ? meDoc.getString("username") : "?";
                sendClanNotification(mClan.getIdLeader(),
                        "Solicitud de ingreso a " + mClan.getName(),
                        "@" + myUsername + " quiere unirse al clan");
            });
            loadClan();
        });
    }

    private void leaveClan() {
        String myId = mAuthProvider.getUid();
        mUsersProvider.getUser(myId).addOnSuccessListener(meDoc -> {
            String myUsername = meDoc.getString("username") != null ? meDoc.getString("username") : "?";
            mClanProvider.leave(mClanId, myId).addOnSuccessListener(u -> {
                sendClanNotification(mClan.getIdLeader(),
                        "Miembro salió del clan",
                        "@" + myUsername + " salió de " + mClan.getName());
                Toast.makeText(this, "Saliste del clan", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void loadPendingRequests(boolean isLeader) {
        mLayoutPending.removeAllViews();
        if (!isLeader) { mLayoutPending.setVisibility(View.GONE); return; }

        List<String> pending = mClan.getPendingMembers();
        if (pending == null || pending.isEmpty()) {
            mLayoutPending.setVisibility(View.GONE);
            return;
        }

        mLayoutPending.setVisibility(View.VISIBLE);
        TextView header = new TextView(this);
        header.setText("📥 SOLICITUDES DE INGRESO");
        header.setTextColor(getColor(R.color.color_primary));
        header.setTextSize(11);
        header.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, 8);
        mLayoutPending.addView(header);

        for (String uid : pending) {
            mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
                String username = doc.exists() && doc.getString("username") != null
                        ? doc.getString("username") : uid;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);

                TextView tvName = new TextView(this);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvName.setText("@" + username);
                tvName.setTextColor(getColor(R.color.color_text_primary));
                tvName.setTextSize(13);
                row.addView(tvName);

                TextView tvApprove = new TextView(this);
                tvApprove.setText("✅ ACEPTAR");
                tvApprove.setTextColor(getColor(R.color.color_primary));
                tvApprove.setTextSize(11);
                tvApprove.setPadding(8, 0, 8, 0);
                tvApprove.setOnClickListener(v -> {
                    mClanProvider.approveJoin(mClanId, uid).addOnSuccessListener(u -> {
                        sendClanNotification(uid, "Solicitud aceptada",
                                "¡Bienvenido al clan " + mClan.getName() + "! 🛡️");
                        Toast.makeText(this, "@" + username + " aceptado", Toast.LENGTH_SHORT).show();
                        loadClan();
                    });
                });
                row.addView(tvApprove);

                TextView tvReject = new TextView(this);
                tvReject.setText("❌");
                tvReject.setTextColor(getColor(R.color.color_text_secondary));
                tvReject.setTextSize(13);
                tvReject.setPadding(4, 0, 0, 0);
                tvReject.setOnClickListener(v -> {
                    mClanProvider.rejectJoin(mClanId, uid).addOnSuccessListener(u -> {
                        sendClanNotification(uid, "Solicitud rechazada",
                                "Tu solicitud para unirte a " + mClan.getName() + " fue rechazada.");
                        Toast.makeText(this, "@" + username + " rechazado", Toast.LENGTH_SHORT).show();
                        loadClan();
                    });
                });
                row.addView(tvReject);

                mLayoutPending.addView(row);
            });
        }

        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(getColor(R.color.color_divider));
        mLayoutPending.addView(divider);
    }

    private void loadMembers(boolean isLeader) {
        mLayoutMembers.removeAllViews();
        List<String> members = mClan.getMembers();
        if (members == null || members.isEmpty()) return;

        List<String> officers = mClan.getOfficers() != null ? mClan.getOfficers() : new java.util.ArrayList<>();
        String myId = mAuthProvider.getUid();
        boolean isOfficer = officers.contains(myId);

        for (String uid : members) {
            mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
                String username = doc.exists() && doc.getString("username") != null
                        ? doc.getString("username") : uid;
                boolean isThisLeader  = uid.equals(mClan.getIdLeader());
                boolean isThisOfficer = officers.contains(uid);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(8, 12, 8, 12);

                TextView tvName = new TextView(this);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                String prefix = isThisLeader ? "👑 " : (isThisOfficer ? "🎖️ " : "• ");
                tvName.setText(prefix + username);
                tvName.setTextColor(getColor(R.color.color_text_primary));
                tvName.setTextSize(14);
                row.addView(tvName);

                if (isLeader && !isThisLeader) {
                    if (!isThisOfficer) {
                        TextView tvPromote = new TextView(this);
                        tvPromote.setText("PROMOVER");
                        tvPromote.setTextColor(getColor(R.color.color_primary));
                        tvPromote.setTextSize(11);
                        tvPromote.setPadding(8, 0, 8, 0);
                        tvPromote.setOnClickListener(v -> confirmPromote(uid, username));
                        row.addView(tvPromote);
                    } else {
                        TextView tvDemote = new TextView(this);
                        tvDemote.setText("DEMOVER");
                        tvDemote.setTextColor(getColor(android.R.color.holo_orange_light));
                        tvDemote.setTextSize(11);
                        tvDemote.setPadding(8, 0, 8, 0);
                        tvDemote.setOnClickListener(v ->
                            mClanProvider.demoteToMember(mClanId, uid).addOnSuccessListener(u -> {
                                sendClanNotification(uid, "Rol actualizado en " + mClan.getName(),
                                        "Tu rol cambió a Miembro en el clan " + mClan.getName());
                                Toast.makeText(this, username + " ya no es oficial", Toast.LENGTH_SHORT).show();
                                loadClan();
                            }));
                        row.addView(tvDemote);
                    }

                    TextView tvKick = new TextView(this);
                    tvKick.setText("EXPULSAR");
                    tvKick.setTextColor(getColor(R.color.color_primary));
                    tvKick.setTextSize(11);
                    tvKick.setPadding(4, 0, 0, 0);
                    tvKick.setOnClickListener(v -> confirmKick(uid, username));
                    row.addView(tvKick);
                }

                if (isOfficer && !isLeader && !isThisLeader && !isThisOfficer && !uid.equals(myId)) {
                    TextView tvKick = new TextView(this);
                    tvKick.setText("EXPULSAR");
                    tvKick.setTextColor(getColor(R.color.color_primary));
                    tvKick.setTextSize(11);
                    tvKick.setPadding(8, 0, 0, 0);
                    tvKick.setOnClickListener(v -> confirmKick(uid, username));
                    row.addView(tvKick);
                }

                mLayoutMembers.addView(row);

                View divider = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(getColor(R.color.color_divider));
                mLayoutMembers.addView(divider);
            });
        }
    }

    private void confirmPromote(String uid, String username) {
        new AlertDialog.Builder(this)
                .setTitle("Promover a Oficial")
                .setMessage("¿Promover a " + username + " como 🎖️ Oficial del clan?")
                .setPositiveButton("PROMOVER", (d, w) ->
                        mClanProvider.promoteToOfficer(mClanId, uid).addOnSuccessListener(u -> {
                            sendClanNotification(uid, "¡Ascendido a Oficial! 🎖️",
                                    "Eres ahora Oficial del clan " + mClan.getName());
                            Toast.makeText(this, username + " es ahora Oficial 🎖️", Toast.LENGTH_SHORT).show();
                            loadClan();
                        }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void confirmKick(String uid, String username) {
        new AlertDialog.Builder(this)
                .setTitle("Expulsar miembro")
                .setMessage("¿Expulsar a " + username + " del clan?")
                .setPositiveButton("EXPULSAR", (d, w) ->
                        mClanProvider.kick(mClanId, uid).addOnSuccessListener(u -> {
                            sendClanNotification(uid, "Expulsado del clan",
                                    "Fuiste expulsado del clan " + mClan.getName());
                            Toast.makeText(this, username + " expulsado", Toast.LENGTH_SHORT).show();
                            loadClan();
                        }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void confirmDissolveClan() {
        new AlertDialog.Builder(this)
                .setTitle("Disolver clan")
                .setMessage("El clan quedará archivado pero visible como historial. ¿Disolver " + mClan.getName() + "?")
                .setPositiveButton("DISOLVER", (d, w) ->
                        mClanProvider.dissolve(mClanId).addOnSuccessListener(u -> {
                            // Notify all members
                            if (mClan.getMembers() != null) {
                                String myId = mAuthProvider.getUid();
                                for (String uid : mClan.getMembers()) {
                                    if (!uid.equals(myId)) {
                                        sendClanNotification(uid, "Clan disuelto",
                                                "El clan " + mClan.getName() + " fue disuelto por el líder.");
                                    }
                                }
                            }
                            Toast.makeText(this, "Clan disuelto y archivado", Toast.LENGTH_SHORT).show();
                            loadClan();
                        }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void showTransferLeadershipDialog() {
        List<String> members = mClan.getMembers();
        String myId = mAuthProvider.getUid();
        if (members == null || members.size() <= 1) {
            Toast.makeText(this, "No hay otros miembros a quién transferir el liderazgo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build list of non-leader members
        List<String> candidates = new java.util.ArrayList<>(members);
        candidates.remove(myId);

        String[] candidateIds = candidates.toArray(new String[0]);
        // We need to load usernames for the dialog
        final String[] usernames = new String[candidateIds.length];
        final int[] loaded = {0};

        for (int i = 0; i < candidateIds.length; i++) {
            final int idx = i;
            mUsersProvider.getUser(candidateIds[idx]).addOnSuccessListener(doc -> {
                usernames[idx] = doc.exists() && doc.getString("username") != null
                        ? "@" + doc.getString("username") : candidateIds[idx];
                loaded[0]++;
                if (loaded[0] == candidateIds.length) {
                    new AlertDialog.Builder(this)
                            .setTitle("Transferir liderazgo")
                            .setItems(usernames, (d, which) -> {
                                String newLeaderId = candidateIds[which];
                                String newLeaderName = usernames[which];
                                new AlertDialog.Builder(this)
                                        .setTitle("Confirmar transferencia")
                                        .setMessage("¿Ceder el liderazgo a " + newLeaderName + "? Pasarás a ser miembro.")
                                        .setPositiveButton("CONFIRMAR", (d2, w2) ->
                                                mClanProvider.transferLeadership(mClanId, newLeaderId).addOnSuccessListener(u -> {
                                                    sendClanNotification(newLeaderId, "¡Nuevo líder del clan! 👑",
                                                            "Eres ahora el líder de " + mClan.getName());
                                                    Toast.makeText(this, "Liderazgo transferido a " + newLeaderName, Toast.LENGTH_SHORT).show();
                                                    loadClan();
                                                }))
                                        .setNegativeButton("Cancelar", null).show();
                            })
                            .setNegativeButton("Cancelar", null).show();
                }
            });
        }
    }

    private void sendClanNotification(String toUserId, String title, String body) {
        // In-app notification
        Notification notif = new Notification();
        notif.setIdTo(toUserId);
        notif.setIdFrom(mAuthProvider.getUid() != null ? mAuthProvider.getUid() : "system");
        notif.setTitle(title);
        notif.setBody(body);
        notif.setRead(false);
        notif.setTimestamp(System.currentTimeMillis());
        mNotificationsProvider.save(notif);

        // Push notification
        mUsersProvider.getUser(toUserId).addOnSuccessListener(doc -> {
            String token = doc.getString("fcmToken");
            if (token != null && !token.isEmpty()) {
                FCMSender.send(this, token, title, body);
            }
        });
    }
}
