package com.optic.socialmediagamer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.fragments.ChatsFragment;
import com.optic.socialmediagamer.fragments.ExploreFragment;
import com.optic.socialmediagamer.fragments.HomeFragment;
import com.optic.socialmediagamer.fragments.NotificationsFragment;
import com.optic.socialmediagamer.fragments.ProfileFragment;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigation;
    NotificationsProvider mNotificationsProvider;
    AuthProvider mAuthProvider;
    ListenerRegistration mUnreadListener;

    HomeFragment         mHomeFragment;
    ExploreFragment      mExploreFragment;
    ChatsFragment        mChatsFragment;
    NotificationsFragment mNotificationsFragment;
    ProfileFragment      mProfileFragment;
    Fragment             mActiveFragment;

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String TAG_HOME          = "tag_home";
    private static final String TAG_EXPLORE       = "tag_explore";
    private static final String TAG_CHATS         = "tag_chats";
    private static final String TAG_NOTIFICATIONS = "tag_notifications";
    private static final String TAG_PROFILE       = "tag_profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(navigationItemSelectedListener);

        int selectedTab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.itemHome)
                : R.id.itemHome;

        initFragments(savedInstanceState, selectedTab);

        mNotificationsProvider = new NotificationsProvider();
        mAuthProvider = new AuthProvider();
        listenUnreadBadge();
        if (savedInstanceState == null) {
            checkProfileComplete();
            new UsersProvider().updateStreak(mAuthProvider.getUid());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, bottomNavigation.getSelectedItemId());
    }

    private void initFragments(Bundle savedInstanceState, int selectedTabId) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState != null) {
            // Fragments already restored by FragmentManager — just retrieve them by tag
            mHomeFragment          = (HomeFragment)          fm.findFragmentByTag(TAG_HOME);
            mExploreFragment       = (ExploreFragment)       fm.findFragmentByTag(TAG_EXPLORE);
            mChatsFragment         = (ChatsFragment)         fm.findFragmentByTag(TAG_CHATS);
            mNotificationsFragment = (NotificationsFragment) fm.findFragmentByTag(TAG_NOTIFICATIONS);
            mProfileFragment       = (ProfileFragment)       fm.findFragmentByTag(TAG_PROFILE);
        }

        // Fallback: create any fragment that wasn't found (first launch or missing tag)
        if (mHomeFragment          == null) mHomeFragment          = new HomeFragment();
        if (mExploreFragment       == null) mExploreFragment       = new ExploreFragment();
        if (mChatsFragment         == null) mChatsFragment         = new ChatsFragment();
        if (mNotificationsFragment == null) mNotificationsFragment = new NotificationsFragment();
        if (mProfileFragment       == null) mProfileFragment       = new ProfileFragment();

        mActiveFragment = fragmentForId(selectedTabId);

        FragmentTransaction t = fm.beginTransaction();
        addIfNeeded(t, mProfileFragment,       TAG_PROFILE);
        addIfNeeded(t, mNotificationsFragment, TAG_NOTIFICATIONS);
        addIfNeeded(t, mChatsFragment,         TAG_CHATS);
        addIfNeeded(t, mExploreFragment,       TAG_EXPLORE);
        addIfNeeded(t, mHomeFragment,          TAG_HOME);

        // Hide all, then show the selected one
        t.hide(mHomeFragment).hide(mExploreFragment).hide(mChatsFragment)
         .hide(mNotificationsFragment).hide(mProfileFragment)
         .show(mActiveFragment);
        t.commit();

        bottomNavigation.setSelectedItemId(selectedTabId);
    }

    private void addIfNeeded(FragmentTransaction t, Fragment f, String tag) {
        if (!f.isAdded()) t.add(R.id.container, f, tag);
    }

    private Fragment fragmentForId(int id) {
        if (id == R.id.itemExplore)       return mExploreFragment;
        if (id == R.id.itemChats)         return mChatsFragment;
        if (id == R.id.itemNotifications) return mNotificationsFragment;
        if (id == R.id.itemProfile)       return mProfileFragment;
        return mHomeFragment;
    }

    private void showFragment(Fragment target) {
        getSupportFragmentManager().beginTransaction()
                .hide(mActiveFragment)
                .show(target)
                .commit();
        mActiveFragment = target;
    }

    private void checkProfileComplete() {
        String uid = mAuthProvider.getUid();
        if (uid == null || mAuthProvider.isGuest()) return;
        new UsersProvider().getUser(uid).addOnSuccessListener(doc -> {
            String username = doc.getString("username");
            if (username == null || username.trim().isEmpty()) {
                startActivity(new Intent(this, CompleteProfileActivity.class));
            }
        });
    }

    private void listenUnreadBadge() {
        String uid = mAuthProvider.getUid();
        if (uid == null) return;
        mUnreadListener = mNotificationsProvider.getUnreadCount(uid)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    int count = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean read = doc.getBoolean("read");
                        if (read == null || !read) count++;
                    }
                    if (count > 0) {
                        bottomNavigation.getOrCreateBadge(R.id.itemNotifications).setNumber(count);
                    } else {
                        bottomNavigation.removeBadge(R.id.itemNotifications);
                    }
                });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUnreadListener != null) mUnreadListener.remove();
    }

    NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = item -> {
        if (item.getItemId() == R.id.itemHome) {
            showFragment(mHomeFragment);
        } else if (item.getItemId() == R.id.itemExplore) {
            showFragment(mExploreFragment);
        } else if (item.getItemId() == R.id.itemChats) {
            showFragment(mChatsFragment);
        } else if (item.getItemId() == R.id.itemNotifications) {
            showFragment(mNotificationsFragment);
        } else if (item.getItemId() == R.id.itemProfile) {
            showFragment(mProfileFragment);
        }
        return true;
    };
}
