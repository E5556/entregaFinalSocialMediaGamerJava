package com.optic.socialmediagamer.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.BadgeProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.ReputationProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamerCardActivity extends AppCompatActivity {

    ImageView mImageViewCard;
    Button mButtonShare;
    Bitmap mCardBitmap;

    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    PostProvider mPostProvider;
    BadgeProvider mBadgeProvider;
    ReputationProvider mReputationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamer_card);

        Toolbar toolbar = findViewById(R.id.toolbarGamerCard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mImageViewCard = findViewById(R.id.imageViewGamerCard);
        mButtonShare   = findViewById(R.id.buttonShareCard);
        mButtonShare.setEnabled(false);

        mAuthProvider      = new AuthProvider();
        mUsersProvider     = new UsersProvider();
        mPostProvider      = new PostProvider();
        mBadgeProvider     = new BadgeProvider();
        mReputationProvider = new ReputationProvider();

        mButtonShare.setOnClickListener(v -> shareCard());
        loadCardData();
    }

    private void loadCardData() {
        String uid = mAuthProvider.getUid();
        Tasks.whenAllSuccess(
                mUsersProvider.getUser(uid),
                mPostProvider.getPostByUser(uid).get(),
                mBadgeProvider.getBadgesByUser(uid).get(),
                mReputationProvider.getByUser(uid)
        ).addOnSuccessListener(results -> {
            DocumentSnapshot userDoc   = (DocumentSnapshot) results.get(0);
            com.google.firebase.firestore.QuerySnapshot postsSnap  =
                    (com.google.firebase.firestore.QuerySnapshot) results.get(1);
            com.google.firebase.firestore.QuerySnapshot badgesSnap =
                    (com.google.firebase.firestore.QuerySnapshot) results.get(2);
            com.google.firebase.firestore.QuerySnapshot ratingsSnap =
                    (com.google.firebase.firestore.QuerySnapshot) results.get(3);

            String username  = userDoc.getString("username") != null ? userDoc.getString("username") : "Gamer";
            long xp          = userDoc.getLong("xp") != null ? userDoc.getLong("xp") : 0L;
            long streak      = userDoc.getLong("currentStreak") != null ? userDoc.getLong("currentStreak") : 0L;
            String nowPlaying = userDoc.getString("nowPlaying");
            int badgeCount   = badgesSnap.size();
            int postCount    = postsSnap.size();

            // Favorite category
            String favCategory = getFavoriteCategory(postsSnap.getDocuments());

            // Gamer score average
            double gamerScore = 0;
            if (!ratingsSnap.isEmpty()) {
                double total = 0;
                for (DocumentSnapshot r : ratingsSnap.getDocuments()) {
                    Double comm  = r.getDouble("communication");
                    Double skill = r.getDouble("skill");
                    Double att   = r.getDouble("attitude");
                    if (comm != null && skill != null && att != null)
                        total += (comm + skill + att) / 3.0;
                }
                gamerScore = total / ratingsSnap.size();
            }

            mCardBitmap = drawCard(username, xp, streak, favCategory, nowPlaying, badgeCount, postCount, gamerScore);
            mImageViewCard.setImageBitmap(mCardBitmap);
            mButtonShare.setEnabled(true);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error generando card", Toast.LENGTH_SHORT).show());
    }

    private String getFavoriteCategory(List<DocumentSnapshot> posts) {
        Map<String, Integer> counts = new HashMap<>();
        for (DocumentSnapshot p : posts) {
            String cat = p.getString("category");
            if (cat != null) counts.put(cat, counts.getOrDefault(cat, 0) + 1);
        }
        String best = null;
        int max = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        }
        return best != null ? best : "—";
    }

    private Bitmap drawCard(String username, long xp, long streak,
                            String favGame, String nowPlaying,
                            int badges, int posts, double gamerScore) {

        int W = 960, H = 540;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        // Background gradient-like (two rects)
        Paint bg = new Paint();
        bg.setColor(Color.parseColor("#0D1117"));
        c.drawRect(0, 0, W, H, bg);

        Paint accent = new Paint();
        accent.setColor(Color.parseColor("#00E5FF"));
        accent.setAlpha(18);
        c.drawRect(0, 0, W / 2f, H, accent);

        // Top border line
        Paint line = new Paint();
        line.setColor(Color.parseColor("#00E5FF"));
        line.setStrokeWidth(4);
        c.drawLine(0, 0, W, 0, line);
        c.drawLine(0, H - 4, W, H - 4, line);

        // App label
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#00E5FF"));
        labelPaint.setTextSize(20);
        labelPaint.setTypeface(Typeface.MONOSPACE);
        labelPaint.setAlpha(180);
        c.drawText("SOCIALMEDIAGAMER  ·  GAMER CARD", 32, 36, labelPaint);

        // Rank emoji big
        Paint rankBig = new Paint(Paint.ANTI_ALIAS_FLAG);
        rankBig.setTextSize(72);
        rankBig.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(RankHelper.getRankEmoji(xp), 32, 130, rankBig);

        // Username
        Paint userPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        userPaint.setColor(Color.WHITE);
        userPaint.setTextSize(48);
        userPaint.setTypeface(Typeface.MONOSPACE);
        userPaint.setFakeBoldText(true);
        c.drawText(username, 32, 200, userPaint);

        // Rank name
        Paint rankName = new Paint(Paint.ANTI_ALIAS_FLAG);
        rankName.setColor(Color.parseColor("#00E5FF"));
        rankName.setTextSize(22);
        rankName.setTypeface(Typeface.MONOSPACE);
        c.drawText(RankHelper.getRankName(xp).toUpperCase() + "  ·  " + xp + " XP", 34, 236, rankName);

        // XP bar background
        Paint barBg = new Paint();
        barBg.setColor(Color.parseColor("#1E2A3A"));
        c.drawRoundRect(new RectF(32, 252, W - 32, 272), 8, 8, barBg);

        // XP bar fill
        int pct = RankHelper.getProgressPercent(xp);
        float fillW = (W - 64) * pct / 100f;
        Paint barFill = new Paint();
        barFill.setColor(Color.parseColor("#00E5FF"));
        if (fillW > 0) c.drawRoundRect(new RectF(32, 252, 32 + fillW, 272), 8, 8, barFill);

        // Stats section
        int statY = 320;
        drawStat(c, "🔥 RACHA", streak + " días", 32, statY);
        drawStat(c, "🎮 JUEGO FAV.", favGame, 280, statY);
        drawStat(c, "📝 POSTS", String.valueOf(posts), 32, statY + 80);
        drawStat(c, "🏅 BADGES", String.valueOf(badges), 280, statY + 80);

        // Gamer score
        String scoreStr = gamerScore > 0 ? String.format("%.1f ★", gamerScore) : "—";
        drawStat(c, "⭐ GAMER SCORE", scoreStr, 530, statY);

        // Now playing
        if (nowPlaying != null && !nowPlaying.isEmpty()) {
            drawStat(c, "▶ JUGANDO AHORA", nowPlaying, 530, statY + 80);
        }

        // Corner watermark
        Paint wm = new Paint(Paint.ANTI_ALIAS_FLAG);
        wm.setColor(Color.parseColor("#00E5FF"));
        wm.setAlpha(60);
        wm.setTextSize(14);
        wm.setTypeface(Typeface.MONOSPACE);
        c.drawText("socialmediagamer.app", W - 250, H - 18, wm);

        return bmp;
    }

    private void drawStat(Canvas c, String label, String value, int x, int y) {
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setColor(Color.parseColor("#8899AA"));
        lp.setTextSize(15);
        lp.setTypeface(Typeface.MONOSPACE);
        c.drawText(label, x, y, lp);

        Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
        vp.setColor(Color.WHITE);
        vp.setTextSize(22);
        vp.setTypeface(Typeface.MONOSPACE);
        vp.setFakeBoldText(true);
        c.drawText(value, x, y + 28, vp);
    }

    private void shareCard() {
        try {
            File cachePath = new File(getCacheDir(), "cards");
            cachePath.mkdirs();
            File file = new File(cachePath, "gamer_card.png");
            FileOutputStream fos = new FileOutputStream(file);
            mCardBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName(), file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, "¡Mi Gamer Card en SocialMediaGamer! 🎮");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartir Gamer Card"));
        } catch (Exception e) {
            Toast.makeText(this, "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }
}
