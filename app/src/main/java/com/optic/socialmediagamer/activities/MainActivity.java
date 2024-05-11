package com.optic.socialmediagamer.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    TextView mTextViewRegister;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputPassword;
    Button mButtonLogin;
    AuthProvider mAuthProvider;
    SignInButton mButtonGoogle;
    public GoogleSignInClient mGoogleSignInClient;  // *********Cambiar a Private****Cliente de inicio de sesión con Google
    private final int REQUEST_CODE_GOOGLE = 1;
    AlertDialog mDialog;
    UsersProvider mUsersProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewRegister = findViewById(R.id.textViewRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputPassword = findViewById(R.id.textInputPassword);
        mButtonLogin = findViewById(R.id.btnLogin);
        mButtonGoogle = findViewById(R.id.btnLoginGoogle);

        mAuthProvider = new AuthProvider();
        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("ESPERE UN MOMENTO")
                .setCancelable(false).build();

        // Configuración de inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso); // Inicialización del cliente de inicio de sesión con Google
        mUsersProvider = new UsersProvider(); // Inicialización del proveedor de usuarios

        // Listener para el botón de inicio de sesión con Google
        mButtonGoogle.setOnClickListener(view -> signInGoogle());   // Iniciar sesión con Google

        // Listener para el botón de inicio de sesión
        mButtonLogin.setOnClickListener(view -> login());

        // Listener para el texto de registro
        mTextViewRegister.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() { // Método para verificar si hay una sesión iniciada
        super.onStart();
        // Verificar si hay una sesión iniciada
        if (mAuthProvider.getUserSession() != null) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Limpiar la pila de actividades
            startActivity(intent); // Iniciar la actividad principal
        }
    }

    // Método para iniciar sesión con Google
    private void signInGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent(); // Obtener el intent de inicio de sesión con Google
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE); // Iniciar la actividad de inicio de sesión con Google
    }

    // Método para manejar el resultado de la actividad de inicio de sesión con Google
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GOOGLE) { // Verificar si el código de solicitud es el mismo
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("ERROR", "Google sign in failed", e);
            }
        }
    }

    // Método para autenticar con Firebase usando Google
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        mDialog.show();
        mAuthProvider.googleLogin(acct).addOnCompleteListener(this, task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                String id = mAuthProvider.getUid();
                checkUserExist(id);
            }
            else {
                Log.w("ERROR", "signInWithCredential:failure", task.getException());
                Toast.makeText(MainActivity.this, "No se pudo iniciar sesion con google", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void checkUserExist(final String id) {  // Método para verificar si el usuario ya existe en la base de datos
        mUsersProvider.getUser(id).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                mDialog.dismiss();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
            else {
                String email = mAuthProvider.getEmail();
                User user = new User();
                user.setEmail(email);
                user.setId(id);
                mUsersProvider.create(user).addOnCompleteListener(task -> {
                    mDialog.dismiss();
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(MainActivity.this, CompleteProfileActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "No se pudo almacenar la informacion del usuario", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Método para iniciar sesión con email y contraseña
    public void login() { // Método para iniciar sesión   // *********Cambiar a Private****
        String email = mTextInputEmail.getText().toString();
        String password = mTextInputPassword.getText().toString();

        mDialog.show();

        mAuthProvider.login(email, password).addOnCompleteListener(task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            else {
                Toast.makeText(MainActivity.this, "El email o la contraseña que ingresaste no son correctas", Toast.LENGTH_LONG).show();
            }
        });
        Log.d("CAMPO1", "email: " + email); // Mostrar email y contraseña en el log
        Log.d("CAMPO2", "password: " + password);// Mostrar email y contraseña en el log
    }
}
