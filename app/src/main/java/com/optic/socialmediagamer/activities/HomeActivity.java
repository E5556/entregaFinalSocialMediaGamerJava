package com.optic.socialmediagamer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(navigationItemSelectedListener);

        initFragments();

        mNotificationsProvider = new NotificationsProvider();
        mAuthProvider = new AuthProvider();
        listenUnreadBadge();
        checkProfileComplete();
        new UsersProvider().updateStreak(mAuthProvider.getUid());
    }

    private void initFragments() {
        mHomeFragment          = new HomeFragment();
        mExploreFragment       = new ExploreFragment();
        mChatsFragment         = new ChatsFragment();
        mNotificationsFragment = new NotificationsFragment();
        mProfileFragment       = new ProfileFragment();
        mActiveFragment        = mHomeFragment;

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.add(R.id.container, mProfileFragment).hide(mProfileFragment);
        t.add(R.id.container, mNotificationsFragment).hide(mNotificationsFragment);
        t.add(R.id.container, mChatsFragment).hide(mChatsFragment);
        t.add(R.id.container, mExploreFragment).hide(mExploreFragment);
        t.add(R.id.container, mHomeFragment);
        t.commit();
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
