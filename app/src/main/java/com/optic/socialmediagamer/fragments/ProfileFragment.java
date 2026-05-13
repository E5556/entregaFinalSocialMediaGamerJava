package com.optic.socialmediagamer.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.optic.socialmediagamer.adapters.BadgesAdapter;
import com.optic.socialmediagamer.adapters.MyPostsAdapter;
import com.optic.socialmediagamer.models.Badge;
import com.optic.socialmediagamer.models.Post;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.EditProfileActivity;
import com.optic.socialmediagamer.activities.MyCollectionsActivity;
import com.optic.socialmediagamer.activities.ClansActivity;
import com.optic.socialmediagamer.activities.GamerCardActivity;
import com.optic.socialmediagamer.activities.WeeklyChallengeActivity;
import com.optic.socialmediagamer.activities.MyChallengesActivity;
import com.optic.socialmediagamer.activities.WeeklyMissionsActivity;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.BadgeProvider;
import com.optic.socialmediagamer.providers.FollowProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.TwitchProvider;
import com.optic.socialmediagamer.utils.RankHelper;
import com.optic.socialmediagamer.utils.ThemeHelper;
import com.squareup.picasso.Picasso;

import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    View mView;
    LinearLayout mLinearLayoutEditProfile;
    TextView mTextViewUsername;
    TextView mTextViewPhone;
    TextView mTextViewEmail;
    TextView mTextViewPostNumber;
    ImageView mImageViewCover;
    CircleImageView mCircleImageProfile;

    TextView mTextViewLiveBadge;
    Button mButtonWatchStream;
    TextView mTextViewRankEmoji;
    TextView mTextViewRankName;
    TextView mTextViewXP;
    TextView mTextViewXPNextRank;
    TextView mTextViewStreak;
    ProgressBar mProgressBarXP;

    RecyclerView mRecyclerView;
    RecyclerView mRecyclerViewBadges;
    MyPostsAdapter mMyPostsAdapter;
    BadgesAdapter mBadgesAdapter;

    TextView mTextViewFollowersCount;
    TextView mTextViewFollowingCount;
    TextView mTextViewNowPlayingDisplay;
    LinearLayout mLayoutNowPlayingEdit;
    EditText mEditTextNowPlaying;
    TextView mBtnEditNowPlaying;
    TextView mBtnSaveNowPlaying;
    TextView mBtnClearNowPlaying;

    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;
    PostProvider mPostProvider;
    BadgeProvider mBadgeProvider;
    FollowProvider mFollowProvider;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_profile, container, false);
        mLinearLayoutEditProfile = mView.findViewById(R.id.linearLayoutEditProfile);
        mTextViewEmail           = mView.findViewById(R.id.textViewEmail);
        mTextViewUsername        = mView.findViewById(R.id.textViewUsername);
        mTextViewPhone           = mView.findViewById(R.id.textViewphone);
        mTextViewPostNumber      = mView.findViewById(R.id.textViewPostNumber);
        mCircleImageProfile      = mView.findViewById(R.id.circleImageProfile);
        mImageViewCover          = mView.findViewById(R.id.imageViewCover);
        mRecyclerView            = mView.findViewById(R.id.recyclerViewMyPosts);
        mRecyclerViewBadges      = mView.findViewById(R.id.recyclerViewBadges);
        mTextViewRankEmoji       = mView.findViewById(R.id.textViewRankEmoji);
        mTextViewRankName        = mView.findViewById(R.id.textViewRankName);
        mTextViewXP              = mView.findViewById(R.id.textViewXP);
        mTextViewXPNextRank      = mView.findViewById(R.id.textViewXPNextRank);
        mTextViewStreak          = mView.findViewById(R.id.textViewStreak);
        mProgressBarXP           = mView.findViewById(R.id.progressBarXP);
        mTextViewLiveBadge       = mView.findViewById(R.id.textViewLiveBadge);
        mButtonWatchStream       = mView.findViewById(R.id.btnWatchStream);
        mTextViewFollowersCount      = mView.findViewById(R.id.textViewProfileFollowers);
        mTextViewFollowingCount      = mView.findViewById(R.id.textViewProfileFollowing);
        mTextViewNowPlayingDisplay   = mView.findViewById(R.id.textViewNowPlayingDisplay);
        mLayoutNowPlayingEdit        = mView.findViewById(R.id.layoutNowPlayingEdit);
        mEditTextNowPlaying          = mView.findViewById(R.id.editTextNowPlaying);
        mBtnEditNowPlaying           = mView.findViewById(R.id.btnEditNowPlaying);
        mBtnSaveNowPlaying           = mView.findViewById(R.id.btnSaveNowPlaying);
        mBtnClearNowPlaying          = mView.findViewById(R.id.btnClearNowPlaying);

        mBtnEditNowPlaying.setOnClickListener(v ->
            mLayoutNowPlayingEdit.setVisibility(
                mLayoutNowPlayingEdit.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        mBtnSaveNowPlaying.setOnClickListener(v -> {
            String game = mEditTextNowPlaying.getText().toString().trim();
            if (game.isEmpty()) { Toast.makeText(getContext(), "Escribe el nombre del juego", Toast.LENGTH_SHORT).show(); return; }
            mUsersProvider.setNowPlaying(mAuthProvider.getUid(), game).addOnSuccessListener(u -> {
                mTextViewNowPlayingDisplay.setText("🎮 " + game);
                mTextViewNowPlayingDisplay.setTextColor(requireContext().getColor(R.color.color_primary));
                mLayoutNowPlayingEdit.setVisibility(View.GONE);
            });
        });

        mBtnClearNowPlaying.setOnClickListener(v ->
            mUsersProvider.clearNowPlaying(mAuthProvider.getUid()).addOnSuccessListener(u -> {
                mTextViewNowPlayingDisplay.setText("🎮 Sin juego activo");
                mTextViewNowPlayingDisplay.setTextColor(requireContext().getColor(R.color.color_text_secondary));
                mEditTextNowPlaying.setText("");
                mLayoutNowPlayingEdit.setVisibility(View.GONE);
            }));

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerViewBadges.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        mLinearLayoutEditProfile.setOnClickListener(view -> goToEditProfile());

        mView.findViewById(R.id.layoutGamerCard).setOnClickListener(v ->
                startActivity(new Intent(getContext(), GamerCardActivity.class)));

        mView.findViewById(R.id.layoutClans).setOnClickListener(v ->
                startActivity(new Intent(getContext(), ClansActivity.class)));

        mView.findViewById(R.id.layoutWeeklyChallenge).setOnClickListener(v ->
                startActivity(new Intent(getContext(), WeeklyChallengeActivity.class)));

        mView.findViewById(R.id.layoutMyChallenges).setOnClickListener(v ->
                startActivity(new Intent(getContext(), MyChallengesActivity.class)));

        mView.findViewById(R.id.layoutWeeklyMissions).setOnClickListener(v ->
                startActivity(new Intent(getContext(), WeeklyMissionsActivity.class)));

        mView.findViewById(R.id.layoutMyCollections).setOnClickListener(v ->
                startActivity(new Intent(getContext(), MyCollectionsActivity.class)));

        SwitchCompat switchDarkMode = mView.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(ThemeHelper.isDarkMode(requireContext()));
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                ThemeHelper.setDarkMode(requireContext(), isChecked));

        mUsersProvider = new UsersProvider();
        mAuthProvider  = new AuthProvider();
        mPostProvider  = new PostProvider();
        mBadgeProvider = new BadgeProvider();
        mFollowProvider = new FollowProvider();

        getUser();
        getPostNumber();
        loadBadges();
        loadFollowCounts();
        checkTimedBadges();
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Query query = mPostProvider.getPostByUser(mAuthProvider.getUid());
        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class).build();
        mMyPostsAdapter = new MyPostsAdapter(options, getContext());
        mRecyclerView.setAdapter(mMyPostsAdapter);
        mMyPostsAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMyPostsAdapter != null) mMyPostsAdapter.stopListening();
    }

    private void goToEditProfile() {
        startActivity(new Intent(getContext(), EditProfileActivity.class));
    }

    private void getPostNumber() {
        mPostProvider.getPostByUser(mAuthProvider.getUid()).get().addOnSuccessListener(snap -> {
            int numberPost = snap.size();
            mTextViewPostNumber.setText(String.valueOf(numberPost));
            checkAndAwardBadges(numberPost);
        });
    }

    private void checkAndAwardBadges(int postCount) {
        String uid = mAuthProvider.getUid();
        if (postCount >= 1) mBadgeProvider.awardIfMissing(uid, Badge.PRIMER_POST);

        mPostProvider.getPostByUser(uid).get().addOnSuccessListener(snap -> {
            long totalLikes = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String category = doc.getString("category");
                if ("PC".equals(category)) mBadgeProvider.awardIfMissing(uid, Badge.PC_FAN);
                else if ("PS4".equals(category)) mBadgeProvider.awardIfMissing(uid, Badge.PS_FAN);
                else if ("XBOX".equals(category)) mBadgeProvider.awardIfMissing(uid, Badge.XBOX_FAN);
                else if ("Nintendo".equals(category)) mBadgeProvider.awardIfMissing(uid, Badge.NINTENDO_FAN);

                Long likeCount = doc.getLong("likeCount");
                if (likeCount != null) totalLikes += likeCount;
            }
            if (totalLikes >= 50) mBadgeProvider.awardIfMissing(uid, Badge.POPULAR);
        });
    }

    private void checkTimedBadges() {
        String uid = mAuthProvider.getUid();
        mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Long accountTimestamp = doc.getLong("timestamp");
            if (accountTimestamp != null) {
                long days = (new Date().getTime() - accountTimestamp) / (1000 * 60 * 60 * 24);
                if (days >= 30) mBadgeProvider.awardIfMissing(uid, Badge.VETERANO);
            }
        });

        mFollowProvider.getFollowing(uid).get().addOnSuccessListener(snap -> {
            if (snap.size() >= 10) mBadgeProvider.awardIfMissing(uid, Badge.SOCIAL);
        });

        mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            long streak = doc.getLong("currentStreak") != null ? doc.getLong("currentStreak") : 0L;
            if (streak >= 7)   mBadgeProvider.awardIfMissing(uid, Badge.RACHA_7);
            if (streak >= 30)  mBadgeProvider.awardIfMissing(uid, Badge.RACHA_30);
            if (streak >= 100) mBadgeProvider.awardIfMissing(uid, Badge.RACHA_100);
        });
    }

    private void loadBadges() {
        mBadgeProvider.getBadgesByUser(mAuthProvider.getUid()).get().addOnSuccessListener(snap -> {
            List<Badge> badges = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Badge badge = doc.toObject(Badge.class);
                if (badge != null) badges.add(badge);
            }
            mBadgesAdapter = new BadgesAdapter(badges);
            mRecyclerViewBadges.setAdapter(mBadgesAdapter);
        });
    }

    private void getUser() {
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("email")) {
                    mTextViewEmail.setText(documentSnapshot.getString("email"));
                }
                if (documentSnapshot.contains("phone")) {
                    mTextViewPhone.setText(documentSnapshot.getString("phone"));
                }
                if (documentSnapshot.contains("username")) {
                    mTextViewUsername.setText(documentSnapshot.getString("username"));
                }
                if (documentSnapshot.contains("image_profile")) {
                    String imageProfile = documentSnapshot.getString("image_profile");
                    if (imageProfile != null && !imageProfile.isEmpty()) {
                        Picasso.get().load(imageProfile).into(mCircleImageProfile);
                    }
                }
                if (documentSnapshot.contains("image_cover")) {
                    String imageCover = documentSnapshot.getString("image_cover");
                    if (imageCover != null && !imageCover.isEmpty()) {
                        Picasso.get().load(imageCover).into(mImageViewCover);
                    }
                }
                long xp = documentSnapshot.getLong("xp") != null ? documentSnapshot.getLong("xp") : 0L;
                updateXPUI(xp);

                long streak = documentSnapshot.getLong("currentStreak") != null
                        ? documentSnapshot.getLong("currentStreak") : 0L;
                if (mTextViewStreak != null) {
                    mTextViewStreak.setText(streak > 0 ? "🔥 " + streak + " días de racha" : "🔥 Sin racha activa");
                }

                String nowPlaying = documentSnapshot.getString("nowPlaying");
                if (nowPlaying != null && !nowPlaying.isEmpty()) {
                    mTextViewNowPlayingDisplay.setText("🎮 " + nowPlaying);
                    mTextViewNowPlayingDisplay.setTextColor(requireContext().getColor(R.color.color_primary));
                    mEditTextNowPlaying.setText(nowPlaying);
                }
                String twitchUsername = documentSnapshot.getString("twitchUsername");
                if (twitchUsername != null && !twitchUsername.isEmpty()) {
                    new TwitchProvider().checkIfLive(twitchUsername, (isLive, viewers) -> {
                        if (mTextViewLiveBadge == null) return;
                        if (isLive) {
                            mTextViewLiveBadge.setVisibility(View.VISIBLE);
                            mTextViewLiveBadge.setText("🔴 EN VIVO · " + viewers + " espectadores");
                            mButtonWatchStream.setVisibility(View.VISIBLE);
                            mButtonWatchStream.setOnClickListener(v -> openTwitchStream(twitchUsername));
                        } else {
                            mTextViewLiveBadge.setVisibility(View.GONE);
                            mButtonWatchStream.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void loadFollowCounts() {
        String uid = mAuthProvider.getUid();
        mFollowProvider.getFollowers(uid).get().addOnSuccessListener(snap -> {
            if (mTextViewFollowersCount != null)
                mTextViewFollowersCount.setText(String.valueOf(snap.size()));
        });
        mFollowProvider.getFollowing(uid).get().addOnSuccessListener(snap -> {
            if (mTextViewFollowingCount != null)
                mTextViewFollowingCount.setText(String.valueOf(snap.size()));
        });
    }

    private void openTwitchStream(String twitchUsername) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.twitch.tv/" + twitchUsername));
        intent.setPackage("tv.twitch.android.app");
        if (intent.resolveActivity(requireActivity().getPackageManager()) == null) {
            intent.setPackage(null);
        }
        startActivity(intent);
    }

    private void updateXPUI(long xp) {
        mTextViewRankEmoji.setText(RankHelper.getRankEmoji(xp));
        mTextViewRankName.setText(RankHelper.getRankName(xp));
        mTextViewXP.setText(xp + " XP");
        mProgressBarXP.setProgress(RankHelper.getProgressPercent(xp));
        long next = RankHelper.getNextRankXP(xp);
        if (next == -1) {
            mTextViewXPNextRank.setText("¡Rango máximo alcanzado!");
        } else {
            mTextViewXPNextRank.setText("Siguiente rango: " + next + " XP");
        }
    }
}
