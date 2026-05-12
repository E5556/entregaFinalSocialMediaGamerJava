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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.UploadTask;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.models.Notification;
import com.optic.socialmediagamer.models.Post;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.FollowProvider;
import com.optic.socialmediagamer.providers.ImageProvider;
import com.optic.socialmediagamer.models.Poll;
import com.optic.socialmediagamer.providers.NotificationsProvider;
import com.optic.socialmediagamer.providers.PollProvider;
import com.optic.socialmediagamer.providers.PostProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.providers.MissionsProvider;
import com.optic.socialmediagamer.providers.XPProvider;
import com.optic.socialmediagamer.utils.FCMSender;
import com.optic.socialmediagamer.utils.RankHelper;
import com.optic.socialmediagamer.utils.FileUtil;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class PostActivity extends AppCompatActivity {

    ImageView mImageViewPost1;
    ImageView mImageViewPost2;
    File mImageFile;
    File mImageFile2;
    Button mButtonPost;
    ImageProvider mImageProvider;
    PostProvider mPostProvider;
    XPProvider mXPProvider;
    AuthProvider mAuthProvider;
    FollowProvider mFollowProvider;
    NotificationsProvider mNotificationsProvider;
    UsersProvider mUsersProvider;
    TextInputEditText mTextInputTitle;
    TextInputEditText mTextInputDescription;
    ImageView mImageViewPC;
    ImageView mImageViewPS4;
    ImageView mImageViewXBOX;
    ImageView mImageViewNitendo;
    CircleImageView mCircleImageBack;
    TextView mTextViewCategory;
    String mCategory = "";
    String mTitle = "";
    String mDescription = "";
    AlertDialog mDialog;
    AlertDialog.Builder mBuilderSelector;
    CharSequence options[];
    private final int GALLERY_REQUEST_CODE = 1;
    private final int GALLERY_REQUEST_CODE_2 = 2;
    private final int PHOTO_REQUEST_CODE = 3;
    private final int PHOTO_REQUEST_CODE_2 = 4;

    String mAbsolutePhotoPath;
    String mPhotoPath;
    File mPhotoFile;
    String mAbsolutePhotoPath2;
    String mPhotoPath2;
    File mPhotoFile2;

    SwitchCompat mSwitchPoll;
    LinearLayout mLayoutPollInputs;
    TextInputEditText mTextInputPollOptionA;
    TextInputEditText mTextInputPollOptionB;
    PollProvider mPollProvider;

    // Modo edición
    boolean mIsEdit = false;
    String mPostId;
    String mExistingImage1 = "";
    String mExistingImage2 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        mImageProvider         = new ImageProvider();
        mPostProvider          = new PostProvider();
        mXPProvider            = new XPProvider();
        mAuthProvider          = new AuthProvider();
        mFollowProvider        = new FollowProvider();
        mNotificationsProvider = new NotificationsProvider();
        mUsersProvider         = new UsersProvider();
        mPollProvider          = new PollProvider();

        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        mBuilderSelector = new AlertDialog.Builder(this);
        mBuilderSelector.setTitle("Selecciona una opcion");
        options = new CharSequence[] {"Imagen de galeria", "Tomar foto"};

        mImageViewPost1      = findViewById(R.id.imageViewPost1);
        mImageViewPost2      = findViewById(R.id.imageViewPost2);
        mButtonPost          = findViewById(R.id.btnPost);
        mTextInputTitle      = findViewById(R.id.textInputVideoGame);
        mTextInputDescription = findViewById(R.id.textInputDescription);
        mImageViewPC         = findViewById(R.id.imageViewPc);
        mImageViewPS4        = findViewById(R.id.imageViewPS4);
        mImageViewXBOX       = findViewById(R.id.imageViewXbox);
        mImageViewNitendo    = findViewById(R.id.imageViewNintendo);
        mTextViewCategory    = findViewById(R.id.textViewCategory);
        mCircleImageBack     = findViewById(R.id.circleImageBack);
        mSwitchPoll          = findViewById(R.id.switchPoll);
        mLayoutPollInputs    = findViewById(R.id.layoutPollInputs);
        mTextInputPollOptionA = findViewById(R.id.textInputPollOptionA);
        mTextInputPollOptionB = findViewById(R.id.textInputPollOptionB);

        mSwitchPoll.setOnCheckedChangeListener((btn, checked) ->
            mLayoutPollInputs.setVisibility(checked ? android.view.View.VISIBLE : android.view.View.GONE));

        // Verificar si es modo edición
        mIsEdit = getIntent().getBooleanExtra("isEdit", false);
        if (mIsEdit) {
            mPostId         = getIntent().getStringExtra("postId");
            mExistingImage1 = getIntent().getStringExtra("image1");
            mExistingImage2 = getIntent().getStringExtra("image2");
            String title    = getIntent().getStringExtra("title");
            String desc     = getIntent().getStringExtra("description");
            mCategory       = getIntent().getStringExtra("category");

            mTextInputTitle.setText(title);
            mTextInputDescription.setText(desc);
            mTextViewCategory.setText(mCategory);
            mButtonPost.setText("ACTUALIZAR");

            if (mExistingImage1 != null && !mExistingImage1.isEmpty()) {
                Picasso.get().load(mExistingImage1).into(mImageViewPost1);
            }
            if (mExistingImage2 != null && !mExistingImage2.isEmpty()) {
                Picasso.get().load(mExistingImage2).into(mImageViewPost2);
            }
        }

        mCircleImageBack.setOnClickListener(view -> finish());
        mButtonPost.setOnClickListener(view -> clickPost());
        mImageViewPost1.setOnClickListener(view -> selectOptionImage(1));
        mImageViewPost2.setOnClickListener(view -> selectOptionImage(2));

        mImageViewPC.setOnClickListener(view -> { mCategory = "PC"; mTextViewCategory.setText(mCategory); });
        mImageViewPS4.setOnClickListener(view -> { mCategory = "PS4"; mTextViewCategory.setText(mCategory); });
        mImageViewXBOX.setOnClickListener(view -> { mCategory = "XBOX"; mTextViewCategory.setText(mCategory); });
        mImageViewNitendo.setOnClickListener(view -> { mCategory = "NINTENDO"; mTextViewCategory.setText(mCategory); });
    }

    private void selectOptionImage(final int numberImage) {
        mBuilderSelector.setItems(options, (dialogInterface, i) -> {
            if (i == 0) {
                if (numberImage == 1) openGallery(GALLERY_REQUEST_CODE);
                else openGallery(GALLERY_REQUEST_CODE_2);
            } else {
                if (numberImage == 1) takePhoto(PHOTO_REQUEST_CODE);
                else takePhoto(PHOTO_REQUEST_CODE_2);
            }
        });
        mBuilderSelector.show();
    }

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

    private File createPhotoFile(int requestCode) throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile = File.createTempFile(new Date() + "_photo", ".jpg", storageDir);
        if (requestCode == PHOTO_REQUEST_CODE) {
            mPhotoPath = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath = photoFile.getAbsolutePath();
        } else {
            mPhotoPath2 = "file:" + photoFile.getAbsolutePath();
            mAbsolutePhotoPath2 = photoFile.getAbsolutePath();
        }
        return photoFile;
    }

    private void clickPost() {
        mTitle = mTextInputTitle.getText().toString().trim();
        mDescription = mTextInputDescription.getText().toString().trim();

        if (mTitle.isEmpty() || mDescription.isEmpty() || mCategory.isEmpty()) {
            Toast.makeText(this, "Completa los campos para publicar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mSwitchPoll.isChecked()) {
            String optA = mTextInputPollOptionA.getText().toString().trim();
            String optB = mTextInputPollOptionB.getText().toString().trim();
            if (optA.isEmpty() || optB.isEmpty()) {
                Toast.makeText(this, "Completa las dos opciones de la encuesta", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (mIsEdit) {
            // Edición: si no cambió imagen, usar la existente
            File newFile1 = mImageFile != null ? mImageFile : mPhotoFile;
            File newFile2 = mImageFile2 != null ? mImageFile2 : mPhotoFile2;

            if (newFile1 != null && newFile2 != null) {
                updateWithBothImages(newFile1, newFile2);
            } else if (newFile1 != null) {
                updateWithImage1Only(newFile1);
            } else if (newFile2 != null) {
                updateWithImage2Only(newFile2);
            } else {
                // No cambió ninguna imagen
                saveUpdate(mExistingImage1, mExistingImage2);
            }
        } else {
            File file1 = mImageFile != null ? mImageFile : mPhotoFile;
            File file2 = mImageFile2 != null ? mImageFile2 : mPhotoFile2;
            if (file1 != null && file2 != null) {
                saveImage(file1, file2);
            } else {
                Toast.makeText(this, "Debes seleccionar ambas imágenes", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateWithBothImages(File file1, File file2) {
        mDialog.show();
        mImageProvider.save(this, file1).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error imagen 1", Toast.LENGTH_SHORT).show(); return; }
            mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri1 -> {
                mImageProvider.save(this, file2).addOnCompleteListener(task2 -> {
                    if (!task2.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error imagen 2", Toast.LENGTH_SHORT).show(); return; }
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri2 -> saveUpdate(uri1.toString(), uri2.toString()));
                });
            });
        });
    }

    private void updateWithImage1Only(File file1) {
        mDialog.show();
        mImageProvider.save(this, file1).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error imagen 1", Toast.LENGTH_SHORT).show(); return; }
            mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri1 -> saveUpdate(uri1.toString(), mExistingImage2));
        });
    }

    private void updateWithImage2Only(File file2) {
        mDialog.show();
        mImageProvider.save(this, file2).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error imagen 2", Toast.LENGTH_SHORT).show(); return; }
            mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri2 -> saveUpdate(mExistingImage1, uri2.toString()));
        });
    }

    private void saveUpdate(String url1, String url2) {
        if (!mDialog.isShowing()) mDialog.show();
        mPostProvider.update(mPostId, mTitle, mDescription, mCategory, url1, url2).addOnCompleteListener(task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Publicación actualizada", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "No se pudo actualizar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImage(File imageFile1, File imageFile2) {
        mDialog.show();
        mImageProvider.save(PostActivity.this, imageFile1).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error al guardar imagen 1", Toast.LENGTH_LONG).show(); return; }
            mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                String url1 = uri.toString();
                mImageProvider.save(PostActivity.this, imageFile2).addOnCompleteListener(task2 -> {
                    if (!task2.isSuccessful()) { mDialog.dismiss(); Toast.makeText(this, "Error al guardar imagen 2", Toast.LENGTH_SHORT).show(); return; }
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(uri2 -> savePost(url1, uri2.toString()));
                });
            });
        });
    }

    private void savePost(String url1, String url2) {
        boolean withPoll = mSwitchPoll.isChecked();
        String optA = withPoll ? mTextInputPollOptionA.getText().toString().trim() : "";
        String optB = withPoll ? mTextInputPollOptionB.getText().toString().trim() : "";

        Post post = new Post();
        post.setImage1(url1);
        post.setImage2(url2);
        post.setTitle(mTitle);
        post.setDescription(mDescription);
        post.setCategory(mCategory);
        post.setIdUser(mAuthProvider.getUid());
        post.setTimestamp(new Date().getTime());
        post.setHasPoll(withPoll);

        mPostProvider.save(post).addOnCompleteListener(task -> {
            mDialog.dismiss();
            if (task.isSuccessful()) {
                String myId = mAuthProvider.getUid();
                mXPProvider.addXP(myId, RankHelper.XP_POST_CREATED);
                new MissionsProvider().incrementProgress(myId, MissionsProvider.TYPE_POST);
                notifyFollowers(myId, mTitle, post.getId());

                if (withPoll) {
                    Poll poll = new Poll();
                    poll.setPostId(post.getId());
                    poll.setOptionA(optA);
                    poll.setOptionB(optB);
                    mPollProvider.save(poll);
                }

                Toast.makeText(this, "Publicacion guardada correctamente", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "No se pudo guardar la publicacion", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyFollowers(String myId, String postTitle, String postId) {
        mUsersProvider.getUser(myId).addOnSuccessListener(doc -> {
            String myUsername = doc.exists() && doc.getString("username") != null
                    ? doc.getString("username") : "Alguien";
            String body = "@" + myUsername + " publicó: " + postTitle;

            mFollowProvider.getFollowers(myId).get().addOnSuccessListener(snap -> {
                for (com.google.firebase.firestore.DocumentSnapshot followDoc : snap.getDocuments()) {
                    String followerId = followDoc.getString("idFollower");
                    if (followerId == null) continue;

                    Notification notif = new Notification();
                    notif.setType("post");
                    notif.setIdFrom(myId);
                    notif.setIdTo(followerId);
                    notif.setIdPost(postId);
                    notif.setBody(body);
                    notif.setRead(false);
                    notif.setTimestamp(new Date().getTime());
                    mNotificationsProvider.save(notif);

                    mUsersProvider.getUser(followerId).addOnSuccessListener(followerDoc -> {
                        String fcmToken = followerDoc.getString("fcmToken");
                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            FCMSender.send(getApplicationContext(), fcmToken, "Nueva publicación 🎮", body);
                        }
                    });
                }
            });
        });
    }

    private void openGallery(int requestCode) {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                mPhotoFile = null;
                mImageFile = FileUtil.from(this, data.getData());
                mImageViewPost1.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
            } catch(Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == GALLERY_REQUEST_CODE_2 && resultCode == RESULT_OK) {
            try {
                mPhotoFile2 = null;
                mImageFile2 = FileUtil.from(this, data.getData());
                mImageViewPost2.setImageBitmap(BitmapFactory.decodeFile(mImageFile2.getAbsolutePath()));
            } catch(Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
