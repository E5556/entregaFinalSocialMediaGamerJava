package com.optic.socialmediagamer.providers;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class XPProvider {

    private static final String TAG = "XPProvider";
    private static final long MAX_XP_PER_ACTION = 1000;
    private final FirebaseFirestore mFirestore;

    public XPProvider() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    public Task<Void> addXP(String userId, long amount) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "addXP ignorado: userId nulo o vacío");
            return Tasks.forException(new IllegalArgumentException("userId no puede ser nulo"));
        }
        if (amount <= 0 || amount > MAX_XP_PER_ACTION) {
            Log.w(TAG, "addXP ignorado: amount inválido = " + amount);
            return Tasks.forException(new IllegalArgumentException("amount debe estar entre 1 y " + MAX_XP_PER_ACTION));
        }

        long currentWeekStart = getWeekStart();

        return mFirestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference ref =
                    mFirestore.collection("Users").document(userId);
            com.google.firebase.firestore.DocumentSnapshot snap = transaction.get(ref);

            long weeklyXpResetAt = snap.getLong("weeklyXpResetAt") != null
                    ? snap.getLong("weeklyXpResetAt") : 0L;

            Map<String, Object> update = new HashMap<>();
            update.put("xp", FieldValue.increment(amount));

            if (weeklyXpResetAt < currentWeekStart) {
                // Nueva semana: reiniciar weeklyXp
                update.put("weeklyXp", amount);
                update.put("weeklyXpResetAt", currentWeekStart);
            } else {
                update.put("weeklyXp", FieldValue.increment(amount));
            }

            transaction.update(ref, update);
            return null;
        });
    }

    private long getWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
