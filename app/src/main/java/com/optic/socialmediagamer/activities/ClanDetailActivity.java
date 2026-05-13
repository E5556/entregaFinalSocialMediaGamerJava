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

import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Clan;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ClanProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.List;

public class ClanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CLAN_ID = "clanId";

    Toolbar mToolbar;
    TextView mTextViewName, mTextViewTag, mTextViewDescription, mTextViewMemberCount;
    Button mButtonJoinLeave, mButtonEditDescription, mButtonDeleteClan, mButtonSaveDescription;
    LinearLayout mLayoutMembers, mLayoutEditDescription;
    EditText mEditTextNewDescription;

    ClanProvider mClanProvider;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;

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

        mTextViewName        = findViewById(R.id.textViewClanName);
        mTextViewTag         = findViewById(R.id.textViewClanTag);
        mTextViewDescription = findViewById(R.id.textViewClanDescription);
        mTextViewMemberCount = findViewById(R.id.textViewMemberCount);
        mButtonJoinLeave     = findViewById(R.id.buttonJoinLeave);
        mButtonEditDescription = findViewById(R.id.buttonEditDescription);
        mButtonDeleteClan    = findViewById(R.id.buttonDeleteClan);
        mLayoutMembers       = findViewById(R.id.layoutMembers);
        mLayoutEditDescription = findViewById(R.id.layoutEditDescription);
        mEditTextNewDescription = findViewById(R.id.editTextNewDescription);
        mButtonSaveDescription  = findViewById(R.id.buttonSaveDescription);

        mClanProvider  = new ClanProvider();
        mAuthProvider  = new AuthProvider();
        mUsersProvider = new UsersProvider();

        mClanId = getIntent().getStringExtra(EXTRA_CLAN_ID);
        loadClan();
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
            renderClan();
        });
    }

    private void renderClan() {
        String myId = mAuthProvider.getUid();
        boolean isLeader = myId.equals(mClan.getIdLeader());
        boolean isMember = mClan.getMembers() != null && mClan.getMembers().contains(myId);

        mToolbar.setTitle("🛡️ " + mClan.getName());
        mTextViewName.setText(mClan.getName());
        mTextViewTag.setText("[" + mClan.getTag() + "]");
        mTextViewDescription.setText(mClan.getDescription() != null ? mClan.getDescription() : "");
        int memberCount = mClan.getMembers() != null ? mClan.getMembers().size() : 0;
        mTextViewMemberCount.setText(memberCount + " miembro" + (memberCount != 1 ? "s" : ""));

        // Join/Leave button (non-leaders only)
        if (!isLeader) {
            mButtonJoinLeave.setVisibility(View.VISIBLE);
            if (isMember) {
                mButtonJoinLeave.setText("SALIR DEL CLAN");
                mButtonJoinLeave.setBackgroundTintList(
                        getColorStateList(R.color.color_text_secondary));
                mButtonJoinLeave.setOnClickListener(v -> leaveClan());
            } else {
                mButtonJoinLeave.setText("UNIRSE AL CLAN");
                mButtonJoinLeave.setBackgroundTintList(
                        getColorStateList(R.color.color_primary));
                mButtonJoinLeave.setOnClickListener(v -> joinClan());
            }
        } else {
            mButtonJoinLeave.setVisibility(View.GONE);
        }

        // Leader-only buttons
        mButtonEditDescription.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        mButtonDeleteClan.setVisibility(isLeader ? View.VISIBLE : View.GONE);

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

        mButtonDeleteClan.setOnClickListener(v -> confirmDeleteClan());

        loadMembers(isLeader);
    }

    private void joinClan() {
        mClanProvider.join(mClanId, mAuthProvider.getUid())
                .addOnSuccessListener(u -> { Toast.makeText(this, "Te uniste al clan", Toast.LENGTH_SHORT).show(); loadClan(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show());
    }

    private void leaveClan() {
        mClanProvider.leave(mClanId, mAuthProvider.getUid())
                .addOnSuccessListener(u -> { Toast.makeText(this, "Saliste del clan", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteClan() {
        new AlertDialog.Builder(this)
                .setTitle("Disolver clan")
                .setMessage("Esta acción es irreversible. ¿Disolver " + mClan.getName() + "?")
                .setPositiveButton("DISOLVER", (d, w) ->
                        mClanProvider.delete(mClanId).addOnSuccessListener(u -> {
                            Toast.makeText(this, "Clan disuelto", Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                .setNegativeButton("Cancelar", null)
                .show();
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

                // Leader: can promote members to officer, demote officers, kick non-leaders
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
                        tvDemote.setOnClickListener(v -> mClanProvider.demoteToMember(mClanId, uid)
                                .addOnSuccessListener(u -> { Toast.makeText(this, username + " ya no es oficial", Toast.LENGTH_SHORT).show(); loadClan(); }));
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

                // Officer: can kick regular members (not the leader or other officers)
                if (isOfficer && !isThisLeader && !isThisOfficer && !uid.equals(myId)) {
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
                            Toast.makeText(this, username + " es ahora Oficial 🎖️", Toast.LENGTH_SHORT).show();
                            loadClan();
                        }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmKick(String uid, String username) {
        new AlertDialog.Builder(this)
                .setTitle("Expulsar miembro")
                .setMessage("¿Expulsar a " + username + " del clan?")
                .setPositiveButton("EXPULSAR", (d, w) ->
                        mClanProvider.kick(mClanId, uid).addOnSuccessListener(u -> {
                            Toast.makeText(this, username + " expulsado", Toast.LENGTH_SHORT).show();
                            loadClan();
                        }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
