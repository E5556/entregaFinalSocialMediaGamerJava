package com.optic.socialmediagamer.activities; // Declaración del paquete

import androidx.annotation.NonNull; // Importación de clases necesarias
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.UploadTask;
import com.optic.socialmediagamer.R; // Importación de clases personalizadas
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.models.User;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ImageProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.FileUtil;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class EditProfileActivity extends AppCompatActivity { // Declaración de la clase

    CircleImageView mCircleImageViewBack; // Declaración de variables de instancia
    CircleImageView mCircleImageViewProfile;
    ImageView mImageViewCover;
    TextInputEditText mTextInputUsername;
    TextInputEditText mTextInputPhone;
    Button mButtonEditProfile;

    AlertDialog.Builder mBuilderSelector;
    CharSequence options[];
    private final int GALLERY_REQUEST_CODE_PROFILE = 1; // Constantes para códigos de solicitud de galería y cámara
    private final int GALLERY_REQUEST_CODE_COVER = 2;
    private final int PHOTO_REQUEST_CODE_PROFILE = 3;
    private final int PHOTO_REQUEST_CODE_COVER = 4;

    // Variables para fotos
    String mAbsolutePhotoPath;
    String mPhotoPath;
    File mPhotoFile;

    String mAbsolutePhotoPath2;
    String mPhotoPath2;
    File mPhotoFile2;

    File mImageFile;
    File mImageFile2;

    String mUsername = ""; // Variables para almacenar datos del usuario
    String mPhone = "";
    String mImageProfile = "";
    String mImageCover = "";

    AlertDialog mDialog; // Diálogo de progreso

    ImageProvider mImageProvider; // Proveedor de imágenes
    UsersProvider mUsersProvider; // Proveedor de usuarios
    AuthProvider mAuthProvider; // Proveedor de autenticación

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método onCreate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile); //  se asigna el layout a la actividad de edición de perfil de usuario

        // Asignación de vistas a variables
        mCircleImageViewBack = findViewById(R.id.circleImageBack);
        mCircleImageViewProfile = findViewById(R.id.circleImageProfile);
        mImageViewCover = findViewById(R.id.imageViewCover);
        mTextInputUsername = findViewById(R.id.textInputUsername);
        mTextInputPhone = findViewById(R.id.textInputPhone);
        mButtonEditProfile = findViewById(R.id.btnEditProfile);

        // Configuración del diálogo de selección de imagen
        mBuilderSelector = new AlertDialog.Builder(this); // aviso de seleccionar imagen
        mBuilderSelector.setTitle("Selecciona una opcion");
        options = new CharSequence[] {"Imagen de galeria", "Tomar foto"};

        // Inicialización de proveedores y diálogo de progreso
        mImageProvider = new ImageProvider();
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();

        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        // Configuración del botón de edición de perfil
        mButtonEditProfile.setOnClickListener(view -> clickEditProfile());

        // Configuración del listener para seleccionar imagen de perfil al hacer clic en la imagen de perfil
        mCircleImageViewProfile.setOnClickListener(view -> selectOptionImage(1));

        // Configuración del listener para seleccionar imagen de portada al hacer clic en la imagen de portada
        mImageViewCover.setOnClickListener(view -> selectOptionImage(2));

        // Configuración del botón para volver atrás
        mCircleImageViewBack.setOnClickListener(view -> finish());

        // Obtener datos del usuario
        getUser();
    }

    private void getUser() { // Método para obtener datos del usuario desde Firebase Firestore
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) { // Si el documento existe
                // Obtener y mostrar nombre de usuario y teléfono si están disponibles
                if (documentSnapshot.contains("username")) {
                    mUsername = documentSnapshot.getString("username");
                    mTextInputUsername.setText(mUsername);
                }
                if (documentSnapshot.contains("phone")) {
                    mPhone = documentSnapshot.getString("phone");
                    mTextInputPhone.setText(mPhone);
                }
                // Cargar imagen de perfil si está disponible
                if (documentSnapshot.contains("image_profile")) {
                    mImageProfile = documentSnapshot.getString("image_profile");
                    if (mImageProfile != null && !mImageProfile.isEmpty()) {
                        Picasso.get().load(mImageProfile).into(mCircleImageViewProfile);
                    }
                }
                // Cargar imagen de portada si está disponible
                if (documentSnapshot.contains("image_cover")) {
                    mImageCover = documentSnapshot.getString("image_cover");
                    if (mImageCover != null && !mImageCover.isEmpty()) {
                        Picasso.get().load(mImageCover).into(mImageViewCover);
                    }
                }
            }
        });
    }

    private void clickEditProfile() { // Método para actualizar los datos del usuario
        mUsername = mTextInputUsername.getText().toString();
        mPhone = mTextInputPhone.getText().toString();
        if (!mUsername.isEmpty() && !mPhone.isEmpty()) {
            // Comprobar qué imágenes se han seleccionado y guardarlas
            if (mImageFile != null && mImageFile2 != null ) {
                saveImageCoverAndProfile(mImageFile, mImageFile2);
            }
            else if (mPhotoFile != null && mPhotoFile2 != null) {
                saveImageCoverAndProfile(mPhotoFile, mPhotoFile2);
            }
            else if (mImageFile != null && mPhotoFile2 != null) {
                saveImageCoverAndProfile(mImageFile, mPhotoFile2);
            }
            else if (mPhotoFile != null && mImageFile2 != null) {
                saveImageCoverAndProfile(mPhotoFile, mImageFile2);
            }
            else if (mPhotoFile != null) {
                saveImage(mPhotoFile, true);
            }
            else if (mPhotoFile2 != null) {
                saveImage(mPhotoFile2, false);
            }
            else if (mImageFile != null) {
                saveImage(mImageFile, true);
            }
            else if (mImageFile2 != null) {
                saveImage(mImageFile2, false);
            }
            else {
                // Si no hay imágenes nuevas, actualizar solo los datos
                User user = new User();
                user.setUsername(mUsername);
                user.setPhone(mPhone);
                user.setId(mAuthProvider.getUid());
                updateInfo(user);
            }
        }
        else {
            Toast.makeText(this, "Ingrese el nombre de usuario y el telefono", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageCoverAndProfile(File imageFile1, final File imageFile2) { // Método para guardar imágenes de perfil y portada, osea dos datos
        mDialog.show(); // Mostrar diálogo de progreso
        // Guardar imagen de perfil
        mImageProvider.save(EditProfileActivity.this, imageFile1).addOnCompleteListener(task -> { //
            if (task.isSuccessful()) {
                // Obtener URL de la imagen de perfil
                mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    final String urlProfile = uri.toString();
                    // Guardar imagen de portada
                    mImageProvider.save(EditProfileActivity.this, imageFile2).addOnCompleteListener(taskImage2 -> {
                        if (taskImage2.isSuccessful()) {
                            // Obtener URL de la imagen de portada
                            mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri2 -> {
                                String urlCover = uri2.toString();
                                // Actualizar datos del usuario con las URLs de las imágenes
                                User user = new User();
                                user.setUsername(mUsername);
                                user.setPhone(mPhone);
                                user.setImageProfile(urlProfile);
                                user.setImageCover(urlCover);
                                user.setId(mAuthProvider.getUid());
                                updateInfo(user);
                            });
                        }
                        else {
                            mDialog.dismiss();
                            Toast.makeText(EditProfileActivity.this, "La imagen numero 2 no se pudo guardar", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
            else {
                mDialog.dismiss();
                Toast.makeText(EditProfileActivity.this, "Hubo error al almacenar la imagen", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveImage(File image, final boolean isProfileImage) { // Método para guardar una sola imagen
        mDialog.show(); // Mostrar diálogo de progreso
        mImageProvider.save(EditProfileActivity.this, image).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Obtener URL de la imagen
                mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    final String url = uri.toString();
                    // Crear objeto de usuario y actualizar datos
                    User user = new User();
                    user.setUsername(mUsername);
                    user.setPhone(mPhone);
                    if (isProfileImage) {
                        user.setImageProfile(url);
                        user.setImageCover(mImageCover);
                    }
                    else {
                        user.setImageCover(url);
                        user.setImageProfile(mImageProfile);
                    }
                    user.setId(mAuthProvider.getUid());
                    updateInfo(user);
                });
            }
            else {
                mDialog.dismiss();
                Toast.makeText(EditProfileActivity.this, "Hubo error al almacenar la imagen", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateInfo(User user) { // Método para actualizar datos del usuario
        if (mDialog.isShowing()) {
            mDialog.show();
        }
        // Actualizar datos en Firestore
        mUsersProvider.update(user).addOnCompleteListener(task -> {
            mDialog.dismiss(); // Ocultar diálogo de progreso
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "La informacion se actualizo correctamente", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(EditProfileActivity.this, "La informacion no se pudo actualizar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectOptionImage(final int numberImage) { // GALERIA  Método para seleccionar una opción de imagen (galería o cámara) GALERIA
        mBuilderSelector.setItems(options, (dialogInterface, i) -> {
            if (i == 0) { // Si se elige la opción de galería
                if (numberImage == 1) {
                    openGallery(GALLERY_REQUEST_CODE_PROFILE); // Abrir galería para imagen de perfil
                }
                else if (numberImage == 2) {
                    openGallery(GALLERY_REQUEST_CODE_COVER); // Abrir galería para imagen de portada
                }
            }
            else if (i == 1){ // Si se elige la opción de tomar foto
                if (numberImage == 1) {
                    takePhoto(PHOTO_REQUEST_CODE_PROFILE); // Tomar foto para imagen de perfil
                }
                else if (numberImage == 2) {
                    takePhoto(PHOTO_REQUEST_CODE_COVER); // Tomar foto para imagen de portada
                }
            }
        });

        mBuilderSelector.show(); // Mostrar diálogo de selección
    }

    private void takePhoto(int requestCode) { // Método para tomar una foto DESDA LA CAMARA
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createPhotoFile(requestCode); // Crear archivo para la foto
            } catch(Exception e) {
                Toast.makeText(this, "Hubo un error con el archivo " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(EditProfileActivity.this, "com.optic.socialmediagamer", photoFile); // Obtener URI del archivo
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, requestCode); // Iniciar la actividad de la cámara
            }
        }
    }

    private File createPhotoFile(int requestCode) throws IOException { // Método para crear un archivo para la foto
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // Directorio de almacenamiento externo
        File photoFile = File.createTempFile(
                new Date() + "_photo",
                ".jpg",
                storageDir
        );
        if (requestCode == PHOTO_REQUEST_CODE_PROFILE) {
            mPhotoPath = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath = photoFile.getAbsolutePath();
        }
        else if (requestCode == PHOTO_REQUEST_CODE_COVER) {
            mPhotoPath2 = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath2 = photoFile.getAbsolutePath();
        }
        return photoFile;
    }

    private void openGallery(int requestCode) { // Método para abrir la galería
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, requestCode); // Iniciar la actividad de la galería
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // Método para manejar el resultado de la actividad (tomar foto o seleccionar imagen de galería)
        super.onActivityResult(requestCode, resultCode, data);
        // Seleccionar imagen desde la galería
        if (requestCode == GALLERY_REQUEST_CODE_PROFILE && resultCode == RESULT_OK) {
            try {
                mPhotoFile = null;
                mImageFile = FileUtil.from(this, data.getData()); // Convertir URI de la imagen en archivo
                mCircleImageViewProfile.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath())); // Mostrar imagen en ImageView
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE_COVER && resultCode == RESULT_OK) {
            try {
                mPhotoFile2 = null;
                mImageFile2 = FileUtil.from(this, data.getData()); // Convertir URI de la imagen en archivo
                mImageViewCover.setImageBitmap(BitmapFactory.decodeFile(mImageFile2.getAbsolutePath())); // Mostrar imagen en ImageView
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        // Tomar foto con la cámara
        if (requestCode == PHOTO_REQUEST_CODE_PROFILE && resultCode == RESULT_OK) {
            mImageFile = null;
            mPhotoFile = new File(mAbsolutePhotoPath);
            Picasso.get().load(mPhotoPath).into(mCircleImageViewProfile); // Mostrar imagen en ImageView
        }

        if (requestCode == PHOTO_REQUEST_CODE_COVER && resultCode == RESULT_OK) {
            mImageFile2 = null;
            mPhotoFile2 = new File(mAbsolutePhotoPath2);
            Picasso.get().load(mPhotoPath2).into(mImageViewCover); // Mostrar imagen en ImageView
        }
    }
}
