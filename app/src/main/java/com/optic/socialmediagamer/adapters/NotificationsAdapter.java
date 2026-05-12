package com.optic.socialmediagamer.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.PostDetailActivity;
import com.optic.socialmediagamer.activities.UserProfileActivity;
import com.optic.socialmediagamer.models.GroupedNotification;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.PostProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private final Context context;
    private final List<GroupedNotification> notifications;
    private final NotificationsProvider mNotificationsProvider;
    private final PostProvider mPostProvider;

    public NotificationsAdapter(List<GroupedNotification> notifications, Context context) {
        this.notifications = notifications;
        this.context = context;
        mNotificationsProvider = new NotificationsProvider();
        mPostProvider = new PostProvider();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupedNotification notif = notifications.get(position);

        holder.textViewBody.setText(notif.getBody());

        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(new Date(notif.getTimestamp()));
        holder.textViewTime.setText(time);

        holder.viewUnreadDot.setVisibility(notif.isRead() ? View.INVISIBLE : View.VISIBLE);

        if (notif.getCount() > 1) {
            holder.textViewGroupCount.setVisibility(View.VISIBLE);
            holder.textViewGroupCount.setText("+" + (notif.getCount() - 1));
        } else {
            holder.textViewGroupCount.setVisibility(View.GONE);
        }

        String type = notif.getType();
        if ("like".equals(type)) {
            holder.imageViewIcon.setImageResource(R.drawable.ic_heart);
            holder.imageViewIcon.setColorFilter(android.graphics.Color.parseColor("#FF003C"));
        } else if ("comment".equals(type)) {
            holder.imageViewIcon.setImageResource(R.drawable.ic_chat);
            holder.imageViewIcon.setColorFilter(android.graphics.Color.parseColor("#BF00FF"));
        } else if ("follow".equals(type)) {
            holder.imageViewIcon.setImageResource(R.drawable.ic_follow);
            holder.imageViewIcon.setColorFilter(android.graphics.Color.parseColor("#00F0FF"));
        } else if ("post".equals(type)) {
            holder.imageViewIcon.setImageResource(R.drawable.ic_videogame);
            holder.imageViewIcon.setColorFilter(android.graphics.Color.parseColor("#00F0FF"));
        } else {
            holder.imageViewIcon.setImageResource(R.drawable.ic_bell);
            holder.imageViewIcon.setColorFilter(android.graphics.Color.parseColor("#00F0FF"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (!notif.isRead()) {
                mNotificationsProvider.markMultipleAsRead(notif.getIds());
                notif.setRead(true);
                notifyItemChanged(position);
            }
            navigateToSource(notif);
        });
    }

    private void navigateToSource(GroupedNotification notif) {
        String type = notif.getType();
        String idPost = notif.getIdPost();
        String idFrom = notif.getIdFrom();

        if ("follow".equals(type)) {
            if (idFrom == null) return;
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("idUser", idFrom);
            context.startActivity(intent);
        } else if (idPost != null) {
            mPostProvider.getById(idPost).get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("idPost", doc.getId());
                intent.putExtra("idUser", doc.getString("idUser"));
                intent.putExtra("image1", doc.getString("image1"));
                intent.putExtra("image2", doc.getString("image2"));
                intent.putExtra("title", doc.getString("title"));
                intent.putExtra("description", doc.getString("description"));
                intent.putExtra("category", doc.getString("category"));
                context.startActivity(intent);
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() { return notifications.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewBody;
        TextView textViewTime;
        TextView textViewGroupCount;
        View viewUnreadDot;

        ViewHolder(View view) {
            super(view);
            imageViewIcon      = view.findViewById(R.id.imageViewNotifIcon);
            textViewBody       = view.findViewById(R.id.textViewNotifBody);
            textViewTime       = view.findViewById(R.id.textViewNotifTime);
            textViewGroupCount = view.findViewById(R.id.textViewGroupCount);
            viewUnreadDot      = view.findViewById(R.id.viewUnreadDot);
        }
    }
}
