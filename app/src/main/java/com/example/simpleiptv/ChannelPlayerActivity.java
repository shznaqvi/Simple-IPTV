package com.example.simpleiptv;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

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
    private ImageView channelLogo;
    private ImageView channelnavbutton;
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

    private PowerManager.WakeLock wakeLock;
    private ImageView favoriteIcon;
    private ChannelAdapter channelAdapter;
    private TextView channleCount;
    private DrawerLayout drawerLayout;

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
        channelLogo = findViewById(R.id.channelLogo);
        channelnavbutton = findViewById(R.id.channelnavbutton);
        channleCount = findViewById(R.id.channleCount);

        // Get the channel URL from Intent extras
        channelPos = getIntent().getIntExtra("channelPos", 0);
        currentChannel = IPTVApplication.channelList.get(channelPos);
        channelUrl = currentChannel.getChannelUrl();
        channelName = currentChannel.getChannelName();

        channelNametxt.setText(channelName);
        setChannelLogo();
        NavigationView navigationView = findViewById(R.id.navigation_view);
        RecyclerView recyclerView = navigationView.findViewById(R.id.recycler_view); // Assuming RecyclerView is inside NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        channelnavbutton.setVisibility(View.VISIBLE);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create an instance of ChannelAdapter and pass the channelList to it
        channelAdapter = new ChannelAdapter(this, IPTVApplication.channelList);
        recyclerView.setAdapter(channelAdapter);
        channleCount.setText("(" + channelAdapter.getItemCount() + ")");
    }

    public void toggleChannelNav(View view) {

// To check if the left drawer (START) is currently open
        boolean isDrawerOpen = drawerLayout.isDrawerOpen(GravityCompat.START);

        if (isDrawerOpen) {
            drawerLayout.closeDrawer(GravityCompat.START);
            channelnavbutton.setVisibility(View.VISIBLE);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
            channelnavbutton.setVisibility(View.GONE);


        }

    }


    private void setChannelLogo() {
        // Load channel logo using Picasso
        if (currentChannel.getTvgLogo() != null && !currentChannel.getTvgLogo().isEmpty()) {
            Picasso.get().load(currentChannel.getTvgLogo()).into(channelLogo);
        } else {
            // Optionally, set a placeholder or default image if the URL is empty or null
            channelLogo.setImageResource(R.drawable.casivue_logo_gray);
        }
    }

    private void initializePlayer(String videoUrl) {
        favoriteIcon = findViewById(R.id.favoriteIcon);

        // Check if the channel is in favorites and update the icon accordingly
        if (IPTVApplication.favoriteChannels.contains(currentChannel)) {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
        }

        // Inside onBindViewHolder
        favoriteIcon.setOnClickListener(view -> {
            if (IPTVApplication.favoriteChannels.contains(currentChannel)) {
                FavoritesManager.removeFromFavorites(this, currentChannel);
                favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
                IPTVApplication.favoriteChannels.remove(currentChannel); // Update favorite channels list
            } else {
                addToFavorites(currentChannel);
                favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                IPTVApplication.favoriteChannels.add(currentChannel); // Update favorite channels list
            }
        });

        trackSelector = new DefaultTrackSelector(this);

  /*      castManager = new CastManager(this);
        MediaRouteButton mediaRouteButton = findViewById(R.id.media_route_button); // Replace with your MediaRouteButton

       castManager.initializeCastButton(mediaRouteButton);
        castManager.setSessionManagerListener();*/
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

                    ObjectAnimator reverseAnimator2 = ObjectAnimator.ofFloat(channelLogo, "alpha", 0.72f, 0f);
                    reverseAnimator2.setDuration(1000);
                    reverseAnimator2.start();

                    if (player.getVideoFormat() != null) {
                        int width = player.getVideoFormat().width;
                        int height = player.getVideoFormat().height;
                        Log.d(TAG, "onPlayerStateChanged(width): " + width);
                        Log.d(TAG, "onPlayerStateChanged(height): " + height);

                        if (width != Format.NO_VALUE && height != Format.NO_VALUE) {
                            int videoAspectRatio = width / height;
                            // Set the aspect ratio of the PlayerView's container
                            //  playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                        }
                    }
                }
                if (playbackState == Player.STATE_BUFFERING) {

                    showLoadingIndicator();
                    // Create an ObjectAnimator to animate the alpha property of the TextView
                    ObjectAnimator animator = ObjectAnimator.ofFloat(channelNametxt, "alpha", 0f, 0.8f);
                    animator.setDuration(1000); // Set the animation duration in milliseconds
                    animator.start();

                    ObjectAnimator animator2 = ObjectAnimator.ofFloat(channelLogo, "alpha", 0f, 0.72f);
                    animator2.setDuration(1000); // Set the animation duration in milliseconds
                    animator2.start();
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
                    channelAdapter.notifyDataSetChanged();


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
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "YourApp:WakeLockTag");
                wakeLock.acquire();
            }
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
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
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
            setChannelLogo();
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
            setChannelLogo();
            releasePlayer();
            initializePlayer(channelUrl);
            buttonPressNext = false;
            buttonPressPrev = true;

        }
    }

/*    public void onStartCastingButtonClick(View view) {
        // Call startCasting() method from CastManager when the button is clicked
        String mediaUrl = currentChannel.getChannelUrl();
        String title = currentChannel.getChannelName();
        castManager.loadMediaForCasting(mediaUrl, title);
    }*/


    private void addToFavorites(IptvChannel channel) {
        FavoritesManager.addToFavorites(this, channel);
        // Notify the user that the channel has been added to favorites
        Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        updateFavoriteChannels(this);
        // You might update the heart icon here to indicate it's now a favorite
        // For example, change the icon from outline to filled heart
        // holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
    }

    // Add a method to update favoriteChannels
    public void updateFavoriteChannels(Context context) {
        IPTVApplication.favoriteChannels = FavoritesManager.loadFavoriteChannels(context);
    }


}
