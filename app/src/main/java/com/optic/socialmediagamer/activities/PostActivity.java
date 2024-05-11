package com.optic.socialmediagamer.activities;

import androidx.annotation.NonNull;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.UploadTask;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.ImageProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.utils.FileUtil;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class PostActivity extends AppCompatActivity {

    // Declaración de variables y componentes de la interfaz
    ImageView mImageViewPost1; // ImageView para la primera imagen
    ImageView mImageViewPost2; // ImageView para la segunda imagen
    File mImageFile; // Archivo de imagen para la primera imagen
    File mImageFile2; // Archivo de imagen para la segunda imagen
    Button mButtonPost; // Botón para publicar el post
    ImageProvider mImageProvider; // Proveedor de imágenes
    PostProvider mPostProvider; // Proveedor de posts
    AuthProvider mAuthProvider; // Proveedor de autenticación
    TextInputEditText mTextInputTitle; // Campo de entrada para el título del post
    TextInputEditText mTextInputDescription; // Campo de entrada para la descripción del post
    ImageView mImageViewPC; // ImageView para la categoría PC
    ImageView mImageViewPS4; // ImageView para la categoría PS4
    ImageView mImageViewXBOX; // ImageView para la categoría XBOX
    ImageView mImageViewNitendo; // ImageView para la categoría Nintendo
    CircleImageView mCircleImageBack; // ImageView para retroceder
    TextView mTextViewCategory; // TextView para mostrar la categoría seleccionada
    String mCategory = ""; // Variable para almacenar la categoría seleccionada
    String mTitle = ""; // Variable para almacenar el título del post
    String mDescription = ""; // Variable para almacenar la descripción del post
    AlertDialog mDialog; // Diálogo para mostrar mensajes de carga
    AlertDialog.Builder mBuilderSelector; // Constructor de diálogo para seleccionar opción de imagen
    CharSequence options[]; // Opciones para la selección de imagen desde la galería o la cámara
    private final int GALLERY_REQUEST_CODE = 1; // Código de solicitud para abrir la galería (primera imagen)
    private final int GALLERY_REQUEST_CODE_2 = 2; // Código de solicitud para abrir la galería (segunda imagen)
    private final int PHOTO_REQUEST_CODE = 3; // Código de solicitud para tomar una foto (primera imagen)
    private final int PHOTO_REQUEST_CODE_2 = 4; // Código de solicitud para tomar una foto (segunda imagen)

    // Ruta absoluta y relativa de la primera imagen tomada con la cámara
    String mAbsolutePhotoPath;
    String mPhotoPath;
    File mPhotoFile;

    // Ruta absoluta y relativa de la segunda imagen tomada con la cámara
    String mAbsolutePhotoPath2;
    String mPhotoPath2;
    File mPhotoFile2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Inicialización de proveedores y variables
        mImageProvider = new ImageProvider();
        mPostProvider = new PostProvider();
        mAuthProvider = new AuthProvider();

        // Configuración del diálogo de carga
        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        mBuilderSelector = new AlertDialog.Builder(this);
        mBuilderSelector.setTitle("Selecciona una opcion");
        options = new CharSequence[] {"Imagen de galeria", "Tomar foto"};

        // Referencias a los elementos de la interfaz de vista
        mImageViewPost1 = findViewById(R.id.imageViewPost1);
        mImageViewPost2 = findViewById(R.id.imageViewPost2);
        mButtonPost = findViewById(R.id.btnPost);
        mTextInputTitle = findViewById(R.id.textInputVideoGame);
        mTextInputDescription = findViewById(R.id.textInputDescription);
        mImageViewPC = findViewById(R.id.imageViewPc);
        mImageViewPS4 = findViewById(R.id.imageViewPS4);
        mImageViewXBOX = findViewById(R.id.imageViewXbox);
        mImageViewNitendo = findViewById(R.id.imageViewNintendo);
        mTextViewCategory = findViewById(R.id.textViewCategory);
        mCircleImageBack = findViewById(R.id.circleImageBack);

        // Listener para el botón de retroceso
        mCircleImageBack.setOnClickListener(view -> finish());

        // Listener para el botón de publicación
        mButtonPost.setOnClickListener(view -> clickPost());

        // Listener para la imagen 1
        mImageViewPost1.setOnClickListener(view -> selectOptionImage(1));

        // Listener para la imagen 2
        mImageViewPost2.setOnClickListener(view -> selectOptionImage(2));

        // Listener para la categoría PC
        mImageViewPC.setOnClickListener(view -> {
            mCategory = "PC";
            mTextViewCategory.setText(mCategory);
        });

        // Listener para la categoría PS4
        mImageViewPS4.setOnClickListener(view -> {
            mCategory = "PS4";
            mTextViewCategory.setText(mCategory);
        });

        // Listener para la categoría XBOX
        mImageViewXBOX.setOnClickListener(view -> {
            mCategory = "XBOX";
            mTextViewCategory.setText(mCategory);
        });

        // Listener para la categoría Nintendo
        mImageViewNitendo.setOnClickListener(view -> {
            mCategory = "NINTENDO";
            mTextViewCategory.setText(mCategory);
        });
    }

    // Método para mostrar el diálogo de selección de opción de imagen
    private void selectOptionImage(final int numberImage) {
        mBuilderSelector.setItems(options, (dialogInterface, i) -> {
            if (i == 0) {
                if (numberImage == 1) {
                    openGallery(GALLERY_REQUEST_CODE);
                }
                else if (numberImage == 2) {
                    openGallery(GALLERY_REQUEST_CODE_2);
                }
            }
            else if (i == 1){
                if (numberImage == 1) {
                    takePhoto(PHOTO_REQUEST_CODE);
                }
                else if (numberImage == 2) {
                    takePhoto(PHOTO_REQUEST_CODE_2);
                }
            }
        });

        mBuilderSelector.show();
    }

    // Método para tomar una foto
    private void takePhoto(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createPhotoFile(requestCode);
            } catch(Exception e) {
                Toast.makeText(this, "Hubo un error con el archivo " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(PostActivity.this, "com.optic.socialmediagamer", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, requestCode);
            }
        }
    }

    // Método para crear un archivo de foto
    private File createPhotoFile(int requestCode) throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile = File.createTempFile(
                new Date() + "_photo",
                ".jpg",
                storageDir
        );
        if (requestCode == PHOTO_REQUEST_CODE) {
            mPhotoPath = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath = photoFile.getAbsolutePath();
        }
        else if (requestCode == PHOTO_REQUEST_CODE_2) {
            mPhotoPath2 = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath2 = photoFile.getAbsolutePath();
        }
        return photoFile;
    }

    // Método para realizar la publicación del post
    private void clickPost() {
        mTitle = mTextInputTitle.getText().toString();
        mDescription = mTextInputDescription.getText().toString();
        if (!mTitle.isEmpty() && !mDescription.isEmpty() && !mCategory.isEmpty()) {
            if (mImageFile != null && mImageFile2 != null ) {
                saveImage(mImageFile, mImageFile2);
            }
            else if (mPhotoFile != null && mPhotoFile2 != null) {
                saveImage(mPhotoFile, mPhotoFile2);
            }
            else if (mImageFile != null && mPhotoFile2 != null) {
                saveImage(mImageFile, mPhotoFile2);
            }
            else if (mPhotoFile != null && mImageFile2 != null) {
                saveImage(mPhotoFile, mImageFile2);
            }
            else {
                Toast.makeText(this, "Debes seleccionar una imagen", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Completa los campos para publicar", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para guardar las imágenes en el almacenamiento
    private void saveImage(File imageFile1, final File imageFile2) {
        mDialog.show();
        mImageProvider.save(PostActivity.this, imageFile1).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            final String url = uri.toString();

                            mImageProvider.save(PostActivity.this, imageFile2).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> taskImage2) {
                                    if (taskImage2.isSuccessful()) {
                                        mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri2) {
                                                String url2 = uri2.toString();
                                                Post post = new Post();
                                                post.setImage1(url);
                                                post.setImage2(url2);
                                                post.setTitle(mTitle);
                                                post.setDescription(mDescription);
                                                post.setCategory(mCategory);
                                                post.setIdUser(mAuthProvider.getUid());
                                                post.setTimestamp(new Date().getTime());
                                                mPostProvider.save(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> taskSave) {
                                                        mDialog.dismiss();
                                                        if (taskSave.isSuccessful()) {
                                                            clearForm();
                                                            Toast.makeText(PostActivity.this, "La informacion se almaceno correctamente", Toast.LENGTH_SHORT).show();
                                                        }
                                                        else {
                                                            Toast.makeText(PostActivity.this, "No se pudo almacenar la informacion", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                    else {
                                        mDialog.dismiss();
                                        Toast.makeText(PostActivity.this, "La imagen numero 2 no se pudo guardar", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
                else {
                    mDialog.dismiss();
                    Toast.makeText(PostActivity.this, "Hubo error al almacenar la imagen", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Método para limpiar el formulario después de la publicación
    private void clearForm() {
        mTextInputTitle.setText("");
        mTextInputDescription.setText("");
        mTextViewCategory.setText("CATEGORIAS");
        mImageViewPost1.setImageResource(R.drawable.upload_image);
        mImageViewPost2.setImageResource(R.drawable.upload_image);
        mTitle = "";
        mDescription = "";
        mCategory = "";
        mImageFile = null;
        mImageFile2 = null;
    }

    // Método para abrir la galería y seleccionar una imagen
    private void openGallery(int requestCode) {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, requestCode);
    }

    // Método que se ejecuta después de seleccionar una imagen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                mPhotoFile = null;
                mImageFile = FileUtil.from(this, data.getData());
                mImageViewPost1.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE_2 && resultCode == RESULT_OK) {
            try {
                mPhotoFile2 = null;
                mImageFile2 = FileUtil.from(this, data.getData());
                mImageViewPost2.setImageBitmap(BitmapFactory.decodeFile(mImageFile2.getAbsolutePath()));
            } catch(Exception e) {
                Log.d("ERROR", "Se produjo un error " + e.getMessage());
                Toast.makeText(this, "Se produjo un error " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            mImageFile = null;
            mPhotoFile = new File(mAbsolutePhotoPath);
            Picasso.get().load(mPhotoPath).into(mImageViewPost1);
        }

        if (requestCode == PHOTO_REQUEST_CODE_2 && resultCode == RESULT_OK) {
            mImageFile2 = null;
            mPhotoFile2 = new File(mAbsolutePhotoPath2);
            Picasso.get().load(mPhotoPath2).into(mImageViewPost2);
        }
    }
}
