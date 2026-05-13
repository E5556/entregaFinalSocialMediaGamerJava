package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.ClanDetailActivity;
import com.optic.socialmediagamer.models.Clan;
import com.optic.socialmediagamer.providers.ClanProvider;

import java.util.List;

public class ClanRankingFragment extends Fragment {

    private LinearLayout mLayoutRanking;
    private final ClanProvider mClanProvider = new ClanProvider();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clan_ranking, container, false);
        mLayoutRanking = view.findViewById(R.id.layoutClanRanking);
        loadRanking();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRanking();
    }

    private void loadRanking() {
        mLayoutRanking.removeAllViews();
        mClanProvider.getRanking().get().addOnSuccessListener(snap -> {
            List<DocumentSnapshot> docs = snap.getDocuments();
            if (docs.isEmpty()) {
                TextView tv = new TextView(getContext());
                tv.setText("Aún no hay clanes con XP acumulado.");
                tv.setTextColor(requireContext().getColor(R.color.color_text_secondary));
                tv.setPadding(24, 24, 24, 24);
                mLayoutRanking.addView(tv);
                return;
            }
            for (int i = 0; i < docs.size(); i++) {
                Clan clan = docs.get(i).toObject(Clan.class);
                if (clan == null) continue;
                addClanRow(i + 1, clan);
            }
        });
    }

    private void addClanRow(int position, Clan clan) {
        String medal = position == 1 ? "🥇" : position == 2 ? "🥈" : position == 3 ? "🥉" : "  " + position + ".";
        int memberCount = clan.getMembers() != null ? clan.getMembers().size() : 0;

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 14, 16, 14);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(requireContext().getDrawable(android.R.drawable.list_selector_background));

        TextView tvPos = new TextView(getContext());
        tvPos.setText(medal + " ");
        tvPos.setTextSize(18);
        tvPos.setMinWidth(72);
        row.addView(tvPos);

        LinearLayout info = new LinearLayout(getContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(getContext());
        tvName.setText("🛡️ " + clan.getName() + "  [" + clan.getTag() + "]");
        tvName.setTextColor(requireContext().getColor(R.color.color_text_primary));
        tvName.setTextSize(14);
        tvName.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        info.addView(tvName);

        TextView tvSub = new TextView(getContext());
        tvSub.setText(memberCount + " miembros");
        tvSub.setTextColor(requireContext().getColor(R.color.color_text_secondary));
        tvSub.setTextSize(12);
        info.addView(tvSub);

        row.addView(info);

        TextView tvXp = new TextView(getContext());
        tvXp.setText(clan.getClanXp() + " XP");
        tvXp.setTextColor(requireContext().getColor(R.color.color_primary));
        tvXp.setTextSize(14);
        tvXp.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(tvXp);

        row.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ClanDetailActivity.class);
            intent.putExtra(ClanDetailActivity.EXTRA_CLAN_ID, clan.getId());
            startActivity(intent);
        });

        mLayoutRanking.addView(row);

        View divider = new View(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(requireContext().getColor(R.color.color_divider));
        mLayoutRanking.addView(divider);
    }
}
