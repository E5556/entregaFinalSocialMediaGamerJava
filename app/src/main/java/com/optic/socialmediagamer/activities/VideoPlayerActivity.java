package com.optic.socialmediagamer.activities;

import android.media.MediaController;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.optic.socialmediagamer.R;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URL   = "videoUrl";
    public static final String EXTRA_TITLE = "videoTitle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        String url   = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        VideoView videoView = findViewById(R.id.videoViewPlayer);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        if (url != null && !url.isEmpty()) {
            videoView.setVideoURI(Uri.parse(url));
            videoView.requestFocus();
            videoView.start();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title != null ? title : "Video");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.imageViewVideoBack).setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onPause() {
        super.onPause();
        VideoView v = findViewById(R.id.videoViewPlayer);
        if (v != null && v.isPlaying()) v.pause();
    }
}
