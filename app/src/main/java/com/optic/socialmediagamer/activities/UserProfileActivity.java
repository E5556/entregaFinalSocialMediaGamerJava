package com.optic.socialmediagamer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.optic.socialmediagamer.providers.ChallengesProvider;
import com.optic.socialmediagamer.providers.EndorsementsProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.ReputationProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.models.Challenge;
import com.optic.socialmediagamer.models.SkillEndorsement;
import com.optic.socialmediagamer.utils.FCMSender;
import com.optic.socialmediagamer.utils.RankHelper;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    ReputationProvider mReputationProvider;
    ChallengesProvider mChallengesProvider;
    EndorsementsProvider mEndorsementsProvider;
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
        mReputationProvider = new ReputationProvider();
        mChallengesProvider = new ChallengesProvider();
        mEndorsementsProvider = new EndorsementsProvider();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mCircleImageBack.setOnClickListener(v -> finish());
        mButtonFollow.setOnClickListener(v -> toggleFollow());

        // Challenge button (only for other users)
        Button btnChallenge = findViewById(R.id.btnChallenge);
        String myId = mAuthProvider.getUid();
        if (myId != null && !myId.equals(mIdUser)) {
            btnChallenge.setVisibility(View.VISIBLE);
            btnChallenge.setOnClickListener(v -> showChallengeDialog());
        }

        loadUserData();
        loadPostCount();
        loadFollowersCount();
        checkIfFollowing();
        loadReputation();
        loadEndorsements();
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

    private void showChallengeDialog() {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        container.setPadding(pad, dpToPx(8), pad, 0);

        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Describe el desafío (ej: primero en llegar a Gold)");
        et.setMinLines(2);
        et.setMaxLines(4);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setTextColor(getColor(R.color.color_text_primary));
        et.setHintTextColor(getColor(R.color.color_text_secondary));
        container.addView(et);

        new android.app.AlertDialog.Builder(this)
                .setTitle("⚔️ Enviar desafío")
                .setView(container)
                .setPositiveButton("ENVIAR", (d, w) -> {
                    String desc = et.getText().toString().trim();
                    if (desc.isEmpty()) { Toast.makeText(this, "Escribe el desafío", Toast.LENGTH_SHORT).show(); return; }
                    String senderId = mAuthProvider.getUid();
                    Challenge c = new Challenge();
                    c.setIdChallenger(senderId);
                    c.setIdChallenged(mIdUser);
                    c.setDescription(desc);
                    c.setTimestamp(System.currentTimeMillis());
                    mChallengesProvider.send(c).addOnSuccessListener(u -> {
                        Toast.makeText(this, "¡Desafío enviado! ⚔️", Toast.LENGTH_SHORT).show();
                        // FCM to challenged
                        mUsersProvider.getUser(senderId).addOnSuccessListener(senderDoc -> {
                            String senderUsername = senderDoc.exists() && senderDoc.getString("username") != null
                                    ? senderDoc.getString("username") : "Alguien";
                            mUsersProvider.getUser(mIdUser).addOnSuccessListener(ownerDoc -> {
                                String token = ownerDoc.getString("fcmToken");
                                if (token != null && !token.isEmpty()) {
                                    FCMSender.send(getApplicationContext(), token,
                                            "⚔️ Nuevo desafío", "@" + senderUsername + " te ha desafiado");
                                }
                            });
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadReputation() {
        mReputationProvider.getByUser(mIdUser).addOnSuccessListener(snap -> {
            if (snap.isEmpty()) return;

            float total = 0f;
            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                com.optic.socialmediagamer.models.GameRating r =
                        doc.toObject(com.optic.socialmediagamer.models.GameRating.class);
                if (r != null) total += (r.getCommunication() + r.getSkill() + r.getAttitude()) / 3f;
            }
            float avg = total / snap.size();

            LinearLayout section = findViewById(R.id.layoutReputationSection);
            View divider = findViewById(R.id.dividerReputation);
            TextView tvValue = findViewById(R.id.textViewGamerScoreValue);
            TextView tvStars = findViewById(R.id.textViewGamerScoreStars);
            TextView tvCount = findViewById(R.id.textViewGamerScoreCount);

            section.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            tvValue.setText(String.format(java.util.Locale.getDefault(), "%.1f", avg));
            tvStars.setText(buildStars(avg));
            tvCount.setText(snap.size() + " valoraciones");
        });
    }

    private String buildStars(float avg) {
        StringBuilder sb = new StringBuilder();
        int full = (int) avg;
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
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

    private void loadEndorsements() {
        String myId = mAuthProvider.getUid();
        boolean isOwnProfile = myId != null && myId.equals(mIdUser);

        android.widget.LinearLayout layoutSection = findViewById(R.id.layoutSkillsSection);
        View divider = findViewById(R.id.dividerSkills);
        if (layoutSection == null) return;

        mEndorsementsProvider.getEndorsementsForUser(mIdUser).addOnSuccessListener(snap -> {
            Map<String, Integer> counts = new HashMap<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                SkillEndorsement e = doc.toObject(SkillEndorsement.class);
                if (e == null || e.getSkill() == null) continue;
                counts.put(e.getSkill(), counts.getOrDefault(e.getSkill(), 0) + 1);
            }

            // Hide section on own profile if no endorsements
            if (counts.isEmpty() && isOwnProfile) return;

            // Clear and rebuild — prevents duplication on reload
            layoutSection.removeAllViews();
            layoutSection.setVisibility(View.VISIBLE);
            if (divider != null) divider.setVisibility(View.VISIBLE);

            android.widget.TextView tvLabel = new android.widget.TextView(this);
            tvLabel.setText("🏷️ HABILIDADES");
            tvLabel.setTextColor(getColor(R.color.color_primary));
            tvLabel.setTextSize(11f);
            tvLabel.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
            tvLabel.setPadding(0, 0, 0, dpToPx(8));
            layoutSection.addView(tvLabel);

            android.widget.LinearLayout layoutTags = new android.widget.LinearLayout(this);
            layoutTags.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            layoutTags.setPadding(0, 0, 0, dpToPx(8));
            layoutSection.addView(layoutTags);

            if (counts.isEmpty()) {
                android.widget.TextView tvEmpty = new android.widget.TextView(this);
                tvEmpty.setText("Sin habilidades endorsadas aún");
                tvEmpty.setTextColor(getColor(R.color.color_text_secondary));
                tvEmpty.setTextSize(12f);
                layoutTags.addView(tvEmpty);
            } else {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String skill = entry.getKey();
                    int count = entry.getValue();
                    android.widget.TextView chip = new android.widget.TextView(this);
                    chip.setText(SkillEndorsement.getEmoji(skill) + " " + SkillEndorsement.getLabel(skill) + " ×" + count);
                    chip.setTextSize(12f);
                    chip.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
                    chip.setBackgroundColor(getColor(count >= 3 ? R.color.color_primary : R.color.color_surface_2));
                    chip.setTextColor(getColor(count >= 3 ? R.color.color_background : R.color.color_text_primary));
                    android.widget.LinearLayout.LayoutParams lp =
                            new android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, dpToPx(6), 0);
                    chip.setLayoutParams(lp);
                    layoutTags.addView(chip);
                }
            }

            // Single endorse button, always at the bottom, only for other users
            if (!isOwnProfile) {
                android.widget.Button btnEndorse = new android.widget.Button(this);
                btnEndorse.setText("+ ENDORSAR HABILIDAD");
                btnEndorse.setTextSize(11f);
                android.widget.LinearLayout.LayoutParams btnLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                btnEndorse.setLayoutParams(btnLp);
                btnEndorse.setOnClickListener(v -> showEndorseDialog(myId));
                layoutSection.addView(btnEndorse);
            }
        });
    }

    private void showEndorseDialog(String myId) {
        String[] skills = {
                SkillEndorsement.BUEN_PUNTERO,
                SkillEndorsement.ESTRATEGA,
                SkillEndorsement.BUEN_COMPANERO,
                SkillEndorsement.LIDER,
                SkillEndorsement.COACH
        };
        String[] labels = {
                SkillEndorsement.getEmoji(SkillEndorsement.BUEN_PUNTERO) + " " + SkillEndorsement.getLabel(SkillEndorsement.BUEN_PUNTERO),
                SkillEndorsement.getEmoji(SkillEndorsement.ESTRATEGA) + " " + SkillEndorsement.getLabel(SkillEndorsement.ESTRATEGA),
                SkillEndorsement.getEmoji(SkillEndorsement.BUEN_COMPANERO) + " " + SkillEndorsement.getLabel(SkillEndorsement.BUEN_COMPANERO),
                SkillEndorsement.getEmoji(SkillEndorsement.LIDER) + " " + SkillEndorsement.getLabel(SkillEndorsement.LIDER),
                SkillEndorsement.getEmoji(SkillEndorsement.COACH) + " " + SkillEndorsement.getLabel(SkillEndorsement.COACH)
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("🏷️ Endorsar habilidad")
                .setItems(labels, (d, which) -> {
                    String skill = skills[which];
                    mEndorsementsProvider.hasEndorsed(myId, mIdUser, skill).addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            Toast.makeText(this, "Ya endorsaste esta habilidad", Toast.LENGTH_SHORT).show();
                        } else {
                            mEndorsementsProvider.endorse(myId, mIdUser, skill).addOnSuccessListener(u -> {
                                Toast.makeText(this, "¡Endorsado! 🏷️", Toast.LENGTH_SHORT).show();
                                loadEndorsements();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
