package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.adapters.CommunitiesAdapter;
import com.optic.socialmediagamer.adapters.EventsAdapter;
import com.optic.socialmediagamer.adapters.PostsAdapter;
import com.optic.socialmediagamer.adapters.SearchUsersAdapter;
import com.optic.socialmediagamer.models.Community;
import com.optic.socialmediagamer.models.Event;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.CommunitiesProvider;
import com.optic.socialmediagamer.providers.EventsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

public class SearchPostsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private TabLayout mTabLayout;

    private PostsAdapter mPostsAdapter;
    private SearchUsersAdapter mUsersAdapter;
    private CommunitiesAdapter mCommunitiesAdapter;
    private EventsAdapter mEventsAdapter;

    private final PostProvider mPostProvider = new PostProvider();
    private final UsersProvider mUsersProvider = new UsersProvider();
    private final CommunitiesProvider mCommunitiesProvider = new CommunitiesProvider();
    private final EventsProvider mEventsProvider = new EventsProvider();
    private final AuthProvider mAuthProvider = new AuthProvider();

    // 0=Posts, 1=Users, 2=Communities, 3=Events
    private int mActiveTab = 0;
    private String mCurrentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_posts);

        mRecyclerView = findViewById(R.id.recyclerViewSearch);
        mSearchView   = findViewById(R.id.searchView);
        mTabLayout    = findViewById(R.id.tabLayoutSearch);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.requestFocus();

        mTabLayout.addTab(mTabLayout.newTab().setText("Publicaciones"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Usuarios"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Comunidades"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Eventos"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                mActiveTab = tab.getPosition();
                search(mCurrentQuery);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mCurrentQuery = query;
                search(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentQuery = newText;
                if (newText.length() >= 2 || newText.isEmpty()) {
                    search(newText);
                }
                return true;
            }
        });

        search("");
    }

    private void search(String text) {
        stopCurrentAdapter();
        switch (mActiveTab) {
            case 0: searchPosts(text); break;
            case 1: searchUsers(text); break;
            case 2: searchCommunities(text); break;
            case 3: searchEvents(text); break;
        }
    }

    private void searchPosts(String text) {
        Query query = TextUtils.isEmpty(text)
                ? mPostProvider.getAll()
                : mPostProvider.searchByTitle(text);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class).build();

        mPostsAdapter = new PostsAdapter(options, this);
        mRecyclerView.setAdapter(mPostsAdapter);
        mPostsAdapter.startListening();
    }

    private void searchUsers(String text) {
        Query query = TextUtils.isEmpty(text)
                ? mUsersProvider.getAll()
                : mUsersProvider.searchByUsername(text);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class).build();

        mUsersAdapter = new SearchUsersAdapter(options, this);
        mRecyclerView.setAdapter(mUsersAdapter);
        mUsersAdapter.startListening();
    }

    private void searchCommunities(String text) {
        Query query = TextUtils.isEmpty(text)
                ? mCommunitiesProvider.getAll()
                : mCommunitiesProvider.searchByName(text);

        FirestoreRecyclerOptions<Community> options = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(query, Community.class).build();

        mCommunitiesAdapter = new CommunitiesAdapter(options, this);
        mRecyclerView.setAdapter(mCommunitiesAdapter);
        mCommunitiesAdapter.startListening();
    }

    private void searchEvents(String text) {
        Query query = TextUtils.isEmpty(text)
                ? mEventsProvider.getAll()
                : mEventsProvider.searchByTitle(text);

        FirestoreRecyclerOptions<Event> options = new FirestoreRecyclerOptions.Builder<Event>()
                .setQuery(query, Event.class).build();

        mEventsAdapter = new EventsAdapter(options, this);
        mRecyclerView.setAdapter(mEventsAdapter);
        mEventsAdapter.startListening();
    }

    private void stopCurrentAdapter() {
        if (mPostsAdapter != null) { mPostsAdapter.stopListening(); mPostsAdapter = null; }
        if (mUsersAdapter != null) { mUsersAdapter.stopListening(); mUsersAdapter = null; }
        if (mCommunitiesAdapter != null) { mCommunitiesAdapter.stopListening(); mCommunitiesAdapter = null; }
        if (mEventsAdapter != null) { mEventsAdapter.stopListening(); mEventsAdapter = null; }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopCurrentAdapter();
    }
}
