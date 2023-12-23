package com.example.simpleiptv;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FetchM3UWorker extends Worker {

    private static final String TAG = "FetchM3UWorker";
    private final Context context;

    public FetchM3UWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // Fetch M3U content
        //String urlString = "https://iptv-org.github.io/iptv/index.nsfw.m3u";
        String urlString = "https://iptv-org.github.io/iptv/index.category.m3u";
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder content = new StringBuilder();
                String line;
                int processedLines = 0;

                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");

                    // Update progress every 10 lines processed
                    processedLines++;
                    if (processedLines % 10 == 0) {
                        int progress = processedLines;
                        setProgressAsync(new Data.Builder().putInt("progress", progress).build());
                    }
                }
                String m3uContent = content.toString();

                // Process fetched M3U content, parse it, and handle channel information
                List<IptvChannel> channels = parseM3UContent(m3uContent);

                // Convert the channel list to JSON using Gson
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonChannels = gson.toJson(channels);

                // Save JSON data to a file in internal storage
                String filename = "channels.json";
                saveJSONToFile(jsonChannels, filename);

            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }


    private List<IptvChannel> parseM3UContent(String m3uContent) {
        List<IptvChannel> channels = new ArrayList<>();

        String[] lines = m3uContent.split("\\r?\\n");

        String currentChannelName = "";
        String currentChannelUrl = "";
        String currentChannelLogo = "";
        IptvChannel channel = new IptvChannel();

        for (int i = 0; i < lines.length; i++) {

            if (lines[i].startsWith("#EXTINF:-1")) {
                channel = new IptvChannel();
                if (i < lines.length) {
                    String infoLine = lines[i];
                    //Log.d(TAG, "parseM3UContent: infoLine: "+infoLine);
                    String[] parts = infoLine.split("\\s+");

                    // Initialize variables to store parsed values
                    String tvgId = "";
                    String tvgLogo = "";
                    String groupTitle = "";
                    String channelName = "";

                    // Loop through each part to extract information
                    for (String part : parts) {
                        if (part.startsWith("tvg-id=")) {
                            tvgId = part.substring(part.indexOf("\"") + 1, part.lastIndexOf("\""));
                        } else if (part.startsWith("tvg-logo=")) {
                            tvgLogo = part.substring(part.indexOf("\"") + 1, part.lastIndexOf("\""));
                        } else if (part.startsWith("group-title=")) {
                            groupTitle = part.substring(part.indexOf("\"") + 1, part.lastIndexOf("\""));
                            channelName = infoLine.substring(infoLine.indexOf(groupTitle) + groupTitle.length() + 2);

                        }
                    }

                    // Output parsed information
                   /* System.out.println("tvg-id: " + tvgId);
                    System.out.println("tvg-logo: " + tvgLogo);
                    System.out.println("groupTitle: " + groupTitle);
                    System.out.println("channelName: " + channelName);*/

                    channel.setChannelName(channelName);
                    // channel.setChannelCountry(convertTLDtoCountry(tvgId));
                    channel.setGroupTitle(groupTitle);
                    channel.setTvgLogo(tvgLogo);
                    channel.setTvgId(tvgId);

                }
            } else if (lines[i].startsWith("http")) {
                // Create an IptvChannel object and add it to the list
                channel.setChannelUrl(lines[i]);
                channels.add(channel);


            }
        }

        return channels;
    }

    private void saveJSONToFile(String json, String filename) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();
            Log.d(TAG, "JSON saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to save JSON!");
        }
    }

    private String convertTLDtoCountry(String tvgID) {


        String jsonString = null;
        JSONObject jsonObject = null;
        try {
            FileInputStream inputStream = getApplicationContext().openFileInput("country_tld.json");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            bufferedReader.close();

            // Convert JSON string to a HashMap
            jsonString = stringBuilder.toString();
            jsonObject = new JSONObject(jsonString);
            // Perform setCountryName using the data from jsonObject

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Load the JSON file and parse its content
        // String jsonContent = jsonString; // Load the JSON content from your file

        // Extract the country name using tvg-id's TLD

        // Extract the TLD from the tvg-id
        String[] parts = tvgID.split("\\.");
        String tld = parts[parts.length - 1]; // Get the last part as TLD

        // Lookup the TLD in the JSON data to get the country name
        String countryName = jsonObject.optString(tld, "Unknown");

        // Set the country name to the channel
        return countryName;
    }
}
