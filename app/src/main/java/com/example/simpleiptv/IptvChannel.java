package com.example.simpleiptv;

import java.util.Objects;

public class IptvChannel {
    private static final String TAG = "IptvChannel";
    private String channelCountry;
    private String channelName;
    private String channelUrl;
    private String tvgId;
    private String tvgLogo;
    private String groupTitle;

    public IptvChannel() {
        // Default constructor
    }

    public IptvChannel(String name, String url, String tvgId, String tvgLogo, String groupTitle) {
        this.tvgId = tvgId;
        this.tvgLogo = tvgLogo;
        this.groupTitle = groupTitle;
        this.channelUrl = channelUrl;

        // Splitting tvgId to update channelName and channelCountry
        if (tvgId != null && tvgId.contains(".")) {
            String[] parts = tvgId.split("\\.");
            if (parts.length >= 2) {
                this.channelName = parts[0];
                this.channelCountry = parts[1];
            }
        }
    }

    public String getChannelCountry() {
        return channelCountry;
    }

    public void setChannelCountry(String channelCountry) {
        this.channelCountry = channelCountry;
    }

    // Getters and setters for each property

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        //   Log.d(TAG, "setChannelName: "+ channelName);
        this.channelName = channelName;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getTvgId() {
        return tvgId;
    }

    public void setTvgId(String tvgId) {
        this.tvgId = tvgId;
    }

    public String getTvgLogo() {
        return tvgLogo;
    }

    public void setTvgLogo(String tvgLogo) {
        this.tvgLogo = tvgLogo;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IptvChannel channel = (IptvChannel) obj;
        return Objects.equals(tvgId, channel.tvgId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tvgId);
    }
}
