package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.concurrent.TimeUnit;

import dmax.dialog.SpotsDialog;

public class PhoneLoginActivity extends AppCompatActivity {

    private TextInputLayout mLayoutPhone, mLayoutCode;
    private TextInputEditText mEditPhone, mEditCode;
    private Button mBtnSendCode, mBtnVerify;
    private LinearLayout mLayoutCodeSection;
    private TextView mTextInfo;

    private FirebaseAuth mAuth;
    private AuthProvider mAuthProvider;
    private UsersProvider mUsersProvider;
    private AlertDialog mDialog;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        Toolbar toolbar = findViewById(R.id.toolbarPhoneLogin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        mLayoutPhone = findViewById(R.id.layoutPhone);
        mLayoutCode  = findViewById(R.id.layoutCode);
        mEditPhone   = findViewById(R.id.editPhone);
        mEditCode    = findViewById(R.id.editCode);
        mBtnSendCode = findViewById(R.id.btnSendCode);
        mBtnVerify   = findViewById(R.id.btnVerifyCode);
        mLayoutCodeSection = findViewById(R.id.layoutCodeSection);
        mTextInfo    = findViewById(R.id.textPhoneInfo);

        mAuth          = FirebaseAuth.getInstance();
        mAuthProvider  = new AuthProvider();
        mUsersProvider = new UsersProvider();

        mDialog = new SpotsDialog.Builder()
                .setContext(this).setMessage("ESPERE UN MOMENTO").setCancelable(false).build();

        mBtnSendCode.setOnClickListener(v -> sendVerificationCode());
        mBtnVerify.setOnClickListener(v -> verifyCode());
    }

    private void sendVerificationCode() {
        String phone = mEditPhone.getText() != null ? mEditPhone.getText().toString().trim() : "";
        mLayoutPhone.setError(null);

        if (TextUtils.isEmpty(phone)) {
            mLayoutPhone.setError("Ingresa tu número de teléfono");
            return;
        }
        // Must start with + and country code
        if (!phone.startsWith("+")) {
            mLayoutPhone.setError("Incluye el código de país, ej: +57 3001234567");
            return;
        }
        if (phone.replaceAll("[^0-9]", "").length() < 7) {
            mLayoutPhone.setError("Número demasiado corto");
            return;
        }

        mDialog.show();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        mDialog.dismiss();
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        mDialog.dismiss();
                        mLayoutPhone.setError("No se pudo enviar el código: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(String verificationId,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        mDialog.dismiss();
                        mVerificationId = verificationId;
                        mResendToken = token;
                        mLayoutCodeSection.setVisibility(View.VISIBLE);
                        mBtnSendCode.setText("REENVIAR CÓDIGO");
                        mTextInfo.setText("Código enviado a " + phone + ". Revisa tus SMS.");
                        Toast.makeText(PhoneLoginActivity.this,
                                "Código enviado ✓", Toast.LENGTH_SHORT).show();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode() {
        String code = mEditCode.getText() != null ? mEditCode.getText().toString().trim() : "";
        mLayoutCode.setError(null);

        if (TextUtils.isEmpty(code) || code.length() < 6) {
            mLayoutCode.setError("Ingresa el código de 6 dígitos");
            return;
        }
        if (mVerificationId == null) {
            Toast.makeText(this, "Primero solicita el código", Toast.LENGTH_SHORT).show();
            return;
        }

        mDialog.show();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                if (uid == null) { finish(); return; }
                mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        goHome();
                    } else {
                        User user = new User();
                        user.setId(uid);
                        user.setEmail("");
                        mUsersProvider.create(user).addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                startActivity(new Intent(this, CompleteProfileActivity.class));
                                finish();
                            }
                        });
                    }
                });
            } else {
                mLayoutCode.setError("Código incorrecto o expirado");
            }
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
