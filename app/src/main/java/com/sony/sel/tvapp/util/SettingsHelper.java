package com.sony.sel.tvapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;

public class SettingsHelper {

  private static final String LOG_TAG = SettingsHelper.class.getSimpleName();

  private static final String PREFS_FILE = "prefs";

  private static final String EPG_SERVER = "EpgServer";
  private static final String CURRENT_CHANNEL = "CurrentChannel";
  private static final String CHANNEL_VIDEOS = "ChannelVideos";
  private static final String FAVORITE_CHANNELS = "FavoriteChannels";
  private static final String RECORD_PROGRAMS = "RecordPrograms";
  private static final String RECORD_SERIES = "RecordSeries";
  private static final String USE_CHANNEL_VIDEO = "UseChannelVideo";
  private static final String AUTO_PLAY_CHANNEL_VIDEO = "AutoPlayChannelVideo";
  private static final String LOOP_VIDEO_PLAYBACK = "LoopVideoPlayback";
  private static final String VIDEO_POSITIONS = "VideoPositions";
  private static final String FAVORITE_PROGRAMS = "FavoritePrograms";
  private static final String VOD_TO_PLAY = "VodToPlay";
  private static final String PROGRAM_TO_RECORD = "ProgramToRecord";
  private static final String INPUT_ID = "InputID";

  private static SettingsHelper INSTANCE;
  private List<VideoItem> channelVideos;
  private Map<String, Integer> videoPositions;

  private static final String[] DEFAULT_CHANNEL_VIDEOS = null; // { "file:///sdcard/Movies/tvapp.mp4" };

