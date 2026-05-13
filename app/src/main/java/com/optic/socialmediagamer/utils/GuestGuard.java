package com.optic.socialmediagamer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.activities.MainActivity;
import com.optic.socialmediagamer.activities.RegisterActivity;
import com.optic.socialmediagamer.providers.AuthProvider;

public class GuestGuard {

    public static final int RC_GOOGLE_FROM_GUARD = 99;

    /**
     * Returns true if the user is a guest AND shows the login prompt.
     * Usage:  if (GuestGuard.check(this)) return;
     */
    public static boolean check(Context context) {
        if (!new AuthProvider().isGuest()) return false;

        String[] options = {"Crear cuenta", "Iniciar con Google", "Ahora no"};

        new AlertDialog.Builder(context)
                .setTitle("Acción no disponible")
                .setMessage("Los invitados solo pueden explorar.\n\nInicia sesión para comentar, publicar, unirte a clanes y mucho más.")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        context.startActivity(new Intent(context, RegisterActivity.class));
                    } else if (which == 1) {
                        // Sign out anonymous session and go to MainActivity for Google login
                        new AuthProvider().logout();
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("autoGoogle", true);
                        context.startActivity(intent);
                    }
                })
                .show();

        return true;
    }
}
