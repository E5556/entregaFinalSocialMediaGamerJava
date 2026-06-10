package com.optic.socialmediagamer.providers;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.socialmediagamer.models.ShopItem;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ShopProvider {

    private final FirebaseFirestore mFirestore;

    public ShopProvider() {
        mFirestore = FirebaseFirestore.getInstance();
    }

    private CollectionReference items(String userId) {
        return mFirestore.collection("shop_purchases").document(userId).collection("items");
    }

    public Task<DocumentSnapshot> hasPurchased(String userId, String itemId) {
        return items(userId).document(itemId).get();
    }

    public Task<QuerySnapshot> getOwnedItems(String userId) {
        return items(userId).get();
    }

    /** Deducts XP and records purchase in a Firestore transaction. */
    public Task<Void> purchase(String userId, ShopItem item) {
        return mFirestore.runTransaction(tx -> {
            com.google.firebase.firestore.DocumentReference userRef =
                    mFirestore.collection("Users").document(userId);
            DocumentSnapshot snap = tx.get(userRef);
            long currentXp = snap.getLong("xp") != null ? snap.getLong("xp") : 0L;
            if (currentXp < item.getXpCost()) {
                throw new Exception("XP insuficiente");
            }

            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("itemId", item.getId());
            purchaseData.put("itemName", item.getName());
            purchaseData.put("xpCost", item.getXpCost());
            purchaseData.put("timestamp", new Date().getTime());

            tx.set(items(userId).document(item.getId()), purchaseData);
            tx.update(userRef, "xp", currentXp - item.getXpCost());
            return null;
        });
    }
}
