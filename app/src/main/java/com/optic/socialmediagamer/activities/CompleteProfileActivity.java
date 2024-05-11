package com.optic.socialmediagamer.activities; // Declaración del paquete

import androidx.annotation.NonNull; // Importación de clases necesarias
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.optic.socialmediagamer.R; // Importación de clases personalizadas
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import java.util.Date; // Importación de clases estándar de Java
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;

public class CompleteProfileActivity extends AppCompatActivity {

    TextInputEditText mTextInputUsername; // Declaración de variables de clase, se instancian variables de vistas
    TextInputEditText mTextInputPhone;
    Button mButtonRegister;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método donde se inicializan las vistas y proveedores
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile); // Asignación del layout

        mTextInputUsername = findViewById(R.id.textInputUsername); // Asignación de vistas a variables
        mTextInputPhone = findViewById(R.id.textInputPhone);
        mButtonRegister = findViewById(R.id.btnRegister);

        mAuthProvider = new AuthProvider(); // Inicialización de proveedores
        mUsersProvider = new UsersProvider();

        mDialog = new SpotsDialog.Builder() // Creación del diálogo de carga
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        mButtonRegister.setOnClickListener(view -> register()); // objeto de escucha para el botón de registro
    }

    private void register() { // Método para registrar usuario
        String username = mTextInputUsername.getText().toString(); // Obtener valores de los campos
        String phone = mTextInputPhone.getText().toString();
        if (!username.isEmpty()) { // Verificar que el campo de nombre de usuario no esté vacío
            updateUser(username, phone); // Llamar al método para actualizar el usuario
        }
        else {
            Toast.makeText(this, "Para continuar inserta todos los campos", Toast.LENGTH_SHORT).show(); // Mostrar mensaje de error si falta información
        }
    }

    private void updateUser(final String username, final String phone) { // Método para actualizar usuario
        String id = mAuthProvider.getUid(); // Obtener el ID del usuario autenticado
        User user = new User(); // Crear un objeto de usuario
        user.setUsername(username); // Establecer valores en el objeto de usuario
        user.setId(id);
        user.setPhone(phone);
        user.setTimestamp(new Date().getTime());

        mDialog.show(); // Mostrar diálogo de carga
        // Actualizar usuario en la base de datos
        mUsersProvider.update(user).addOnCompleteListener(task -> { // Escuchar el resultado de la actualización
            mDialog.dismiss(); // Ocultar diálogo de carga
            if (task.isSuccessful()) { // Verificar si la actualización fue exitosa
                Intent intent = new Intent(CompleteProfileActivity.this, HomeActivity.class); // Crear un intent para ir a la actividad principal
                startActivity(intent); // Iniciar la actividad principal
            }
            else {
                Toast.makeText(CompleteProfileActivity.this, "No se pudo almacenar el usuario en la base de datos", Toast.LENGTH_SHORT).show(); // Mostrar mensaje de error si la actualización falla
            }
        });
    }

}
