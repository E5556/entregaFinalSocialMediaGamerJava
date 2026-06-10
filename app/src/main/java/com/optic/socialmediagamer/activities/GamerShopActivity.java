package com.optic.socialmediagamer.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.ShopItem;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ShopProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GamerShopActivity extends AppCompatActivity {

    Toolbar mToolbar;
    RecyclerView mRecyclerView;
    TextView mTextViewXP;

    AuthProvider mAuthProvider;
    ShopProvider mShopProvider;
    UsersProvider mUsersProvider;

    long mCurrentXp = 0;
    String mActiveFrame = "";
    String mActiveTitle = "";
    final Set<String> mOwnedIds = new HashSet<>();
    List<ShopItem> mCatalog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamer_shop);

        mToolbar = findViewById(R.id.toolbarShop);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());
        mToolbar.setTitle("🛒 Tienda Gamer");

        mTextViewXP   = findViewById(R.id.textViewShopXP);
        mRecyclerView = findViewById(R.id.recyclerViewShop);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAuthProvider  = new AuthProvider();
        mShopProvider  = new ShopProvider();
        mUsersProvider = new UsersProvider();
        mCatalog       = ShopItem.getCatalog();

        loadData();
    }

    private void loadData() {
        String uid = mAuthProvider.getUid();
        mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
            mCurrentXp   = doc.getLong("xp") != null ? doc.getLong("xp") : 0L;
            mActiveFrame = doc.getString("activeFrame") != null ? doc.getString("activeFrame") : "";
            mActiveTitle = doc.getString("activeTitle") != null ? doc.getString("activeTitle") : "";
            mTextViewXP.setText(mCurrentXp + " XP");

            mShopProvider.getOwnedItems(uid).addOnSuccessListener(snap -> {
                for (DocumentSnapshot d : snap.getDocuments()) mOwnedIds.add(d.getId());
                mRecyclerView.setAdapter(new ShopAdapter());
            });
        });
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ShopItem item = mCatalog.get(position);
            holder.textEmoji.setText(item.getEmoji());
            holder.textName.setText(item.getName());
            holder.textDesc.setText(item.getDescription());
            holder.textType.setText(typeLabel(item.getType()));

            boolean owned = mOwnedIds.contains(item.getId());
            boolean equipped = isEquipped(item);

            if (equipped) {
                holder.btnAction.setText("✓ EQUIPADO");
                holder.btnAction.setBackgroundTintList(
                        getColorStateList(R.color.color_text_secondary));
                holder.btnAction.setEnabled(false);
            } else if (owned) {
                holder.btnAction.setText("EQUIPAR");
                holder.btnAction.setBackgroundTintList(
                        getColorStateList(R.color.color_secondary));
                holder.btnAction.setEnabled(true);
                holder.btnAction.setOnClickListener(v -> equip(item, holder));
            } else {
                holder.btnAction.setText(item.getXpCost() + " XP");
                holder.btnAction.setBackgroundTintList(
                        mCurrentXp >= item.getXpCost()
                                ? getColorStateList(R.color.color_primary)
                                : getColorStateList(R.color.color_text_secondary));
                holder.btnAction.setEnabled(mCurrentXp >= item.getXpCost());
                holder.btnAction.setOnClickListener(v -> purchase(item, holder));
            }
        }

        @Override
        public int getItemCount() { return mCatalog.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView textEmoji, textName, textDesc, textType;
            Button btnAction;
            VH(@NonNull View v) {
                super(v);
                textEmoji = v.findViewById(R.id.textViewShopEmoji);
                textName  = v.findViewById(R.id.textViewShopName);
                textDesc  = v.findViewById(R.id.textViewShopDescription);
                textType  = v.findViewById(R.id.textViewShopType);
                btnAction = v.findViewById(R.id.buttonShopAction);
            }
        }
    }

    private boolean isEquipped(ShopItem item) {
        switch (item.getType()) {
            case ShopItem.TYPE_FRAME:      return item.getId().equals(mActiveFrame);
            case ShopItem.TYPE_TITLE:      return item.getId().equals(mActiveTitle);
            case ShopItem.TYPE_BACKGROUND: return item.getId().equals(
                    mUsersProvider == null ? "" : "");
            default: return false;
        }
    }

    private void purchase(ShopItem item, ShopAdapter.VH holder) {
        String uid = mAuthProvider.getUid();
        mShopProvider.purchase(uid, item).addOnSuccessListener(v -> {
            mCurrentXp -= item.getXpCost();
            mTextViewXP.setText(mCurrentXp + " XP");
            mOwnedIds.add(item.getId());
            Toast.makeText(this, "¡" + item.getName() + " desbloqueado! 🎉", Toast.LENGTH_SHORT).show();
            equip(item, holder);
        }).addOnFailureListener(e ->
            Toast.makeText(this, "XP insuficiente", Toast.LENGTH_SHORT).show());
    }

    private void equip(ShopItem item, ShopAdapter.VH holder) {
        String uid = mAuthProvider.getUid();
        String field;
        switch (item.getType()) {
            case ShopItem.TYPE_FRAME:      field = "activeFrame";      mActiveFrame = item.getId(); break;
            case ShopItem.TYPE_TITLE:      field = "activeTitle";      mActiveTitle = item.getId(); break;
            case ShopItem.TYPE_BACKGROUND: field = "activeBackground"; break;
            default: return;
        }
        mUsersProvider.setActiveCosmetic(uid, item.getType(), item.getId())
            .addOnSuccessListener(v -> {
                Toast.makeText(this, item.getName() + " equipado ✓", Toast.LENGTH_SHORT).show();
                if (mRecyclerView.getAdapter() != null) mRecyclerView.getAdapter().notifyDataSetChanged();
            });
    }

    private String typeLabel(String type) {
        switch (type) {
            case ShopItem.TYPE_FRAME:      return "MARCO DE PERFIL";
            case ShopItem.TYPE_TITLE:      return "TÍTULO EN POSTS";
            case ShopItem.TYPE_BACKGROUND: return "FONDO DE PERFIL";
            default: return type;
        }
    }
}
