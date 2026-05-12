package com.optic.socialmediagamer.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.PostDetailActivity;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.CommentsProvider;
import com.optic.socialmediagamer.providers.LikesProvider;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.models.Poll;
import com.optic.socialmediagamer.providers.PollProvider;
import com.optic.socialmediagamer.providers.ReactionsProvider;
import com.optic.socialmediagamer.providers.TwitchProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.utils.FCMSender;
import com.optic.socialmediagamer.utils.RankHelper;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.ViewHolder> {

    private static final String[] EMOJIS = {"👍", "❤️", "😂", "😮", "😢", "🔥"};

    Context context;
    LikesProvider mLikesProvider;
    CommentsProvider mCommentsProvider;
    AuthProvider mAuthProvider;
    NotificationsProvider mNotificationsProvider;
    UsersProvider mUsersProvider;
    ReactionsProvider mReactionsProvider;
    PostProvider mPostProvider;
    XPProvider mXPProvider;
    PollProvider mPollProvider;

    private final Map<String, ListenerRegistration> mReactionListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> mPollListeners = new HashMap<>();

    public PostsAdapter(FirestoreRecyclerOptions<Post> options, Context context) {
        super(options);
        this.context = context;
        mLikesProvider         = new LikesProvider();
        mCommentsProvider      = new CommentsProvider();
        mAuthProvider          = new AuthProvider();
        mNotificationsProvider = new NotificationsProvider();
        mUsersProvider         = new UsersProvider();
        mReactionsProvider     = new ReactionsProvider();
        mPostProvider          = new PostProvider();
        mXPProvider            = new XPProvider();
        mPollProvider          = new PollProvider();
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Post post) {
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        String idPost = snapshot.getId();

        holder.textViewTitle.setText(post.getTitle());
        holder.textViewDescription.setText(post.getDescription());
        if (post.getImage1() != null && !post.getImage1().isEmpty()) {
            Picasso.get().load(post.getImage1()).into(holder.imageViewPost);
        }

        holder.textViewAuthorUsername.setText("@gamer");
        holder.textViewAuthorRank.setText("🥉");
        holder.circleImageAuthor.setImageResource(R.drawable.ic_person);
        if (post.getIdUser() != null) {
            mUsersProvider.getUser(post.getIdUser()).addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String username = userDoc.getString("username");
                    if (username != null) holder.textViewAuthorUsername.setText("@" + username);
                    String imageProfile = userDoc.getString("image_profile");
                    if (imageProfile != null && !imageProfile.isEmpty()) {
                        Picasso.get().load(imageProfile).placeholder(R.drawable.ic_person).into(holder.circleImageAuthor);
                    }
                    long xp = userDoc.getLong("xp") != null ? userDoc.getLong("xp") : 0L;
                    holder.textViewAuthorRank.setText(RankHelper.getRankEmoji(xp));

                    String nowPlaying = userDoc.getString("nowPlaying");
                    if (nowPlaying != null && !nowPlaying.isEmpty()) {
                        holder.textViewNowPlaying.setText("🎮 " + nowPlaying);
                        holder.textViewNowPlaying.setVisibility(View.VISIBLE);
                    } else {
                        holder.textViewNowPlaying.setVisibility(View.GONE);
                    }

                    String twitch = userDoc.getString("twitchUsername");
                    if (twitch != null && !twitch.isEmpty()) {
                        new TwitchProvider().checkIfLive(twitch, (isLive, viewers) ->
                            holder.textViewLiveDot.setVisibility(isLive ? View.VISIBLE : View.GONE));
                    }
                }
            });
        }

        mLikesProvider.getLikesByPost(idPost).get().addOnSuccessListener(snap ->
            holder.textViewLikeCount.setText(String.valueOf(snap.size())));

        mCommentsProvider.getCommentsByPost(idPost).get().addOnSuccessListener(snap ->
            holder.textViewCommentCount.setText(String.valueOf(snap.size())));

        // Poll en tiempo real
        if (post.isHasPoll()) {
            holder.layoutPollCard.setVisibility(View.VISIBLE);
            bindPoll(idPost, holder);
        } else {
            holder.layoutPollCard.setVisibility(View.GONE);
        }

        // Reacciones en tiempo real
        bindReactions(idPost, holder);

        String myId = mAuthProvider.getUid();
        if (myId != null) {
            mLikesProvider.getLike(myId, idPost).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    holder.imageViewLike.setImageResource(R.drawable.ic_heart);
                } else {
                    holder.imageViewLike.setImageResource(R.drawable.ic_heart_outline);
                }
            });

            holder.imageViewLike.setOnClickListener(v -> {
                mLikesProvider.getLike(myId, idPost).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        mLikesProvider.unlike(myId, idPost).addOnSuccessListener(u -> {
                            holder.imageViewLike.setImageResource(R.drawable.ic_heart_outline);
                            mPostProvider.decrementLikeCount(idPost);
                            mLikesProvider.getLikesByPost(idPost).get().addOnSuccessListener(snap ->
                                holder.textViewLikeCount.setText(String.valueOf(snap.size())));
                        });
                    } else {
                        mLikesProvider.like(myId, idPost).addOnSuccessListener(u -> {
                            holder.imageViewLike.setImageResource(R.drawable.ic_heart);
                            mPostProvider.incrementLikeCount(idPost);
                            mXPProvider.addXP(myId, RankHelper.XP_LIKE_GIVEN);
                            String postOwnerId2 = post.getIdUser();
                            if (postOwnerId2 != null && !myId.equals(postOwnerId2)) {
                                mXPProvider.addXP(postOwnerId2, RankHelper.XP_LIKE_RECEIVED);
                            }
                            mLikesProvider.getLikesByPost(idPost).get().addOnSuccessListener(snap ->
                                holder.textViewLikeCount.setText(String.valueOf(snap.size())));
                            String postOwnerId = post.getIdUser();
                            if (!myId.equals(postOwnerId)) {
                                mUsersProvider.getUser(myId).addOnSuccessListener(userDoc -> {
                                    String username = userDoc.exists() && userDoc.getString("username") != null
                                            ? userDoc.getString("username") : "Alguien";
                                    Notification notif = new Notification();
                                    notif.setType("like");
                                    notif.setIdFrom(myId);
                                    notif.setIdTo(postOwnerId);
                                    notif.setIdPost(idPost);
                                    notif.setBody("@" + username + " le dio like a tu publicación");
                                    notif.setRead(false);
                                    notif.setTimestamp(new Date().getTime());
                                    mNotificationsProvider.save(notif);
                                    mUsersProvider.getUser(postOwnerId).addOnSuccessListener(ownerDoc -> {
                                        String fcmToken = ownerDoc.getString("fcmToken");
                                        if (fcmToken != null && !fcmToken.isEmpty()) {
                                            FCMSender.send(context, fcmToken, "Nuevo like 🔥", "@" + username + " le dio like a tu publicación");
                                        }
                                    });
                                });
                            }
                        });
                    }
                });
            });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("idPost", idPost);
            intent.putExtra("idUser", post.getIdUser());
            intent.putExtra("image1", post.getImage1());
            intent.putExtra("image2", post.getImage2());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("category", post.getCategory());
            context.startActivity(intent);
        });
    }

    private void bindPoll(String idPost, ViewHolder holder) {
        ListenerRegistration prev = mPollListeners.get(idPost);
        if (prev != null) prev.remove();

        ListenerRegistration reg = mPollProvider.listen(idPost, (snap, e) -> {
            if (snap == null || !snap.exists()) return;
            Poll poll = snap.toObject(Poll.class);
            if (poll == null) return;

            String myId = mAuthProvider.getUid();
            java.util.List<String> votesA = poll.getVotesA();
            java.util.List<String> votesB = poll.getVotesB();
            int countA = votesA.size();
            int countB = votesB.size();
            int total  = countA + countB;

            holder.textViewPollOptionA.setText(poll.getOptionA());
            holder.textViewPollOptionB.setText(poll.getOptionB());
            holder.textViewPollTotalVotes.setText(total + " votos");

            if (total > 0) {
                int pctA = (int) (countA * 100f / total);
                int pctB = 100 - pctA;
                holder.progressBarA.setProgress(pctA);
                holder.progressBarB.setProgress(pctB);
                holder.textViewPollPercentA.setText(pctA + "% (" + countA + ")");
                holder.textViewPollPercentB.setText(pctB + "% (" + countB + ")");
            } else {
                holder.progressBarA.setProgress(50);
                holder.progressBarB.setProgress(50);
                holder.textViewPollPercentA.setText("0%");
                holder.textViewPollPercentB.setText("0%");
            }

            boolean votedA = myId != null && votesA.contains(myId);
            boolean votedB = myId != null && votesB.contains(myId);

            holder.btnVoteA.setAlpha(votedA ? 1f : 0.5f);
            holder.btnVoteB.setAlpha(votedB ? 1f : 0.5f);

            holder.btnVoteA.setOnClickListener(v -> {
                if (myId == null) return;
                if (votedA) {
                    mPollProvider.removeVoteA(idPost, myId);
                } else {
                    if (votedB) mPollProvider.removeVoteB(idPost, myId);
                    mPollProvider.voteA(idPost, myId);
                }
            });
            holder.btnVoteB.setOnClickListener(v -> {
                if (myId == null) return;
                if (votedB) {
                    mPollProvider.removeVoteB(idPost, myId);
                } else {
                    if (votedA) mPollProvider.removeVoteA(idPost, myId);
                    mPollProvider.voteB(idPost, myId);
                }
            });
        });

        mPollListeners.put(idPost, reg);
    }

    private void bindReactions(String idPost, ViewHolder holder) {
        // Cancelar listener anterior si este ViewHolder se reutiliza
        ListenerRegistration prev = mReactionListeners.get(idPost);
        if (prev != null) prev.remove();

        ListenerRegistration reg = mReactionsProvider.listenReactionsByPost(idPost, (snap, e) -> {
            if (snap == null) return;
            String myId = mAuthProvider.getUid();

            // Contar por emoji
            Map<String, Integer> counts = new HashMap<>();
            Map<String, Boolean> myReactions = new HashMap<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String type = doc.getString("type");
                if (type == null) continue;
                Integer prev2 = counts.get(type);
                counts.put(type, prev2 != null ? prev2 + 1 : 1);
                if (myId != null && myId.equals(doc.getString("idUser"))) {
                    myReactions.put(type, true);
                }
            }

            // Construir resumen "👍2 ❤️1 🔥3"
            StringBuilder sb = new StringBuilder();
            for (String emoji : EMOJIS) {
                Integer count = counts.get(emoji);
                if (count != null && count > 0) {
                    if (myReactions.containsKey(emoji)) {
                        sb.append("[").append(emoji).append(count).append("] ");
                    } else {
                        sb.append(emoji).append(count).append(" ");
                    }
                }
            }

            if (sb.length() > 0) {
                holder.textViewReactionsSummary.setText(sb.toString().trim());
                holder.textViewReactionsSummary.setVisibility(View.VISIBLE);
            } else {
                holder.textViewReactionsSummary.setVisibility(View.GONE);
            }

            // Botón para abrir picker
            holder.textViewAddReaction.setOnClickListener(v ->
                showEmojiPicker(v, idPost, myId, myReactions));
            holder.textViewReactionsSummary.setOnClickListener(v ->
                showEmojiPicker(v, idPost, myId, myReactions));
        });

        mReactionListeners.put(idPost, reg);
        holder.boundPostId = idPost;
    }

    private void showEmojiPicker(View anchor, String idPost, String myId, Map<String, Boolean> myReactions) {
        if (myId == null) return;

        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_emoji_picker, null);
        PopupWindow popup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setElevation(8f);

        LinearLayout container = popupView.findViewById(R.id.emojiContainer);
        for (String emoji : EMOJIS) {
            TextView tv = new TextView(context);
            tv.setText(emoji);
            tv.setTextSize(22f);
            tv.setPadding(16, 8, 16, 8);
            tv.setAlpha(myReactions.containsKey(emoji) ? 1f : 0.5f);
            tv.setOnClickListener(v -> {
                popup.dismiss();
                toggleReaction(idPost, myId, emoji, myReactions.containsKey(emoji));
            });
            container.addView(tv);
        }

        popup.showAsDropDown(anchor, 0, -anchor.getHeight() - 60, Gravity.START);
    }

    private void toggleReaction(String idPost, String myId, String emoji, boolean alreadyReacted) {
        if (alreadyReacted) {
            mReactionsProvider.unreact(myId, idPost, emoji);
        } else {
            mReactionsProvider.react(myId, idPost, emoji);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.boundPostId != null) {
            ListenerRegistration reg = mReactionListeners.remove(holder.boundPostId);
            if (reg != null) reg.remove();
            ListenerRegistration pollReg = mPollListeners.remove(holder.boundPostId);
            if (pollReg != null) pollReg.remove();
            holder.boundPostId = null;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_post, parent, false);
        return new ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewDescription;
        ImageView imageViewPost;
        ImageView imageViewLike;
        TextView textViewLikeCount;
        TextView textViewCommentCount;
        CircleImageView circleImageAuthor;
        TextView textViewAuthorUsername;
        TextView textViewAuthorRank;
        TextView textViewNowPlaying;
        TextView textViewLiveDot;
        TextView textViewAddReaction;
        TextView textViewReactionsSummary;
        LinearLayout layoutPollCard;
        TextView textViewPollOptionA;
        TextView textViewPollOptionB;
        ProgressBar progressBarA;
        ProgressBar progressBarB;
        TextView textViewPollPercentA;
        TextView textViewPollPercentB;
        TextView btnVoteA;
        TextView btnVoteB;
        TextView textViewPollTotalVotes;
        String boundPostId;

        public ViewHolder(View view) {
            super(view);
            textViewTitle           = view.findViewById(R.id.textViewTitlePostCard);
            textViewDescription     = view.findViewById(R.id.textViewDescriptionPostCard);
            imageViewPost           = view.findViewById(R.id.imageViewPostCard);
            imageViewLike           = view.findViewById(R.id.imageViewLike);
            textViewLikeCount       = view.findViewById(R.id.textViewLikeCount);
            textViewCommentCount    = view.findViewById(R.id.textViewCommentCount);
            circleImageAuthor       = view.findViewById(R.id.circleImageAuthor);
            textViewAuthorUsername  = view.findViewById(R.id.textViewAuthorUsername);
            textViewAuthorRank      = view.findViewById(R.id.textViewAuthorRank);
            textViewNowPlaying      = view.findViewById(R.id.textViewNowPlaying);
            textViewLiveDot         = view.findViewById(R.id.textViewLiveDot);
            textViewAddReaction      = view.findViewById(R.id.textViewAddReaction);
            textViewReactionsSummary = view.findViewById(R.id.textViewReactionsSummary);
            layoutPollCard           = view.findViewById(R.id.layoutPollCard);
            textViewPollOptionA      = view.findViewById(R.id.textViewPollOptionA);
            textViewPollOptionB      = view.findViewById(R.id.textViewPollOptionB);
            progressBarA             = view.findViewById(R.id.progressBarA);
            progressBarB             = view.findViewById(R.id.progressBarB);
            textViewPollPercentA     = view.findViewById(R.id.textViewPollPercentA);
            textViewPollPercentB     = view.findViewById(R.id.textViewPollPercentB);
            btnVoteA                 = view.findViewById(R.id.btnVoteA);
            btnVoteB                 = view.findViewById(R.id.btnVoteB);
            textViewPollTotalVotes   = view.findViewById(R.id.textViewPollTotalVotes);
        }
    }
}
