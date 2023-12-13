package com.example.simpleiptv;


import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;


public class ChannelPlayerActivity extends AppCompatActivity implements Player.Listener {

    private static final String TAG = "ChannelPlayerActivity";
    private static final long PLAYER_INITIALIZATION_TIMEOUT_MS = 30000; // 30 seconds
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private SimpleExoPlayer player;
    private ProgressBar loadingIndicator;
    private PlayerView playerView;
    private DefaultTrackSelector trackSelector;
    private int channelPos;
    private String channelUrl;
    private TextView channelNametxt;
    private String channelName;
    private final Runnable timeoutRunnable = () -> {
        // Handle timeout: release player or perform any necessary action
        // releasePlayer();
        // finish();
        // Show an error message or take appropriate action
        switchChannel();
    };
    private IptvChannel currentChannel;
    private boolean buttonPressNext = false;
    private boolean buttonPressPrev = false;

    private void switchChannel() {
        if (this.buttonPressNext) {
            Toast.makeText(this, this.currentChannel.getChannelName() + " could not be played.", Toast.LENGTH_SHORT).show();
            loadNextChannel();
        }
        if (this.buttonPressPrev) {
            Toast.makeText(this, this.currentChannel.getChannelName() + " could not be played.", Toast.LENGTH_SHORT).show();
            loadPreviousChannel();
        }
        if (!buttonPressNext && !buttonPressPrev) {
            releasePlayer();
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_player);

        // Enable immersive mode
        hideSystemUI();

        loadingIndicator = findViewById(R.id.loading_indicator);


        playerView = findViewById(R.id.player_view);
        channelNametxt = findViewById(R.id.channelNametxt);

        // Get the channel URL from Intent extras
        channelPos = getIntent().getIntExtra("channelPos", 0);
        currentChannel = IPTVApplication.channelList.get(channelPos);
        channelUrl = currentChannel.getChannelUrl();
        channelName = currentChannel.getChannelName();

        channelNametxt.setText(channelName);

    }

    private void initializePlayer(String videoUrl) {
        trackSelector = new DefaultTrackSelector(this);
        // Global settings.

/*
        ExoPlayer player =
                new ExoPlayer.Builder(this)
                        .setMediaSourceFactory(
                                new DefaultMediaSourceFactory(this, new DefaultTrackSelector()).setLiveTargetOffsetMs(5000))
                        .build();

// Per MediaItem settings.
        MediaItem mediaItem =
                new MediaItem.Builder()
                        .setUri(videoUrl)
                        .setLiveConfiguration(
                                new MediaItem.LiveConfiguration.Builder()
                                        .setMaxPlaybackSpeed(1.02f)
                                        .build())
                        .build();
        player.setMediaItem(mediaItem);*/

        player = new SimpleExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();

        // Start the timeout runnable after PLAYER_INITIALIZATION_TIMEOUT_MS milliseconds
        timeoutHandler.postDelayed(timeoutRunnable, PLAYER_INITIALIZATION_TIMEOUT_MS);

        // Once the player is ready, hide the loading indicator
        player.addListener(new Player.Listener() {

            @Override
            public void onTracksChanged(Tracks tracks) {
                Player.Listener.super.onTracksChanged(tracks);
                for (int i = 0; i < tracks.getGroups().get(i).getMediaTrackGroup().length; i++) {
                    TrackGroup trackGroup = tracks.getGroups().get(i).getMediaTrackGroup();
                    for (int j = 0; j < trackGroup.length; j++) {
                        Format format = trackGroup.getFormat(j);
                        Log.d("Track Info", "Codec: " + format.sampleMimeType);
                    }
                }

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    // Remove the timeout runnable when player state is ready or ended
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    hideLoadingIndicator();

                    ObjectAnimator reverseAnimator = ObjectAnimator.ofFloat(channelNametxt, "alpha", 0.8f, 0f);
                    reverseAnimator.setDuration(1000);
                    reverseAnimator.start();
                }
                if (playbackState == Player.STATE_BUFFERING) {

                    showLoadingIndicator();
                    // Create an ObjectAnimator to animate the alpha property of the TextView
                    ObjectAnimator animator = ObjectAnimator.ofFloat(channelNametxt, "alpha", 0f, 0.8f);
                    animator.setDuration(1000); // Set the animation duration in milliseconds

// Start the animation
                    animator.start();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Throwable cause = error.getCause();
                Log.d(TAG, "onPlayerError: " + error.getErrorCodeName() + " | " + error);

                if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                    HttpDataSource.InvalidResponseCodeException responseCodeException =
                            (HttpDataSource.InvalidResponseCodeException) cause;
                    int responseCode = responseCodeException.responseCode;
                    // Handle the 404 error (Not Found) or other response codes
                    IPTVApplication.channelList.remove(currentChannel);


                    if (responseCode == 404) {
                        Toast.makeText(ChannelPlayerActivity.this, "Channel not found (" + currentChannel.getChannelName() + ")", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ChannelPlayerActivity.this, "Playback error (" + currentChannel.getChannelName() + "): " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                    switchChannel();
                } else {
                    Toast.makeText(ChannelPlayerActivity.this, "Playback error (" + currentChannel.getChannelName() + ") : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    switchChannel();
                }

                // Handle other ExoPlayer errors if needed
            }


        });

        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        playerView.setPlayer(player);

        playerView.findViewById(R.id.exo_next).setOnClickListener(v -> loadNextChannel());
        playerView.findViewById(R.id.exo_prev).setOnClickListener(v -> loadPreviousChannel());
        TextView titleText = playerView.findViewById(R.id.title);
        titleText.setText("Live Stream: " + currentChannel.getChannelName());


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer(channelUrl); // Use the channel URL from Intent extras
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer(channelUrl);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void showLoadingIndicator() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void loadNextChannel() {
        if (channelPos < IPTVApplication.channelList.size() - 1) {
            channelPos++;
            currentChannel = IPTVApplication.channelList.get(channelPos);
            channelUrl = currentChannel.getChannelUrl();
            channelName = currentChannel.getChannelName();
            channelNametxt.setText(channelName);
            releasePlayer();
            initializePlayer(channelUrl);
            buttonPressNext = true;
            buttonPressPrev = false;
        }
    }

    private void loadPreviousChannel() {
        if (channelPos > 0) {
            channelPos--;
            currentChannel = IPTVApplication.channelList.get(channelPos);
            channelUrl = currentChannel.getChannelUrl();
            channelName = currentChannel.getChannelName();
            channelNametxt.setText(channelName);
            releasePlayer();
            initializePlayer(channelUrl);
            buttonPressNext = false;
            buttonPressPrev = true;

        }
    }


}