  private Context context;
  private String currentSearchQuery;

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
      } else {
        channelVideos = new ArrayList<>();
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
    setToChannelVideoSetting(false);
  }

  public void setToChannelVideoSetting(boolean condition) {
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(USE_CHANNEL_VIDEO, condition);
    editor.commit();
  }

  public boolean useChannelVideosSetting() {
    return getSharedPreferences().getBoolean(USE_CHANNEL_VIDEO, false);
  }

  public void setAutoPlay(boolean condition) {
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(AUTO_PLAY_CHANNEL_VIDEO, condition);
    editor.commit();
  }

  public boolean getAutoPlay() {
    return getSharedPreferences().getBoolean(AUTO_PLAY_CHANNEL_VIDEO, true);
  }

  public void setLoopVideoPlayback(boolean condition) {
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(LOOP_VIDEO_PLAYBACK, condition);
    editor.commit();
  }

  public boolean getLoopVideoPlayback() {
    return getSharedPreferences().getBoolean(LOOP_VIDEO_PLAYBACK, true);
  }

  public Set<String> getFavoriteChannels() {
    return getSharedPreferences().getStringSet(FAVORITE_CHANNELS, new HashSet<String>());
  }

  public void addFavoriteChannel(String channelId) {
    Set<String> channelList = getFavoriteChannels();
    channelList.add(channelId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(FAVORITE_CHANNELS, channelList);
    editor.commit();
    EventBus.getInstance().post(new EventBus.FavoriteChannelsChangedEvent(channelList));
  }

  public void removeFavoriteChannel(String channelId) {
    Set<String> channelList = getFavoriteChannels();
    channelList.remove(channelId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(FAVORITE_CHANNELS, channelList);
    editor.commit();
    EventBus.getInstance().post(new EventBus.FavoriteChannelsChangedEvent(channelList));
  }

  public Set<String> getProgramsToRecord() {
    return getSharedPreferences().getStringSet(RECORD_PROGRAMS, new HashSet<String>());
  }

  public void addRecording(@NonNull VideoProgram program) {
    addRecording(program.getId());
  }

  public void addRecording(@NonNull String id) {
    Set<String> recordings = getProgramsToRecord();
    recordings.add(id);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(RECORD_PROGRAMS, recordings);
    editor.commit();
    EventBus.getInstance().post(new EventBus.RecordingsChangedEvent());
  }

  public void removeRecording(@NonNull VideoProgram program) {
    removeRecording(program.getId());
  }

  public void removeRecording(@NonNull String id) {
    Set<String> recordings = getProgramsToRecord();
    recordings.remove(id);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(RECORD_PROGRAMS, recordings);
    editor.commit();
    EventBus.getInstance().post(new EventBus.RecordingsChangedEvent());
  }

  public boolean isProgramRecorded(VideoProgram program) {
    return getProgramsToRecord().contains(program.getId());
  }

  public Set<String> getSeriesToRecord() {
    return getSharedPreferences().getStringSet(RECORD_SERIES, new HashSet<String>());
  }

  public void addSeriesRecording(@NonNull VideoProgram program) {
    addSeriesRecording(program.getSeriesId());
  }

  public void addSeriesRecording(@NonNull String seriesId) {
    Set<String> recordings = getSeriesToRecord();
    recordings.add(seriesId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(RECORD_SERIES, recordings);
    editor.commit();
    EventBus.getInstance().post(new EventBus.RecordingsChangedEvent());
  }

  public void removeSeriesRecording(@NonNull VideoProgram program) {
    removeSeriesRecording(program.getSeriesId());
  }

  public void removeSeriesRecording(@NonNull String seriesId) {
    Set<String> recordings = getSeriesToRecord();
    recordings.remove(seriesId);
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(RECORD_SERIES, recordings);
    editor.commit();
    EventBus.getInstance().post(new EventBus.RecordingsChangedEvent());
  }

  public boolean isSeriesRecorded(VideoProgram program) {
    return getSeriesToRecord().contains(program.getSeriesId());
  }

  public int getVideoPosition(String uri) {
    Map<String, Integer> positions = getVideoPositions();
    if (positions.containsKey(uri)) {
      return positions.get(uri);
    } else {
      return 0;
    }
  }

  public void saveVideoPosition(@NonNull String uri, int position) {
    Map<String, Integer> positions = getVideoPositions();
    positions.put(uri, position);
    saveVideoPositions(positions);
  }

  @NonNull
  private Map<String, Integer> getVideoPositions() {
    if (videoPositions == null) {
      String json = getSharedPreferences().getString(VIDEO_POSITIONS, null);
      if (json == null) {
        videoPositions = new HashMap<>();
      } else {
        videoPositions = new Gson().fromJson(json, new TypeToken<Map<String, Integer>>() {
        }.getType());
      }
    }
    return videoPositions;
  }

  private void saveVideoPositions(@NonNull Map<String, Integer> positions) {
    videoPositions = positions;
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(VIDEO_POSITIONS, new Gson().toJson(positions));
    editor.commit();
  }

  public Set<String> getFavoritePrograms() {
    return getSharedPreferences().getStringSet(FAVORITE_PROGRAMS, new HashSet<String>());
  }

  public void addFavoriteProgram(@NonNull VideoProgram program) {
    addFavoriteProgram(program.getSeriesId());
  }

  public void addFavoriteProgram(@NonNull String seriesId) {
    Set<String> favorites = getFavoritePrograms();
    favorites.add(seriesId);
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    editor.putStringSet(FAVORITE_PROGRAMS, favorites);
    editor.commit();
    EventBus.getInstance().post(new EventBus.FavoriteProgramsChangedEvent());
  }

  public void removeFavoriteProgram(@NonNull VideoProgram program) {
    removeFavoriteProgram(program.getSeriesId());
  }

  public void removeFavoriteProgram(@NonNull String seriesId) {
    Set<String> favorites = getFavoritePrograms();
    favorites.remove(seriesId);
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    editor.putStringSet(FAVORITE_PROGRAMS, favorites);
    editor.commit();
    EventBus.getInstance().post(new EventBus.FavoriteProgramsChangedEvent());
  }

  public boolean isFavoriteProgram(@NonNull VideoProgram program) {
    return getFavoritePrograms().contains(program.getSeriesId());
  }

  public VideoItem getVodItemToPlay() {
    String json = getSharedPreferences().getString(VOD_TO_PLAY, null);
    if (json != null) {
      return new Gson().fromJson(json, VideoItem.class);
    } else {
      return null;
    }
  }

  public void setVodItemToPlay(VideoItem vod) {
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    if (vod != null) {
      editor.putString(VOD_TO_PLAY, vod.toString());
    } else {
      editor.remove(VOD_TO_PLAY);
    }
    editor.commit();
  }


  public VideoProgram getProgramToRecord() {
    String json = getSharedPreferences().getString(PROGRAM_TO_RECORD, null);
    if (json != null) {
      return new Gson().fromJson(json, VideoProgram.class);
    } else {
      return null;
    }
  }

  public void setProgramToRecord(VideoProgram program) {
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    if (program != null) {
      editor.putString(PROGRAM_TO_RECORD, program.toString());
    } else {
      editor.remove(PROGRAM_TO_RECORD);
    }
    editor.commit();
  }

  public String getCurrentSearchQuery() {
    return currentSearchQuery;
  }

  public void setCurrentSearchQuery(String currentSearchQuery) {
    this.currentSearchQuery = currentSearchQuery;
  }

  public String getInputId() {
    return getSharedPreferences().getString(INPUT_ID, null);
  }

  public void setInputId(String inputId) {
    SharedPreferences.Editor editor = getSharedPreferences().edit();
    editor.putString(INPUT_ID, inputId);
    editor.commit();
  }


}