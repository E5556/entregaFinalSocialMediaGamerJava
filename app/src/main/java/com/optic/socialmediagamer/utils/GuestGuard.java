package com.optic.socialmediagamer.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.optic.socialmediagamer.activities.RegisterActivity;
import com.optic.socialmediagamer.providers.AuthProvider;

public class GuestGuard {

    /**
     * Returns true if the user is a guest AND shows the login prompt.
     * Usage:  if (GuestGuard.check(this)) return;
     */
    public static boolean check(Context context) {
        if (!new AuthProvider().isGuest()) return false;

        new AlertDialog.Builder(context)
                .setTitle("Acción no disponible")
                .setMessage("Los invitados solo pueden explorar.\n\nCrea una cuenta gratuita para comentar, publicar, unirte a clanes y mucho más.")
                .setPositiveButton("CREAR CUENTA", (d, w) ->
                        context.startActivity(new Intent(context, RegisterActivity.class)))
                .setNegativeButton("Ahora no", null)
                .show();

        return true;
    }
}
