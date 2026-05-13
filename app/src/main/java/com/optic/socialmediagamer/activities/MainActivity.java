package com.optic.socialmediagamer.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_GOOGLE = 1;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 30_000;

    private TextInputLayout  mLayoutEmail, mLayoutPassword;
    private TextInputEditText mTextInputEmail, mTextInputPassword;
    private Button mButtonLogin;
    private ImageButton mButtonGoogle, mButtonPhone, mButtonGuest;
    private TextView mTextViewRegister, mTextViewForgotPassword, mTextViewRateLimit;

    private AuthProvider mAuthProvider;
    private UsersProvider mUsersProvider;
    private GoogleSignInClient mGoogleSignInClient;
    private AlertDialog mDialog;

    private int mFailedAttempts = 0;
    private boolean mLockedOut = false;
    private CountDownTimer mLockoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayoutEmail    = findViewById(R.id.layoutEmail);
        mLayoutPassword = findViewById(R.id.layoutPassword);
        mTextInputEmail    = findViewById(R.id.textInputEmail);
        mTextInputPassword = findViewById(R.id.textInputPassword);
        mButtonLogin  = findViewById(R.id.btnLogin);
        mButtonGoogle = findViewById(R.id.btnLoginGoogle);
        mButtonPhone  = findViewById(R.id.btnLoginPhone);
        mButtonGuest  = findViewById(R.id.btnLoginGuest);
        mTextViewRegister      = findViewById(R.id.textViewRegister);
        mTextViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        mTextViewRateLimit     = findViewById(R.id.textViewRateLimit);

        mAuthProvider  = new AuthProvider();
        mUsersProvider = new UsersProvider();

        mDialog = new SpotsDialog.Builder()
                .setContext(this).setMessage("ESPERE UN MOMENTO").setCancelable(false).build();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mButtonLogin.setOnClickListener(v -> login());
        mButtonGoogle.setOnClickListener(v -> signInGoogle());
        mButtonPhone.setOnClickListener(v -> signInWithPhone());
        mButtonGuest.setOnClickListener(v -> signInAsGuest());

        if (getIntent().getBooleanExtra("autoGoogle", false)) {
            signInGoogle();
        }
        mTextViewRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        mTextViewForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuthProvider.getUserSession() != null) {
            goHome();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        mLayoutEmail.setError(null);
        mLayoutPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            mLayoutEmail.setError("El correo es obligatorio");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mLayoutEmail.setError("Formato de correo inválido");
            valid = false;
        } else if (email.length() > 100) {
            mLayoutEmail.setError("Correo demasiado largo");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            mLayoutPassword.setError("La contraseña es obligatoria");
            valid = false;
        } else if (password.length() < 6) {
            mLayoutPassword.setError("Mínimo 6 caracteres");
            valid = false;
        }

        return valid;
    }

    // ── Brute-force protection ────────────────────────────────────────────────

    private void recordFailedAttempt() {
        mFailedAttempts++;
        if (mFailedAttempts >= MAX_ATTEMPTS) {
            startLockout();
        }
    }

    private void startLockout() {
        mLockedOut = true;
        mButtonLogin.setEnabled(false);
        mTextViewRateLimit.setVisibility(View.VISIBLE);

        if (mLockoutTimer != null) mLockoutTimer.cancel();
        mLockoutTimer = new CountDownTimer(LOCKOUT_MS, 1000) {
            @Override public void onTick(long ms) {
                mTextViewRateLimit.setText("Demasiados intentos. Espera " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                mLockedOut = false;
                mFailedAttempts = 0;
                mButtonLogin.setEnabled(true);
                mTextViewRateLimit.setVisibility(View.GONE);
            }
        }.start();
    }

    // ── Email / Password login ────────────────────────────────────────────────

    private void login() {
        if (mLockedOut) return;

        String email    = mTextInputEmail.getText() != null
                ? mTextInputEmail.getText().toString().trim() : "";
        String password = mTextInputPassword.getText() != null
                ? mTextInputPassword.getText().toString() : "";

        if (!validateInputs(email, password)) return;

        mDialog.show();
        mAuthProvider.login(email, password).addOnCompleteListener(task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                mFailedAttempts = 0;
                saveFcmToken();
                goHome();
            } else {
                recordFailedAttempt();
                // Generic message — prevents email enumeration
                mLayoutPassword.setError("Correo o contraseña incorrectos");
            }
        });
    }

    // ── Google login ──────────────────────────────────────────────────────────

    private void signInGoogle() {
        // Sign out from Google client first to force account picker every time
        mGoogleSignInClient.signOut().addOnCompleteListener(this, t ->
                startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_GOOGLE));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                mDialog.show();
                mAuthProvider.googleLogin(account).addOnCompleteListener(this, t -> {
                    mDialog.dismiss();
                    if (t.isSuccessful()) {
                        checkUserExist(mAuthProvider.getUid());
                    } else {
                        Toast.makeText(this, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (ApiException e) {
                Log.w("LOGIN", "Google sign in failed", e);
            }
        }
    }

    // ── Guest / Anonymous login ───────────────────────────────────────────────

    private void signInAsGuest() {
        new AlertDialog.Builder(this)
                .setTitle("Entrar como invitado")
                .setMessage("Podrás explorar la app pero no podrás publicar ni interactuar.\n\nPuedes crear una cuenta en cualquier momento.")
                .setPositiveButton("ENTRAR", (d, w) -> {
                    mDialog.show();
                    FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
                        mDialog.dismiss();
                        if (task.isSuccessful()) {
                            goHome();
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "desconocido";
                            Log.e("GUEST_LOGIN", "Error: " + err);
                            Toast.makeText(this, "Error: " + err, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ── Phone login ───────────────────────────────────────────────────────────

    private void signInWithPhone() {
        startActivity(new Intent(this, PhoneLoginActivity.class));
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    private void showForgotPasswordDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("tu@correo.com");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Recibirás un enlace en tu correo para restablecer tu contraseña.")
                .setView(input)
                .setPositiveButton("ENVIAR", (d, w) -> {
                    String email = input.getText().toString().trim();
                    if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener(task ->
                                    // Same message whether email exists or not (prevent enumeration)
                                    Toast.makeText(this,
                                            "Si el correo existe, recibirás un enlace de recuperación.",
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkUserExist(String uid) {
        mUsersProvider.getUser(uid).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                saveFcmToken();
                goHome();
            } else {
                String email = mAuthProvider.getEmail();
                User user = new User();
                user.setEmail(email);
                user.setId(uid);
                mUsersProvider.create(user).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, CompleteProfileActivity.class));
                    } else {
                        Toast.makeText(this, "No se pudo guardar el usuario", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveFcmToken() {
        String uid = mAuthProvider.getUid();
        if (uid == null) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> mUsersProvider.saveToken(uid, token));
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
