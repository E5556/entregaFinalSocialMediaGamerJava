package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.models.Season;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SeasonProvider {

    private final CollectionReference mCollection;
    private final CollectionReference mUsers;

    public SeasonProvider() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mCollection = db.collection("seasons");
        mUsers = db.collection("Users");
    }

    /** Top 10 users by seasonXp for the current month. */
    public Query getSeasonLeaderboard() {
        return mUsers.orderBy("seasonXp", Query.Direction.DESCENDING).limit(10);
    }

    /** All past seasons ordered by closedAt desc. */
    public Task<QuerySnapshot> getPastSeasons() {
        return mCollection.orderBy("closedAt", Query.Direction.DESCENDING).get();
    }

    /** Saves the season champion and awards the badge. Call after determining winner externally. */
    public Task<Void> closeSeason(String championId, String championUsername, long championSeasonXp) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -0); // current month
        String seasonId = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        String label    = new SimpleDateFormat("MMMM yyyy", new Locale("es")).format(new Date());

        Season season = new Season();
        season.setId(seasonId);
        season.setLabel(capitalize(label));
        season.setChampionId(championId);
        season.setChampionUsername(championUsername);
        season.setSeasonXp(championSeasonXp);
        season.setClosedAt(System.currentTimeMillis());

        return mCollection.document(seasonId).set(season);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
