package com.optic.socialmediagamer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.adapters.PostsAdapter;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.FollowProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.utils.FCMSender;
import com.optic.socialmediagamer.utils.RankHelper;
import com.squareup.picasso.Picasso;

import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    ImageView mImageViewCover;
    CircleImageView mCircleImageProfile;
    CircleImageView mCircleImageBack;
    TextView mTextViewUsername;
    TextView mTextViewEmail;
    TextView mTextViewPostNumber;
    TextView mTextViewFollowersCount;
    Button mButtonFollow;
    RecyclerView mRecyclerView;

    UsersProvider mUsersProvider;
    PostProvider mPostProvider;
    FollowProvider mFollowProvider;
    AuthProvider mAuthProvider;
    NotificationsProvider mNotificationsProvider;
    XPProvider mXPProvider;
    PostsAdapter mPostsAdapter;

    String mIdUser;
    boolean mIsFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mIdUser = getIntent().getStringExtra("idUser");

        mImageViewCover       = findViewById(R.id.imageViewCoverProfile);
        mCircleImageProfile   = findViewById(R.id.circleImageUserProfile);
        mCircleImageBack      = findViewById(R.id.circleImageBackProfile);
        mTextViewUsername     = findViewById(R.id.textViewUsernameProfile);
        mTextViewEmail        = findViewById(R.id.textViewEmailProfile);
        mTextViewPostNumber   = findViewById(R.id.textViewPostNumberProfile);
        mTextViewFollowersCount = findViewById(R.id.textViewFollowersCount);
        mButtonFollow         = findViewById(R.id.btnFollow);
        mRecyclerView         = findViewById(R.id.recyclerViewUserPosts);

        mUsersProvider         = new UsersProvider();
        mPostProvider          = new PostProvider();
        mFollowProvider        = new FollowProvider();
        mAuthProvider          = new AuthProvider();
        mNotificationsProvider = new NotificationsProvider();
        mXPProvider = new XPProvider();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mCircleImageBack.setOnClickListener(v -> finish());
        mButtonFollow.setOnClickListener(v -> toggleFollow());

        loadUserData();
        loadPostCount();
        loadFollowersCount();
        checkIfFollowing();
    }

    private void loadUserData() {
        mUsersProvider.getUser(mIdUser).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username     = documentSnapshot.getString("username");
                String email        = documentSnapshot.getString("email");
                String imageProfile = documentSnapshot.getString("image_profile");
                String imageCover   = documentSnapshot.getString("image_cover");

                if (username != null) mTextViewUsername.setText(username);
                if (email != null)    mTextViewEmail.setText(email);
                if (imageProfile != null && !imageProfile.isEmpty()) Picasso.get().load(imageProfile).into(mCircleImageProfile);
                if (imageCover != null && !imageCover.isEmpty()) Picasso.get().load(imageCover).into(mImageViewCover);
            }
        });
    }

    private void loadPostCount() {
        mPostProvider.getPostByUser(mIdUser).get().addOnSuccessListener(snap -> {
            mTextViewPostNumber.setText(String.valueOf(snap.size()));
        });
    }

    private void loadFollowersCount() {
        mFollowProvider.getFollowers(mIdUser).get().addOnSuccessListener(snap -> {
            mTextViewFollowersCount.setText(String.valueOf(snap.size()));
        });
    }

    private void checkIfFollowing() {
        String myId = mAuthProvider.getUid();
        if (myId == null || myId.equals(mIdUser)) {
            mButtonFollow.setVisibility(android.view.View.GONE);
            return;
        }
        mFollowProvider.getFollow(myId, mIdUser).get().addOnSuccessListener(doc -> {
            mIsFollowing = doc.exists();
            updateFollowButton();
        });
    }

    private void toggleFollow() {
        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        if (mIsFollowing) {
            mFollowProvider.unfollow(myId, mIdUser).addOnSuccessListener(unused -> {
                mIsFollowing = false;
                updateFollowButton();
                loadFollowersCount();
            });
        } else {
            mFollowProvider.follow(myId, mIdUser).addOnSuccessListener(unused -> {
                mIsFollowing = true;
                updateFollowButton();
                new MissionsProvider().incrementProgress(myId, MissionsProvider.TYPE_FOLLOW);
                loadFollowersCount();
                mXPProvider.addXP(myId, RankHelper.XP_FOLLOW_GIVEN);
                mXPProvider.addXP(mIdUser, RankHelper.XP_FOLLOWED);
                sendFollowNotification(myId);
            });
        }
    }

    private void updateFollowButton() {
        if (mIsFollowing) {
            mButtonFollow.setText("SIGUIENDO");
            mButtonFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#BF00FF")));
        } else {
            mButtonFollow.setText("SEGUIR");
            mButtonFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#00F0FF")));
        }
    }

    private void sendFollowNotification(String myId) {
        mUsersProvider.getUser(myId).addOnSuccessListener(doc -> {
            String myUsername = doc.exists() && doc.getString("username") != null
                    ? doc.getString("username") : "Alguien";
            Notification notif = new Notification();
            notif.setType("follow");
            notif.setIdFrom(myId);
            notif.setIdTo(mIdUser);
            notif.setBody("@" + myUsername + " comenzó a seguirte");
            notif.setRead(false);
            notif.setTimestamp(new Date().getTime());
            mNotificationsProvider.save(notif);

            mUsersProvider.getUser(mIdUser).addOnSuccessListener(ownerDoc -> {
                String fcmToken = ownerDoc.getString("fcmToken");
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    FCMSender.send(getApplicationContext(), fcmToken, "Nuevo seguidor 👾", "@" + myUsername + " comenzó a seguirte");
                }
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Query query = mPostProvider.getPostByUser(mIdUser);
        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class).build();
        mPostsAdapter = new PostsAdapter(options, this);
        mRecyclerView.setAdapter(mPostsAdapter);
        mPostsAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPostsAdapter.stopListening();
    }
}
