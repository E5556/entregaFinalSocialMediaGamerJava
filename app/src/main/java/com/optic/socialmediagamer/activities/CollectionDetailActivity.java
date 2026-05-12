package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.providers.CollectionsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CollectionDetailActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private SavedPostsAdapter mAdapter;
    private final List<DocumentSnapshot> mPostDocs = new ArrayList<>();

    private CollectionsProvider mCollectionsProvider;
    private PostProvider mPostProvider;
    private String mCollectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_detail);

        mCollectionId = getIntent().getStringExtra("collectionId");
        String name = getIntent().getStringExtra("collectionName");

        TextView textTitle = findViewById(R.id.textViewCollectionDetailTitle);
        if (textTitle != null && name != null) textTitle.setText("📂 " + name);

        mRecyclerView = findViewById(R.id.recyclerViewSavedPosts);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mCollectionsProvider = new CollectionsProvider();
        mPostProvider = new PostProvider();

        mAdapter = new SavedPostsAdapter(mPostDocs, (doc, position) ->
            new AlertDialog.Builder(this)
                .setTitle("Eliminar de colección")
                .setMessage("¿Quitar esta publicación de la colección?")
                .setPositiveButton("Quitar", (d, w) -> {
                    mCollectionsProvider.removePost(mCollectionId, doc.getId())
                        .addOnSuccessListener(v -> {
                            mPostDocs.remove(position);
                            mAdapter.notifyItemRemoved(position);
                        });
                })
                .setNegativeButton("Cancelar", null)
                .show()
        );
        mRecyclerView.setAdapter(mAdapter);

        loadPosts();
    }

    private void loadPosts() {
        mCollectionsProvider.get(mCollectionId).addOnSuccessListener(colDoc -> {
            if (!colDoc.exists()) return;
            List<String> postIds = (List<String>) colDoc.get("postIds");
            if (postIds == null || postIds.isEmpty()) return;

            mPostDocs.clear();
            for (String postId : postIds) {
                mPostProvider.getById(postId).get().addOnSuccessListener(postDoc -> {
                    if (postDoc.exists()) {
                        mPostDocs.add(postDoc);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    interface OnRemoveClick { void onClick(DocumentSnapshot doc, int position); }

    static class SavedPostsAdapter extends RecyclerView.Adapter<SavedPostsAdapter.VH> {

        private final List<DocumentSnapshot> mList;
        private final OnRemoveClick mListener;

        SavedPostsAdapter(List<DocumentSnapshot> list, OnRemoveClick listener) {
            mList = list;
            mListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DocumentSnapshot doc = mList.get(position);
            Post post = doc.toObject(Post.class);
            if (post == null) return;

            h.textTitle.setText(post.getTitle() != null ? post.getTitle() : "Sin título");
            h.textDescription.setText(post.getDescription() != null ? post.getDescription() : "");

            if (post.getImage1() != null && !post.getImage1().isEmpty()) {
                Picasso.get().load(post.getImage1())
                        .placeholder(R.drawable.ic_videogame)
                        .into(h.imagePost);
            }

            h.btnRemove.setOnClickListener(v -> mListener.onClick(doc, h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return mList.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView textTitle, textDescription, btnRemove;
            android.widget.ImageView imagePost;

            VH(View v) {
                super(v);
                textTitle       = v.findViewById(R.id.textViewSavedPostTitle);
                textDescription = v.findViewById(R.id.textViewSavedPostDescription);
                imagePost       = v.findViewById(R.id.imageViewSavedPost);
                btnRemove       = v.findViewById(R.id.btnRemoveSavedPost);
            }
        }
    }
}
