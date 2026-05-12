package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.CreateLFGActivity;
import com.optic.socialmediagamer.activities.MatchPlayActivity;
import com.optic.socialmediagamer.adapters.LFGListAdapter;
import com.optic.socialmediagamer.models.LFGPost;
import com.optic.socialmediagamer.providers.LFGProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LFGFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private LFGListAdapter mAdapter;
    private final LFGProvider mLFGProvider = new LFGProvider();
    private ListenerRegistration mListener;

    private final List<LFGPost> mAllPosts      = new ArrayList<>();
    private final List<LFGPost> mFilteredPosts = new ArrayList<>();

    private String mActivePlatform = null; // null = Todos
    private String mSearchQuery    = "";

    private TextView mChipAll, mChipPC, mChipPS4, mChipXBOX, mChipNintendo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lfg, container, false);

        mRecyclerView = view.findViewById(R.id.recyclerViewLFG);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LFGListAdapter(mFilteredPosts, getContext());
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = view.findViewById(R.id.fabCreateLFG);
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), CreateLFGActivity.class)));

        FloatingActionButton fabMatch = view.findViewById(R.id.fabMatchPlay);
        fabMatch.setOnClickListener(v -> startActivity(new Intent(getActivity(), MatchPlayActivity.class)));

        // Chips
        mChipAll      = view.findViewById(R.id.chipAll);
        mChipPC       = view.findViewById(R.id.chipPC);
        mChipPS4      = view.findViewById(R.id.chipPS4);
        mChipXBOX     = view.findViewById(R.id.chipXBOX);
        mChipNintendo = view.findViewById(R.id.chipNintendo);

        setChipActive(mChipAll);
        mChipAll.setOnClickListener(v      -> selectPlatform(null,       mChipAll));
        mChipPC.setOnClickListener(v       -> selectPlatform("PC",       mChipPC));
        mChipPS4.setOnClickListener(v      -> selectPlatform("PS4",      mChipPS4));
        mChipXBOX.setOnClickListener(v     -> selectPlatform("XBOX",     mChipXBOX));
        mChipNintendo.setOnClickListener(v -> selectPlatform("Nintendo", mChipNintendo));

        // Búsqueda
        EditText search = view.findViewById(R.id.editTextSearchLFG);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                mSearchQuery = s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void selectPlatform(String platform, TextView chip) {
        mActivePlatform = platform;
        setChipInactive(mChipAll);
        setChipInactive(mChipPC);
        setChipInactive(mChipPS4);
        setChipInactive(mChipXBOX);
        setChipInactive(mChipNintendo);
        setChipActive(chip);
        applyFilters();
    }

    private void setChipActive(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_background));
    }

    private void setChipInactive(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_surface_2));
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary));
    }

    private void applyFilters() {
        mFilteredPosts.clear();
        String query = mSearchQuery.toLowerCase();
        for (LFGPost post : mAllPosts) {
            if (mActivePlatform != null && !mActivePlatform.equalsIgnoreCase(post.getPlatform())) continue;
            if (!query.isEmpty()) {
                boolean matchGame = post.getGame() != null && post.getGame().toLowerCase().contains(query);
                boolean matchDesc = post.getDescription() != null && post.getDescription().toLowerCase().contains(query);
                if (!matchGame && !matchDesc) continue;
            }
            mFilteredPosts.add(post);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    private void startListening() {
        if (mListener != null) { mListener.remove(); mListener = null; }
        mAllPosts.clear();
        mFilteredPosts.clear();
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        mListener = mLFGProvider.getAll().addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                String docId = dc.getDocument().getId();
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    LFGPost post = dc.getDocument().toObject(LFGPost.class);
                    post.setId(docId);
                    boolean exists = false;
                    for (LFGPost p : mAllPosts) { if (docId.equals(p.getId())) { exists = true; break; } }
                    if (!exists) mAllPosts.add(post);
                } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < mAllPosts.size(); i++) {
                        if (docId.equals(mAllPosts.get(i).getId())) {
                            LFGPost updated = dc.getDocument().toObject(LFGPost.class);
                            updated.setId(docId);
                            mAllPosts.set(i, updated);
                            break;
                        }
                    }
                } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                    mAllPosts.removeIf(p -> docId.equals(p.getId()));
                }
            }
            Collections.sort(mAllPosts, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            applyFilters();
        });
    }

    private void stopListening() {
        if (mListener != null) { mListener.remove(); mListener = null; }
    }
}
