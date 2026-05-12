package com.optic.socialmediagamer.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Clan;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ClanProvider;

import java.util.Collections;

public class CreateClanActivity extends AppCompatActivity {

    EditText mEditTextName, mEditTextTag, mEditTextDescription;
    Button mButtonCreate;
    ClanProvider mClanProvider;
    AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_clan);

        Toolbar toolbar = findViewById(R.id.toolbarCreateClan);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mEditTextName        = findViewById(R.id.editTextClanName);
        mEditTextTag         = findViewById(R.id.editTextClanTag);
        mEditTextDescription = findViewById(R.id.editTextClanDescription);
        mButtonCreate        = findViewById(R.id.buttonCreateClan);

        mClanProvider = new ClanProvider();
        mAuthProvider = new AuthProvider();

        mButtonCreate.setOnClickListener(v -> createClan());
    }

    private void createClan() {
        String name = mEditTextName.getText().toString().trim();
        String tag  = mEditTextTag.getText().toString().trim().toUpperCase();
        String desc = mEditTextDescription.getText().toString().trim();

        if (name.isEmpty()) { Toast.makeText(this, "Escribe el nombre del clan", Toast.LENGTH_SHORT).show(); return; }
        if (tag.isEmpty())  { Toast.makeText(this, "Escribe el tag del clan", Toast.LENGTH_SHORT).show(); return; }

        String myId = mAuthProvider.getUid();
        Clan clan = new Clan();
        clan.setName(name);
        clan.setTag(tag);
        clan.setDescription(desc);
        clan.setIdLeader(myId);
        clan.setMembers(Collections.singletonList(myId));
        clan.setTimestamp(System.currentTimeMillis());

        mButtonCreate.setEnabled(false);
        mClanProvider.create(clan).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Clan creado", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al crear clan", Toast.LENGTH_SHORT).show();
            mButtonCreate.setEnabled(true);
        });
    }
}
