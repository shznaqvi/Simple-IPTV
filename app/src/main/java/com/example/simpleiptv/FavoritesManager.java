package com.example.simpleiptv;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class FavoritesManager {
    private static final String FAVORITES_FILE_NAME = "favorites.json";
    private static final String TAG = "FavoritesManager";
    private static HashSet<String> favoriteIds;
    private static List<IptvChannel> favoriteChannels; // New variable to store loaded favorites

    public static List<IptvChannel> loadFavoriteChannels(Context context) {
        if (favoriteChannels != null) {
            return favoriteChannels; // If favorites are already loaded, return them directly
        }

        try {
            File favoritesFile = new File(context.getFilesDir(), FAVORITES_FILE_NAME);

            if (!favoritesFile.exists()) {
                try {
                    if (favoritesFile.createNewFile()) {
                        // File created successfully
                    } else {
                        // File creation failed
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileInputStream fileInputStream = context.openFileInput(FAVORITES_FILE_NAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();

            Gson gson = new Gson();
            Type listType = new TypeToken<List<IptvChannel>>() {
            }.getType();
            favoriteChannels = gson.fromJson(stringBuilder.toString(), listType);
            if (favoriteChannels == null) {
                favoriteChannels = new ArrayList<>(); // Initialize if null to prevent issues
            }
            return favoriteChannels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void saveFavoriteChannels(Context context, List<IptvChannel> favorites) {
        try {
            Gson gson = new Gson();
            List<IptvChannel> listWithDuplicates = favorites;
            Set<IptvChannel> setWithoutDuplicates = new LinkedHashSet<>(listWithDuplicates);
            List<IptvChannel> listWithoutDuplicates = new ArrayList<>(setWithoutDuplicates);
            String json = gson.toJson(listWithoutDuplicates);
            Log.d(TAG, "JSON saved successfully!" + json);
            FileOutputStream fileOutputStream = context.openFileOutput(FAVORITES_FILE_NAME, Context.MODE_PRIVATE);
            fileOutputStream.write(json.getBytes());
            fileOutputStream.close();

            favoriteChannels = favorites; // Update the loaded favorites after saving
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void loadFavoriteIds(Context context) {
        // Load favorite channel IDs from the JSON file into favoriteIds HashSet
        List<IptvChannel> favorites = loadFavoriteChannels(context);
        favoriteIds = new HashSet<>();
        for (IptvChannel channel : favorites) {
            favoriteIds.add(channel.getTvgId());
        }
    }

    public static void addToFavorites(Context context, IptvChannel newChannel) {
        if (favoriteIds == null) {
            loadFavoriteIds(context);
        }

        // Check if the channel already exists in favorites, if not, add it
        if (!favoriteIds.contains(newChannel.getTvgId())) {
            favoriteIds.add(newChannel.getTvgId());
            favoriteChannels.add(newChannel);
            saveFavoriteChannels(context, favoriteChannels);
        } else {
            Log.d(TAG, "addToFavorites: already exists " + newChannel.getChannelName());
        }
    }

    public static void removeFromFavorites(Context context, IptvChannel channel) {
        if (favoriteIds == null) {
            loadFavoriteIds(context);
        }

        // Remove the channel ID from favorites if it exists
        favoriteIds.remove(channel.getTvgId());
        favoriteChannels.remove(channel);
        saveFavoriteChannels(context, favoriteChannels);
    }

    public static boolean isFavorite(IptvChannel channel) {
        return favoriteIds != null && favoriteIds.contains(channel.getTvgId());
    }

}
