package com.example.simpleiptv;

import android.app.Application;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IPTVApplication extends Application implements Configuration.Provider {

    private static final String TAG = "IPTVApplication";
    public static boolean permissionCheck = false;
    public static List<IptvChannel> channelList;
    public static List<IptvChannel> favoriteChannels;
    private static IPTVApplication instance;
    private WorkManager workManager;

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
        workManager = WorkManager.getInstance(this);

        //castContext = CastContext.getSharedInstance(this);

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

        initializeWork();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().build();
    }

    private void initializeWork() {
        if (isFileExists("country_tld.json")) {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CountryTLDWorker.class).build();
            workManager.enqueue(request);
        }

        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest fetchM3URequest = new PeriodicWorkRequest.Builder(
                FetchM3UWorker.class, 1, TimeUnit.DAYS
        ).setConstraints(constraints).build();

        workManager.enqueue(fetchM3URequest);
        workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                fetchM3URequest);
    }


    private boolean isFileExists(String filename) {
        File file = new File(getApplicationContext().getFilesDir(), filename);
        return file.exists();
    }
}
