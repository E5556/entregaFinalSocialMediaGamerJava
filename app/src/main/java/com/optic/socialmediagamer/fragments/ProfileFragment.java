package com.optic.socialmediagamer.fragments;

import android.content.Intent; // Importa la clase Intent para cambiar de actividad
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater; // Importa las clases necesarias para inflar y manejar vistas
import android.view.View; // Importa la clase View para trabajar con la interfaz de usuario
import android.view.ViewGroup;

import android.widget.ImageView; // Importa la clase ImageView para mostrar imágenes
import android.widget.LinearLayout; // Importa la clase LinearLayout para organizar vistas en una disposición lineal
import android.widget.TextView; // Importa la clase TextView para mostrar texto

import com.google.android.gms.tasks.OnSuccessListener; // Importa la interfaz OnSuccessListener para manejar eventos de éxito en tareas
import com.google.firebase.firestore.DocumentSnapshot; // Importa la clase DocumentSnapshot para representar un snapshot de un documento
import com.google.firebase.firestore.QuerySnapshot; // Importa la clase QuerySnapshot para representar un conjunto de resultados de una consulta Firestore
import com.optic.socialmediagamer.R; // Importa las constantes definidas en la clase R
import com.optic.socialmediagamer.activities.EditProfileActivity; // Importa la actividad EditProfileActivity
import com.optic.socialmediagamer.providers.AuthProvider; // Importa la clase AuthProvider para manejar la autenticación
import com.optic.socialmediagamer.providers.PostProvider; // Importa la clase PostProvider para manejar las publicaciones
import com.optic.socialmediagamer.providers.UsersProvider; // Importa la clase UsersProvider para manejar los usuarios
import com.squareup.picasso.Picasso; // Importa la clase Picasso para cargar imágenes desde URL

import de.hdodenhof.circleimageview.CircleImageView; // Importa la clase CircleImageView para mostrar una imagen de perfil circular

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    View mView; // Vista del fragmento
    LinearLayout mLinearLayoutEditProfile; // Layout para el botón de edición de perfil
    TextView mTextViewUsername; // TextView para el nombre de usuario
    TextView mTextViewPhone; // TextView para el número de teléfono
    TextView mTextViewEmail; // TextView para el correo electrónico
    TextView mTextViewPostNumber; // TextView para el número de publicaciones
    ImageView mImageViewCover; // ImageView para la imagen de portada
    CircleImageView mCircleImageProfile; // CircleImageView para la imagen de perfil

    UsersProvider mUsersProvider; // Proveedor de usuarios
    AuthProvider mAuthProvider; // Proveedor de autenticación
    PostProvider mPostProvider; // Proveedor de publicaciones

    public ProfileFragment() {
        // Constructor por defecto
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el diseño del fragmento
        mView = inflater.inflate(R.layout.fragment_profile, container, false);
        // Inicializa las vistas
        mLinearLayoutEditProfile = mView.findViewById(R.id.linearLayoutEditProfile);
        mTextViewEmail = mView.findViewById(R.id.textViewEmail);
        mTextViewUsername = mView.findViewById(R.id.textViewUsername);
        mTextViewPhone = mView.findViewById(R.id.textViewphone);
        mTextViewPostNumber = mView.findViewById(R.id.textViewPostNumber);
        mCircleImageProfile = mView.findViewById(R.id.circleImageProfile);
        mImageViewCover = mView.findViewById(R.id.imageViewCover);

        // Configura un listener para el botón de edición de perfil
        mLinearLayoutEditProfile.setOnClickListener(view -> goToEditProfile());

        // Inicializa los proveedores
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();
        mPostProvider = new PostProvider();

        // Obtiene la información del usuario y el número de publicaciones
        getUser();
        getPostNumber();
        return mView;
    }

    // Método para abrir la actividad de edición de perfil
    private void goToEditProfile() {
        Intent intent = new Intent(getContext(), EditProfileActivity.class);
        startActivity(intent);
    }

    // Método para obtener el número de publicaciones del usuario actual
    private void getPostNumber() {
        // Consulta las publicaciones del usuario actual y cuenta el número de documentos
        mPostProvider.getPostByUser(mAuthProvider.getUid()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                int numberPost = queryDocumentSnapshots.size(); // Obtiene el número de publicaciones
                mTextViewPostNumber.setText(String.valueOf(numberPost)); // Muestra el número de publicaciones en el TextView correspondiente
            }
        });
    }

    // Método para obtener la información del usuario actual
    private void getUser() {
        // Obtiene el documento del usuario actual desde Firestore
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) { // Si el documento existe
                // Obtiene y muestra el correo electrónico, si está disponible
                if (documentSnapshot.contains("email")) {
                    String email = documentSnapshot.getString("email");
                    mTextViewEmail.setText(email);
                }
                // Obtiene y muestra el número de teléfono, si está disponible
                if (documentSnapshot.contains("phone")) {
                    String phone = documentSnapshot.getString("phone");
                    mTextViewPhone.setText(phone);
                }
                // Obtiene y muestra el nombre de usuario, si está disponible
                if (documentSnapshot.contains("username")) {
                    String username = documentSnapshot.getString("username");
                    mTextViewUsername.setText(username);
                }
                // Obtiene y muestra la imagen de perfil, si está disponible
                if (documentSnapshot.contains("image_profile")) {
                    String imageProfile = documentSnapshot.getString("image_profile");
                    if (imageProfile != null && !imageProfile.isEmpty()) {
                        Picasso.get().load(imageProfile).into(mCircleImageProfile);
                    }
                }
                // Obtiene y muestra la imagen de portada, si está disponible
                if (documentSnapshot.contains("image_cover")) {
                    String imageCover = documentSnapshot.getString("image_cover");
                    if (imageCover != null && !imageCover.isEmpty()) {
                        Picasso.get().load(imageCover).into(mImageViewCover);
                    }
                }
            }
        });
    }
}
