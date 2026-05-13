package com.optic.socialmediagamer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.optic.socialmediagamer.utils.FCMSender;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.adapters.CommentsAdapter;
import com.optic.socialmediagamer.models.Comment;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.CommentsProvider;
import com.optic.socialmediagamer.providers.LikesProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity {

    ImageView mImageViewDetail1;
    ImageView mImageViewDetail2;
    TextView mTextViewTitle;
    TextView mTextViewDescription;
    TextView mTextViewUsername;
    TextView mTextViewCategory;
    CircleImageView mCircleImageBack;
    ImageView mImageViewShare;
    TextInputEditText mTextInputComment;
    ImageView mImageViewSend;
    RecyclerView mRecyclerViewComments;

    CommentsProvider mCommentsProvider;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    NotificationsProvider mNotificationsProvider;
    PostProvider mPostProvider;
    LikesProvider mLikesProvider;
    ImageView mImageViewLike;
    LinearLayout mLayoutReplyIndicator;
    TextView mTextViewReplyIndicator;
    String mReplyingToCommentId = null;
    CommentsAdapter mCommentsAdapter;
    List<Comment> mComments = new ArrayList<>();
    ListenerRegistration mCommentsListener;
    ListenerRegistration mNotificationsListener;
    ListenerRegistration mPostListener;
    TextView mTextViewLikeCount;
    View mRootView;
    boolean mInitialNotifLoad = true;

    String mIdPost;
    String mIdUser;
    String mImage1;
    String mImage2;
    String mTitle;
    String mDescription;
    String mCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        mIdPost       = getIntent().getStringExtra("idPost");
        mIdUser       = getIntent().getStringExtra("idUser");
        mImage1       = getIntent().getStringExtra("image1");
        mImage2       = getIntent().getStringExtra("image2");
        mTitle        = getIntent().getStringExtra("title");
        mDescription  = getIntent().getStringExtra("description");
        mCategory     = getIntent().getStringExtra("category");

        mImageViewDetail1    = findViewById(R.id.imageViewDetail1);
        mImageViewDetail2    = findViewById(R.id.imageViewDetail2);
        mTextViewTitle       = findViewById(R.id.textViewDetailTitle);
        mTextViewDescription = findViewById(R.id.textViewDetailDescription);
        mTextViewUsername    = findViewById(R.id.textViewDetailUsername);
        mTextViewCategory    = findViewById(R.id.textViewDetailCategory);
        mCircleImageBack     = findViewById(R.id.circleImageBack);
        mImageViewShare      = findViewById(R.id.imageViewShare);
        mTextInputComment    = findViewById(R.id.textInputComment);
        mImageViewSend       = findViewById(R.id.imageViewSendComment);
        mRecyclerViewComments = findViewById(R.id.recyclerViewComments);

        mCommentsProvider      = new CommentsProvider();
        mAuthProvider          = new AuthProvider();
        mUsersProvider         = new UsersProvider();
        mNotificationsProvider = new NotificationsProvider();
        mPostProvider          = new PostProvider();
        mLikesProvider         = new LikesProvider();

        mRootView = findViewById(R.id.postDetailRoot);
        mTextViewLikeCount      = findViewById(R.id.textViewDetailLikeCount);
        mImageViewLike          = findViewById(R.id.imageViewDetailLike);
        mLayoutReplyIndicator   = findViewById(R.id.layoutReplyIndicator);
        mTextViewReplyIndicator = findViewById(R.id.textViewReplyIndicator);
        TextView cancelReply    = findViewById(R.id.textViewCancelReply);
        cancelReply.setOnClickListener(v -> cancelReplyMode());
        mRecyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        mCommentsAdapter = new CommentsAdapter(mComments, this);
        mCommentsAdapter.setOnReplyClickListener((commentId, username) -> enterReplyMode(commentId, username));
        mRecyclerViewComments.setAdapter(mCommentsAdapter);

        mTextViewTitle.setText(mTitle);
        mTextViewDescription.setText(mDescription);
        mTextViewCategory.setText(mCategory);

        if (mImage1 != null && !mImage1.isEmpty()) Picasso.get().load(mImage1).into(mImageViewDetail1);
        if (mImage2 != null && !mImage2.isEmpty()) Picasso.get().load(mImage2).into(mImageViewDetail2);

        mUsersProvider.getUser(mIdUser).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                if (username != null) mTextViewUsername.setText("@" + username);
            }
        });

        mTextViewUsername.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("idUser", mIdUser);
            startActivity(intent);
        });

        mCircleImageBack.setOnClickListener(v -> finish());
        mImageViewSend.setOnClickListener(v -> sendComment());
        mImageViewShare.setOnClickListener(v -> sharePost());

        loadComments();
        listenForNewNotifications();
        listenForLikeCount();
        setupLikeButton();
    }

    private void setupLikeButton() {
        String myId = mAuthProvider.getUid();
        if (myId == null) return;

        mLikesProvider.getLike(myId, mIdPost).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                mImageViewLike.setImageResource(R.drawable.ic_heart);
                mImageViewLike.setColorFilter(android.graphics.Color.parseColor("#FF003C"));
            } else {
                mImageViewLike.setImageResource(R.drawable.ic_heart_outline);
                mImageViewLike.clearColorFilter();
            }
        });

        mImageViewLike.setOnClickListener(v -> {
            mLikesProvider.getLike(myId, mIdPost).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    mLikesProvider.unlike(myId, mIdPost);
                    mPostProvider.decrementLikeCount(mIdPost);
                    mImageViewLike.setImageResource(R.drawable.ic_heart_outline);
                    mImageViewLike.clearColorFilter();
                } else {
                    mLikesProvider.like(myId, mIdPost);
                    mPostProvider.incrementLikeCount(mIdPost);
                    mImageViewLike.setImageResource(R.drawable.ic_heart);
                    mImageViewLike.setColorFilter(android.graphics.Color.parseColor("#FF003C"));

                    if (!myId.equals(mIdUser)) {
                        sendLikeNotification(myId);
                    }
                }
            });
        });
    }

    private void sendLikeNotification(String myId) {
        mUsersProvider.getUser(myId).addOnSuccessListener(doc -> {
            String username = doc.exists() && doc.getString("username") != null
                    ? doc.getString("username") : "Alguien";
            String body = "@" + username + " le dio like a tu publicación";

            Notification notif = new Notification();
            notif.setType("like");
            notif.setIdFrom(myId);
            notif.setIdTo(mIdUser);
            notif.setIdPost(mIdPost);
            notif.setBody(body);
            notif.setRead(false);
            notif.setTimestamp(new Date().getTime());
            mNotificationsProvider.save(notif);

            mUsersProvider.getUser(mIdUser).addOnSuccessListener(ownerDoc -> {
                String fcmToken = ownerDoc.getString("fcmToken");
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    FCMSender.send(getApplicationContext(), fcmToken, "Nuevo like 🔥", body);
                }
            });
        });
    }

    private void listenForLikeCount() {
        mPostListener = mPostProvider.getById(mIdPost).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) return;
            long count = doc.getLong("likeCount") != null ? doc.getLong("likeCount") : 0;
            mTextViewLikeCount.setText(count + (count == 1 ? " like" : " likes"));
        });
    }

    private void loadComments() {
        mCommentsListener = mCommentsProvider.getCommentsByPost(mIdPost)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        String docId = dc.getDocument().getId();
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Comment comment = dc.getDocument().toObject(Comment.class);
                            comment.setId(docId);
                            boolean exists = false;
                            for (Comment c : mComments) {
                                if (docId.equals(c.getId())) { exists = true; break; }
                            }
                            if (!exists) mComments.add(comment);
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            mComments.removeIf(c -> docId.equals(c.getId()));
                        }
                    }

                    Collections.sort(mComments, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                    mCommentsAdapter.notifyDataSetChanged();
                    if (!mComments.isEmpty()) {
                        mRecyclerViewComments.scrollToPosition(mComments.size() - 1);
                    }
                });
    }

    private void sharePost() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "🎮 " + mTitle + "\n\n" + mDescription + "\n\n[Compartido desde SocialMediaGamer]");
        startActivity(Intent.createChooser(shareIntent, "Compartir publicación"));
    }

    private void enterReplyMode(String commentId, String username) {
        mReplyingToCommentId = commentId;
        mTextViewReplyIndicator.setText("Respondiendo a " + username);
        mLayoutReplyIndicator.setVisibility(View.VISIBLE);
        mTextInputComment.requestFocus();
    }

    private void cancelReplyMode() {
        mReplyingToCommentId = null;
        mLayoutReplyIndicator.setVisibility(View.GONE);
        mTextInputComment.setText("");
    }

    private void sendComment() {
        if (com.optic.socialmediagamer.utils.GuestGuard.check(this)) return;
        String commentText = mTextInputComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show();
            return;
        }

        String myId = mAuthProvider.getUid();
        Comment comment = new Comment();
        comment.setComment(commentText);
        comment.setIdUser(myId);
        comment.setIdPost(mIdPost);
        comment.setTimestamp(new Date().getTime());

        if (mReplyingToCommentId != null) {
            String parentId = mReplyingToCommentId;
            mCommentsProvider.saveReply(parentId, comment).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    cancelReplyMode();
                    mCommentsAdapter.expandReplies(parentId);
                } else {
                    Toast.makeText(this, "No se pudo enviar la respuesta", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            mCommentsProvider.save(comment).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    mTextInputComment.setText("");
                    if (myId != null) new MissionsProvider().incrementProgress(myId, MissionsProvider.TYPE_COMMENT);
                    if (myId != null && !myId.equals(mIdUser)) {
                        sendCommentNotification(myId, commentText);
                    }
                } else {
                    Toast.makeText(this, "No se pudo enviar el comentario", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void listenForNewNotifications() {
        String myId = mAuthProvider.getUid();
        if (myId == null || !myId.equals(mIdUser)) return;

        mNotificationsListener = mNotificationsProvider.getNotificationsByUser(myId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    if (mInitialNotifLoad) {
                        mInitialNotifLoad = false;
                        return;
                    }
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;
                        String idPost = dc.getDocument().getString("idPost");
                        if (!mIdPost.equals(idPost)) continue;
                        String body = dc.getDocument().getString("body");
                        if (body != null) {
                            Snackbar.make(mRootView, body, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendCommentNotification(String myId, String commentText) {
        mUsersProvider.getUser(myId).addOnSuccessListener(doc -> {
            String myUsername = doc.exists() && doc.getString("username") != null
                    ? doc.getString("username") : "Alguien";
            String body = "@" + myUsername + " comentó: " + commentText;

            Notification notif = new Notification();
            notif.setType("comment");
            notif.setIdFrom(myId);
            notif.setIdTo(mIdUser);
            notif.setIdPost(mIdPost);
            notif.setBody(body);
            notif.setRead(false);
            notif.setTimestamp(new Date().getTime());
            mNotificationsProvider.save(notif);

            mUsersProvider.getUser(mIdUser).addOnSuccessListener(ownerDoc -> {
                String fcmToken = ownerDoc.getString("fcmToken");
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    FCMSender.send(getApplicationContext(), fcmToken, "Nuevo comentario 💬", body);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCommentsListener != null) mCommentsListener.remove();
        if (mNotificationsListener != null) mNotificationsListener.remove();
        if (mPostListener != null) mPostListener.remove();
        if (mCommentsAdapter != null) mCommentsAdapter.releaseListeners();
    }
}
