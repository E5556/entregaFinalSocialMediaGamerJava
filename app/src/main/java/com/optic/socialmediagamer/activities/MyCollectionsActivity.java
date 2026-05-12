package com.optic.socialmediagamer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Collection;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.CollectionsProvider;

import java.util.ArrayList;
import java.util.List;

public class MyCollectionsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private CollectionsAdapter mAdapter;
    private final List<Collection> mCollections = new ArrayList<>();

    private final CollectionsProvider mCollectionsProvider = new CollectionsProvider();
    private final AuthProvider mAuthProvider = new AuthProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_collections);

        mRecyclerView = findViewById(R.id.recyclerViewCollections);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new CollectionsAdapter(mCollections, col -> {
            Intent intent = new Intent(this, CollectionDetailActivity.class);
            intent.putExtra("collectionId", col.getId());
            intent.putExtra("collectionName", col.getName());
            startActivity(intent);
        });
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = findViewById(R.id.fabNewCollection);
        fab.setOnClickListener(v -> showCreateDialog());

        loadCollections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCollections();
    }

    private void loadCollections() {
        String uid = mAuthProvider.getUid();
        if (uid == null) return;
        mCollectionsProvider.getByUser(uid).get().addOnSuccessListener(snap -> {
            mCollections.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Collection col = doc.toObject(Collection.class);
                if (col != null) { col.setId(doc.getId()); mCollections.add(col); }
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    private void showCreateDialog() {
        EditText input = new EditText(this);
        input.setHint("Nombre de la colección");
        new AlertDialog.Builder(this)
                .setTitle("Nueva colección")
                .setView(input)
                .setPositiveButton("Crear", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show(); return; }
                    mCollectionsProvider.create(mAuthProvider.getUid(), name)
                            .addOnSuccessListener(v -> loadCollections());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    interface OnCollectionClick { void onClick(Collection col); }

    static class CollectionsAdapter extends RecyclerView.Adapter<CollectionsAdapter.VH> {

        private final List<Collection> mList;
        private final OnCollectionClick mListener;

        CollectionsAdapter(List<Collection> list, OnCollectionClick listener) {
            mList = list;
            mListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collection, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Collection col = mList.get(position);
            h.textName.setText(col.getName());
            int count = col.getPostIds() != null ? col.getPostIds().size() : 0;
            h.textCount.setText(count + (count == 1 ? " publicación" : " publicaciones"));
            h.itemView.setOnClickListener(v -> mListener.onClick(col));
        }

        @Override
        public int getItemCount() { return mList.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView textName, textCount;
            VH(View v) {
                super(v);
                textName  = v.findViewById(R.id.textViewCollectionName);
                textCount = v.findViewById(R.id.textViewCollectionCount);
            }
        }
    }
}
