package com.example.simpleiptv;

import static com.example.simpleiptv.IPTVApplication.isH264DecoderSupported;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ArrayList<String> channelNames;
    private ChannelAdapter channelAdapter;
    private RecyclerView recyclerView;
    private TextView notFound;
    private TextView toolbarTitle;
    private List<IptvChannel> channelList; // Declaration of channelList
    private WorkManager workManager;
    private TextView progressIndicator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_screen); // Display the loading screen initially

        checkPermissions();
        isH264DecoderSupported();

        workManager = WorkManager.getInstance(this);
        initializeWork();

        initializeApp();


    }

    private void initializeApp() {
        // Perform initialization tasks here (e.g., fetching data, setting up resources)
        // Simulating a delay using a Handler. Replace this with your actual initialization process.

        new Handler().postDelayed(() -> {
            // Initialization complete; switch to the main UI
            if (isFileExists("channels.json")) {
                switchToMainUI();
            } else {
                initializeApp();
            }
        }, 3000); // Simulated delay of 3 seconds (replace this with your actual initialization time)
    }

    private void switchToMainUI() {
        setContentView(R.layout.activity_main);


        //setupUI();


     /*   if (isFileExists("country_tld.json")) {

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CountryTLDWorker.class).build();
            WorkManager.getInstance(this).enqueue(request);
        }*/
        recyclerView = findViewById(R.id.recyclerView);
        notFound = findViewById(R.id.notFound);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        channelNames = new ArrayList<>();
        //adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, channelNames);


        // Check if the JSON file exists
        if (isFileExists("channels.json")) {
            // If the file exists, update the channel list
            retrieveUpdatedChannelList();
        } else {


        }

     /*   // Set constraints for the WorkManager (optional)
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();

// Create the PeriodicWorkRequest to run once a day
        PeriodicWorkRequest fetchM3URequest = new PeriodicWorkRequest.Builder(
                FetchM3UWorker.class, // Your worker class
                1, // Repeat once a day
                TimeUnit.DAYS
        ).setConstraints(constraints).build();

// Enqueue the PeriodicWorkRequest
        WorkManager.getInstance(this).enqueue(fetchM3URequest);

// Observe the WorkInfo to update the RecyclerView when work finishes
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(fetchM3URequest.getId()).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                Toast.makeText(this, "WorkManager task completed", Toast.LENGTH_SHORT).show();
                retrieveUpdatedChannelList();
            }
        });*/

/*        // Set a click listener for channel selection
        channelListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedChannel = channelNames.get(position);
            // Implement logic to play the selected channel using ExoPlayer or other player
            Toast.makeText(MainActivity.this, "Selected: " + selectedChannel, Toast.LENGTH_SHORT).show();
        });*/
        CheckBox hdCheckBox = findViewById(R.id.hdCheckBox);
        hdCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                channelAdapter.filterHDChannels();
                toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");
            } else {
                channelAdapter.removeFilter();
                toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");

            }
        });

        CheckBox favoritesCheckbox = findViewById(R.id.favoritesCheckbox);
        favoritesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                channelAdapter.filterByFavorites();
                toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");
            } else {
                channelAdapter.removeFilter();
                toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");

            }
        });


        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not needed, but required to implement
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Filter the list based on the entered text
                if ((i >= 2 && i2 == 1) || (i >= 3 && i1 == 1)) {
                    channelAdapter.filterChannels(charSequence.toString());

                    if (channelAdapter.getItemCount() == 0) {
                        notFound.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);

                    } else {
                        notFound.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);


                    }

                } else {
                    channelAdapter.removeFilter();
                    notFound.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                //  Log.d(TAG, "onTextChanged: " + i + " " + i1 + " " + i2 + " ");
                toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not needed, but required to implement
            }
        });

    }

    private void populateSpinner() {
        Spinner groupTitleSpinner = findViewById(R.id.groupTitleSpinner);
        List<String> uniqueGroupTitles = null;

// Get unique groupTitles
        uniqueGroupTitles = getUniqueGroupTitles(channelList);

        Collections.sort(uniqueGroupTitles);

// Add "No Filter" option at the beginning of the list
        uniqueGroupTitles.add(0, "No Filter");

// Set up adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                uniqueGroupTitles
        );
        groupTitleSpinner.setAdapter(spinnerAdapter);

// Listener for spinner selection
        groupTitleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedGroupTitle = (String) parentView.getItemAtPosition(position);

                if (position == 0) {
                    // No Filter selected, display all channels
                    channelAdapter.removeFilter(); // Implement this method to remove the filter
                } else {
                    // Apply filter based on selected groupTitle
                    channelAdapter.filterChannelsByGroupTitle(selectedGroupTitle);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing or display default list
            }
        });
    }

    private List<String> getUniqueGroupTitles(List<IptvChannel> channelList) {
        List<String> uniqueGroupTitles = new ArrayList<>();
        HashSet<String> uniqueSet = new HashSet<>();

        for (IptvChannel channel : channelList) {
            if (uniqueSet.add(channel.getGroupTitle())) {
                uniqueGroupTitles.add(channel.getGroupTitle());
            }
        }

        return uniqueGroupTitles;
    }


    private void retrieveUpdatedChannelList() {
        String jsonChannels = readFromFile("channels.json");
        if (jsonChannels != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<IptvChannel>>() {
            }.getType();
            channelList = gson.fromJson(jsonChannels, listType);

            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Create an instance of ChannelAdapter and pass the channelList to it
            channelAdapter = new ChannelAdapter(this, channelList);
            recyclerView.setAdapter(channelAdapter);
            toolbarTitle.setText("Channel list (" + channelAdapter.getItemCount() + ")");
            populateSpinner();

        }
    }

    private boolean isFileExists(String filename) {
        File file = new File(getApplicationContext().getFilesDir(), filename);
        return file.exists();
    }

    private String readFromFile(String filename) {
        try {
            FileInputStream fileInputStream = openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }

            bufferedReader.close();
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadChannelListFromJson() {
        // Read JSON file from assets folder
        String json;
        try {
            InputStream inputStream = getAssets().open("channels.json");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
            json = stringBuilder.toString();

            // Convert JSON to list of IptvChannel using Gson
            Gson gson = new Gson();
            Type listType = new TypeToken<List<IptvChannel>>() {
            }.getType();
            channelList = gson.fromJson(json, listType);
            Toast.makeText(this, "Channels Count: " + channelList.size(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        observeWork(fetchM3URequest);
        progressIndicator = findViewById(R.id.progressIndicator);
    }


    private void observeWork(WorkRequest workRequest) {
        workManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, workInfo -> {
            if (workInfo != null) {
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Toast.makeText(this, "WorkManager task completed", Toast.LENGTH_SHORT).show();
                    retrieveUpdatedChannelList();
                }
                Data progress = workInfo.getProgress();
                if (progress != null && progress.getInt("progress", 0) != 0) {
                    int currentProgress = progress.getInt("progress", 0);
                    progressIndicator.setText(String.valueOf(currentProgress));
                    // Update your RecyclerView or UI based on the progress value
                    // For example:
                    // progressBar.setProgress(currentProgress);
                }

            }
        });


    }

    private void checkPermissions() {
        Dexter.withContext(this)
                .withPermissions(
                        android.Manifest.permission.ACCESS_NETWORK_STATE,
                        android.Manifest.permission.WAKE_LOCK,
                        android.Manifest.permission.INTERNET,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            IPTVApplication.permissionCheck = true;
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
}
