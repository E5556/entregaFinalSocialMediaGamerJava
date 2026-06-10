package com.optic.socialmediagamer.providers;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.optic.socialmediagamer.utils.CompressorBitmapImage;

import java.io.File;
import java.util.Date;
import java.util.Random;

public class ImageProvider {

    StorageReference mStorage;

    public ImageProvider() {
        mStorage = FirebaseStorage.getInstance().getReference();
    }

    public UploadTask save(Context context, File file) {
        byte[] imageByte = CompressorBitmapImage.getImage(context, file.getPath(), 500, 500);
        StorageReference storage = FirebaseStorage.getInstance().getReference().child(new Date().getTime() + ".jpg");
        mStorage = storage;
        UploadTask task = storage.putBytes(imageByte);
        return task;
    }

    public StorageReference getStorage() {
        return mStorage;
    }

    public UploadTask saveStoryImage(Uri uri) {
        StorageReference storage = FirebaseStorage.getInstance().getReference()
                .child("stories/" + new Date().getTime() + ".jpg");
        mStorage = storage;
        return storage.putFile(uri);
    }

    public UploadTask saveVideo(Context context, Uri uri) {
        StorageReference storage = FirebaseStorage.getInstance().getReference()
                .child("videos/" + new Date().getTime() + ".mp4");
        mStorage = storage;
        return storage.putFile(uri);
    }

}
