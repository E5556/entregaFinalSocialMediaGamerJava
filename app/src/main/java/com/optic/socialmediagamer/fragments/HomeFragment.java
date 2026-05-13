package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.CreateStoryActivity;
import com.optic.socialmediagamer.activities.MainActivity;
import com.optic.socialmediagamer.activities.PostActivity;
import com.optic.socialmediagamer.activities.SearchPostsActivity;
import com.optic.socialmediagamer.adapters.PostsAdapter;
import com.optic.socialmediagamer.adapters.StoriesAdapter;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.models.Story;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.StoriesProvider;

public class HomeFragment extends Fragment {

    View mview;
    FloatingActionButton mFab;
    Toolbar mToolbar;
    AuthProvider mAuthProvider;
    RecyclerView mRecyclerView;
    RecyclerView mRecyclerViewStories;
    PostProvider mPostProvider;
    StoriesProvider mStoriesProvider;
    PostsAdapter mPostsAdapter;
    StoriesAdapter mStoriesAdapter;

    private final Handler mStoriesRefreshHandler = new Handler(Looper.getMainLooper());
    private static final long STORIES_REFRESH_INTERVAL = 5 * 60 * 1000L; // 5 minutos
    private final Runnable mStoriesRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStoriesAdapter();
            mStoriesRefreshHandler.postDelayed(this, STORIES_REFRESH_INTERVAL);
        }
    };

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public HomeFragment() {}

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mview = inflater.inflate(R.layout.fragment_home, container, false);
        mFab = mview.findViewById(R.id.fab);
        mToolbar = mview.findViewById(R.id.toolbar);
        mRecyclerView = mview.findViewById(R.id.recyclerViewHome);
        mRecyclerViewStories = mview.findViewById(R.id.recyclerViewStories);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerViewStories.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Publicaciones");
        setHasOptionsMenu(true);

        mAuthProvider = new AuthProvider();
        mPostProvider = new PostProvider();
        mStoriesProvider = new StoriesProvider();

        mFab.setOnClickListener(view -> goToPost());

        mview.findViewById(R.id.layoutAddStory).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CreateStoryActivity.class)));

        return mview;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query postQuery = mPostProvider.getAll();
        FirestoreRecyclerOptions<Post> postOptions = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(postQuery, Post.class).build();
        mPostsAdapter = new PostsAdapter(postOptions, getContext());
        mRecyclerView.setAdapter(mPostsAdapter);
        mPostsAdapter.startListening();

        refreshStoriesAdapter();
        mStoriesRefreshHandler.postDelayed(mStoriesRefreshRunnable, STORIES_REFRESH_INTERVAL);
    }

    @Override
    public void onStop() {
        super.onStop();
        mPostsAdapter.stopListening();
        mStoriesRefreshHandler.removeCallbacks(mStoriesRefreshRunnable);
        if (mStoriesAdapter != null) mStoriesAdapter.stopListening();
    }

    private void refreshStoriesAdapter() {
        if (mStoriesAdapter != null) mStoriesAdapter.stopListening();
        Query storyQuery = mStoriesProvider.getActiveStories();
        FirestoreRecyclerOptions<Story> storyOptions = new FirestoreRecyclerOptions.Builder<Story>()
                .setQuery(storyQuery, Story.class).build();
        mStoriesAdapter = new StoriesAdapter(storyOptions, getContext());
        mRecyclerViewStories.setAdapter(mStoriesAdapter);
        mStoriesAdapter.startListening();
    }

    private void goToPost() {
        if (com.optic.socialmediagamer.utils.GuestGuard.check(getActivity())) return;
        startActivity(new Intent(getActivity(), PostActivity.class));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemLogout) {
            logout();
        } else if (item.getItemId() == R.id.itemSearch) {
            startActivity(new Intent(getActivity(), SearchPostsActivity.class));
        }
        return true;
    }

    private void logout() {
        mAuthProvider.logout();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
