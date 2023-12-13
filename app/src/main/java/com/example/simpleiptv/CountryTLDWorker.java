package com.example.simpleiptv;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CountryTLDWorker extends Worker {

    private static final String TAG = "CountryTLDWorker";

    public CountryTLDWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Map<String, String> countryTLDMap = new HashMap<>();

        try {
            URL url = new URL("https://restcountries.com/v3.1/all");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONArray countriesArray = new JSONArray(response.toString());

            for (int i = 0; i < countriesArray.length(); i++) {
                JSONObject countryObject = countriesArray.getJSONObject(i);
                String name = countryObject.getString("name");
                String tld = countryObject.optString("tld");

                countryTLDMap.put(name, tld);
            }

            reader.close();
            inputStream.close();
            // Convert Map to JSONObject
            JSONObject jsonMap = new JSONObject(countryTLDMap);
            Log.d(TAG, "doWork(jsonMap): " + jsonMap);

            // Write JSON data to a file
            String filename = "country_tld.json";
            OutputStream outputStream = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(jsonMap.toString().getBytes());
            outputStream.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return Result.failure();
        }

        // Handle the populated map here
        for (Map.Entry<String, String> entry : countryTLDMap.entrySet()) {
            Log.d("CountryTLDMap", "Country: " + entry.getKey() + ", TLD: " + entry.getValue());
        }


        return Result.success();
    }
}
