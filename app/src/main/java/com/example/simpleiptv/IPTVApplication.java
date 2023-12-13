package com.example.simpleiptv;

import android.app.Application;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import java.util.List;

public class IPTVApplication extends Application implements Configuration.Provider {

    private static final String TAG = "IPTVApplication";
    public static boolean permissionCheck = false;
    public static List<IptvChannel> channelList;
    private static IPTVApplication instance;

    public static IPTVApplication getInstance() {
        return instance;
    }

    public static boolean isH264DecoderSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecs = codecList.getCodecInfos();
            for (MediaCodecInfo codecInfo : codecs) {
                if (codecInfo.isEncoder()) {
                    continue;
                }
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase("video/avc")) {
                        Log.d(TAG, "Device supports H.264 (AVC) video codec");
                        return true;
                    }
                }
            }
        }
        Log.d(TAG, "Device does not support H.264 (AVC) video codec");
        return false;
    }

    // Add methods to handle global states or configurations if needed
    // For example:
    // public void setAuthToken(String token) { ... }
    // public String getAuthToken() { ... }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

       /* Log.d(TAG, "onCreate: This is IPTVApplication");
        WorkManager.initialize(this, new Configuration.Builder().build());
        try {
            // Check if the WorkManagerInitializer is enabled
            ApplicationInfo info = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);

            Bundle bundle = info.metaData;
            if (bundle != null) {
                boolean workManagerEnabled = bundle.getBoolean("androidx.work.workmanager-init", true);
                Log.d(TAG, "onCreate(workManagerEnabled): "+workManagerEnabled);
             *//*   if (!workManagerEnabled) {
                    Log.d(TAG, "onCreate(workManagerEnabled): "+workManagerEnabled);
                } else {
                    // WorkManagerInitializer is enabled in the manifest
                    // WorkManager should function normally
                }*//*
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate(message): "+e.getMessage());
        }*/
        // Initialize any components or services needed globally
        // For example:
        // Initialize networking libraries
        // Setup analytics tools
        // Configure logging
        // Initialize database connections
        // ... and so on
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().build();
    }
}
