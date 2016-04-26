package com.sony.sel.tvapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;

public class SettingsHelper {

  private static final String LOG_TAG = SettingsHelper.class.getSimpleName();

  private static final String PREFS_FILE = "prefs";

  private static final String EPG_SERVER = "EpgServer";
  private static final String CURRENT_CHANNEL = "CurrentChannel";
  private static final String CHANNEL_VIDEOS = "ChannelVideos";
  private static final String FAVORITE_CHANNELS = "FavoriteChannels";

  private static SettingsHelper INSTANCE;
  private List<VideoItem> channelVideos;

  private static final String[] DEFAULT_CHANNEL_VIDEOS = null; // { "file:///sdcard/Movies/tvapp.mp4" };

  private Context context;

  /**
   * Get helper instance.
   */
  public static SettingsHelper getHelper(Context context) {
    if (INSTANCE == null) {
      // ensure application context is used to prevent leaks
      INSTANCE = new SettingsHelper(context.getApplicationContext());
    }
    return INSTANCE;
  }

  private SettingsHelper(Context context) {
    this.context = context;
  }

  private SharedPreferences getSharedPreferences() {
    return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
  }

  /**
   * Return the current EPG server
   */
  public String getEpgServer() {
    return getSharedPreferences().getString(EPG_SERVER, null);
  }

  /**
   * Set the current EPG server
   */
  public void setEpgServer(String server) {
    if (!server.equals(getEpgServer())) {
      SharedPreferences prefs = getSharedPreferences();
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString(EPG_SERVER, server);
      editor.commit();
      EventBus.getInstance().post(new EventBus.EpgServerChangedEvent(server));
    }
  }

  public DlnaObjects.VideoBroadcast getCurrentChannel() {
    String channelString = getSharedPreferences().getString(CURRENT_CHANNEL, null);
    if (channelString != null) {
      return new Gson().fromJson(channelString, DlnaObjects.VideoBroadcast.class);
    }
    // no channel
    return null;
  }

  public void setCurrentChannel(DlnaObjects.VideoBroadcast channel) {

    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(CURRENT_CHANNEL, channel.toString());
    editor.commit();
    EventBus.getInstance().post(new EventBus.ChannelChangedEvent(channel));

  }

  /**
   * Class for deserializing videos
   */
  private static class VideoList extends ArrayList<VideoItem> {
  }

  public List<VideoItem> getChannelVideos() {
    if (channelVideos == null) {
      String videosString = getSharedPreferences().getString(CHANNEL_VIDEOS, null);
      if (videosString != null) {
        channelVideos = new Gson().fromJson(videosString, VideoList.class);
      } else if (DEFAULT_CHANNEL_VIDEOS != null) {
        channelVideos = new ArrayList<>();
        for (String video : DEFAULT_CHANNEL_VIDEOS) {
          VideoItem item = new VideoItem();
          item.setRes(video);
          channelVideos.add(item);
        }
      }
    }
    return channelVideos;
  }

  private void saveChannelVideos() {
    if (channelVideos != null) {
      SharedPreferences prefs = getSharedPreferences();
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString(CHANNEL_VIDEOS, new Gson().toJson(channelVideos));
      editor.commit();
    }
  }

  public void addChannelVideo(VideoItem video) {
    List<VideoItem> videos = getChannelVideos();
    videos.add(video);
    saveChannelVideos();
  }

  public void removeChannelVideo(VideoItem video) {
    List<VideoItem> videos = getChannelVideos();
    videos.remove(video);
    saveChannelVideos();
  }

  public void clearChannelVideos() {
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(CHANNEL_VIDEOS);
    editor.commit();
    channelVideos = new ArrayList<>();
  }

  public Set<String> getFavoriteChannels() {
    Set<String> channelList = getSharedPreferences().getStringSet(FAVORITE_CHANNELS, null);
    if (channelList == null) {
      channelList = new HashSet<>();
    }
    return channelList;
  }

  public void addFavoriteChannel(String channelId) {
    Set<String> channelList = getFavoriteChannels();
    channelList.add(channelId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(FAVORITE_CHANNELS, channelList);
    editor.commit();
  }

  public void removeFavoriteChannel(String channelId) {
    Set<String> channelList = getFavoriteChannels();
    channelList.remove(channelId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(FAVORITE_CHANNELS, channelList);
    editor.commit();
  }

}