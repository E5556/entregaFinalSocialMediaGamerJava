package com.optic.socialmediagamer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Clan;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ClanProvider;

import java.util.List;

public class ClansActivity extends AppCompatActivity {

    LinearLayout mLayoutClans;
    TextView mTextViewEmpty;
    FloatingActionButton mFab;
    ClanProvider mClanProvider;
    AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clans);

        Toolbar toolbar = findViewById(R.id.toolbarClans);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mLayoutClans   = findViewById(R.id.layoutClansList);
        mTextViewEmpty = findViewById(R.id.textViewClansEmpty);
        mFab           = findViewById(R.id.fabCreateClan);

        mClanProvider = new ClanProvider();
        mAuthProvider = new AuthProvider();

        mFab.setOnClickListener(v -> startActivity(new Intent(this, CreateClanActivity.class)));
        loadClans();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClans();
    }

    private void loadClans() {
        mLayoutClans.removeAllViews();
        mClanProvider.getAll().get().addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            if (docs.isEmpty()) {
                mTextViewEmpty.setVisibility(View.VISIBLE);
                return;
            }
            mTextViewEmpty.setVisibility(View.GONE);
            for (DocumentSnapshot doc : docs) {
                Clan clan = doc.toObject(Clan.class);
                if (clan == null) continue;
                addClanRow(clan);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error cargando clanes", Toast.LENGTH_SHORT).show());
    }

    private void addClanRow(Clan clan) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackground(getDrawable(android.R.drawable.list_selector_background));
        row.setClickable(true);
        row.setFocusable(true);

        TextView tvName = new TextView(this);
        tvName.setText("🛡️ " + clan.getName() + "  [" + clan.getTag() + "]");
        tvName.setTextColor(getColor(R.color.color_text_primary));
        tvName.setTextSize(15);
        tvName.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(tvName);

        int memberCount = clan.getMembers() != null ? clan.getMembers().size() : 0;
        TextView tvInfo = new TextView(this);
        tvInfo.setText(memberCount + " miembro" + (memberCount != 1 ? "s" : "")
                + (clan.getDescription() != null && !clan.getDescription().isEmpty()
                   ? "  ·  " + clan.getDescription() : ""));
        tvInfo.setTextColor(getColor(R.color.color_text_secondary));
        tvInfo.setTextSize(12);
        tvInfo.setMaxLines(1);
        tvInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(tvInfo);

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClanDetailActivity.class);
            intent.putExtra(ClanDetailActivity.EXTRA_CLAN_ID, clan.getId());
            startActivity(intent);
        });

        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 4, 0, 0);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(getColor(R.color.color_divider));

        mLayoutClans.addView(row);
        mLayoutClans.addView(divider);
    }
}
