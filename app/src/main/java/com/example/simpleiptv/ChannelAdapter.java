package com.example.simpleiptv;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private final List<IptvChannel> allChannelList;
    private final List<IptvChannel> filteredChannelList;
    private final Context mContext;
    private List<IptvChannel> favoriteChannels;

    // Constructor to initialize with a channel list
    public ChannelAdapter(Context context, List<IptvChannel> channelList) {
        this.mContext = context;
        IPTVApplication.channelList = channelList;
        filterAdultChannels();
        this.allChannelList = channelList;
        filteredChannelList = new ArrayList<>();
        this.favoriteChannels = FavoritesManager.loadFavoriteChannels(context); // Assign loaded favorites here


    }

    public void filterByFavorites() {
        favoriteChannels = FavoritesManager.loadFavoriteChannels(mContext);

        // Filter the channel list to show only favorite channels
        List<IptvChannel> filteredList = new ArrayList<>();
        for (IptvChannel channel : allChannelList) {
            if (favoriteChannels.contains(channel)) {
                filteredList.add(channel);
            }
        }

        // Update the RecyclerView with the filtered list
        updateChannelList(filteredList);

        notifyDataSetChanged();

    }

    public void filterChannelsByGroupTitle(String groupTitle) {
        List<IptvChannel> filteredChannels = new ArrayList<>();
        for (IptvChannel channel : IPTVApplication.channelList) {
            if (channel.getGroupTitle().equals(groupTitle)) {
                filteredChannels.add(channel);
            }
        }
        updateChannelList(filteredChannels);
    }


    public void filterAdultChannels() {
        IPTVApplication.channelList.removeIf(channel -> channel.getChannelName().contains("Adult"));
        IPTVApplication.channelList.removeIf(channel -> channel.getGroupTitle().contains("XXX"));


    }


    public void filterHDChannels() {
        List<IptvChannel> temp = new ArrayList<>();

        for (IptvChannel channel : IPTVApplication.channelList) {
            if (channel.getChannelName().contains("1080p")) {
                temp.add(channel);
            }
        }

        filteredChannelList.clear();
        filteredChannelList.addAll(temp);
        updateChannelList(temp);

        notifyDataSetChanged();
    }

    public void removeFilter() {
        filteredChannelList.clear();
        //filteredChannelList.addAll(IPTVApplication.channelList);
        updateChannelList(allChannelList);
        notifyDataSetChanged();
    }

    public void filterChannels(String query) {
        List<IptvChannel> filteredList = new ArrayList<>();

        for (IptvChannel channel : IPTVApplication.channelList) {
            if (channel.getChannelName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(channel);
            }
        }
        updateChannelList(filteredList);

        // Update the RecyclerView with the filtered list
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_item, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        int pos = position;
        IptvChannel channel = IPTVApplication.channelList.get(pos);
        holder.channelNameTextView.setText(channel.getChannelName());
        holder.channelCountryTextView.setText(channel.getGroupTitle());

        // Load channel logo using Picasso
        if (channel.getTvgLogo() != null && !channel.getTvgLogo().isEmpty()) {
            Picasso.get().load(channel.getTvgLogo()).into(holder.channelLogoImageView);
        } else {
            // Optionally, set a placeholder or default image if the URL is empty or null
            holder.channelLogoImageView.setImageResource(R.drawable.casivue_logo_gray);
        }

        // Set an OnClickListener to start ChannelPlayerActivity on item click
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, ChannelPlayerActivity.class);
            intent.putExtra("channelPos", pos);
            mContext.startActivity(intent);

        });


        // Check if the channel is in favorites and update the icon accordingly
        if (favoriteChannels.contains(channel)) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
        }

        // Inside onBindViewHolder
        holder.favoriteIcon.setOnClickListener(view -> {
            if (favoriteChannels.contains(channel)) {
                FavoritesManager.removeFromFavorites(mContext, channel);
                holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_outline);
                favoriteChannels.remove(channel); // Update favorite channels list
            } else {
                addToFavorites(channel);
                holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                favoriteChannels.add(channel); // Update favorite channels list
            }
        });


    }

    @Override
    public int getItemCount() {
        return IPTVApplication.channelList.size();
    }

    // Method to update the channel list
    public void updateChannelList(List<IptvChannel> newList) {
        IPTVApplication.channelList = newList;
        notifyDataSetChanged();
    }

    private void addToFavorites(IptvChannel channel) {
        FavoritesManager.addToFavorites(mContext, channel);
        // Notify the user that the channel has been added to favorites
        Toast.makeText(mContext, "Added to Favorites", Toast.LENGTH_SHORT).show();
        updateFavoriteChannels(mContext);
        // You might update the heart icon here to indicate it's now a favorite
        // For example, change the icon from outline to filled heart
        // holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
    }

    // Add a method to update favoriteChannels
    public void updateFavoriteChannels(Context context) {
        favoriteChannels = FavoritesManager.loadFavoriteChannels(context);
        notifyDataSetChanged();
    }

    // ViewHolder for each item in the RecyclerView
    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView channelNameTextView;
        TextView channelCountryTextView;
        ImageView channelLogoImageView;
        ImageView favoriteIcon;

        public ChannelViewHolder(View itemView) {
            super(itemView);
            channelNameTextView = itemView.findViewById(R.id.channel_name);
            channelCountryTextView = itemView.findViewById(R.id.channel_country);
            channelLogoImageView = itemView.findViewById(R.id.channel_logo);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}
