package com.sony.localserver;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Class to serve channel and EPG data via a web socket.
 */
public class WebSocketServer extends NanoWSD {

  public static final String TAG = WebSocketServer.class.getSimpleName();

  private LocalWebSocket ws;
  private String udn;
  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;
  private SettingsHelper settingsHelper;

  public WebSocketServer(String host, int port, DlnaInterface dlnaHelper, DlnaCache dlnaCache, SettingsHelper settingsHelper) {
    super(host, port);
    this.dlnaHelper = dlnaHelper;
    this.settingsHelper = settingsHelper;
    this.dlnaCache = dlnaCache;
  }

  public String getUdn() {
    return udn;
  }

  public void setUdn(String udn) {
    this.udn = udn;
  }

  @Override
  protected WebSocket openWebSocket(IHTTPSession handshake) {
    Log.d(TAG, "OPEN WEBSOCKET");
    ws = new LocalWebSocket(handshake);
    return ws;
  }

  public class LocalWebSocket extends WebSocket {

    public LocalWebSocket(IHTTPSession handshakeRequest) {
      super(handshakeRequest);
    }

    @Override
    protected void onOpen() {
      Log.d(TAG, "OPEN");
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
      Log.d(TAG, "CLOSE WEBSOCKET");
    }


    @Override
    protected void onMessage(final WebSocketFrame message) {
      Log.d(TAG, "MESSAGE RECEIVED: " + message.getTextPayload());
      String payload = message.getTextPayload();
      if (payload.equalsIgnoreCase("Ping")) {
        try {
          ws.send("Ping");
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else if (payload.contains("browseEPGData")) {
        Log.d(TAG, "Browse EPG data.");
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            String json = processEpgRequest(message.getTextPayload());
            if (json != null) {
              try {
                ws.send(json);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        });
        t.start();
      } else if (payload.contains("searchEPGCache")) {
        Log.d(TAG, "Search EPG cache.");
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            long time = System.currentTimeMillis();
            String json = searchEpgCache(message.getTextPayload());
            Log.d(TAG, "Sending search EPG cache result. Time = " + (System.currentTimeMillis() - time) + ".");
            if (json != null) {
              try {
                ws.send(json);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        });
        t.start();
      } else if (payload.equals("browseEPGStations")) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Browse EPG stations.");
            String json = getChannels();
            if (json != null) {
              try {
                ws.send(json);
              } catch (IOException e) {
                Log.e(TAG, "Error sending Channels:" + e);
              }
            }
          }
        }).start();
      } else if (payload.equals("getRecordingsAndFavorites")) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Get recordings and favorites.");
            String json = getRecordingsAndFavorites();
            if (json != null) {
              try {
                ws.send(json);
              } catch (IOException e) {
                Log.e(TAG, "Error sending recordings and favorites:" + e);
              }
            }
          }
        }).start();
      } else if (payload.startsWith("keepUIVisible:")) {
        // send event for UI keep-alive
        String duration = payload.split(":")[1];
        if ("long".equalsIgnoreCase(duration)) {
          EventBus.getInstance().post(new EventBus.ResetUiTimerShortEvent());
        } else if ("short".equalsIgnoreCase(duration)) {
          EventBus.getInstance().post(new EventBus.ResetUiTimerLongEvent());
        } else if (Long.valueOf(duration) > 0) {
          EventBus.getInstance().post(new EventBus.ResetUiTimerEvent(Long.valueOf(duration)));
        }
      } else if (payload.startsWith("changeChannel:")) {
        // send channel change event
        String channelId = payload.split(":")[1];
        List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);
        for (final VideoBroadcast channel : channels) {
          if (channel.getChannelId().equals(channelId)) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                // send on UI thread
                settingsHelper.setCurrentChannel(channel);
              }
            });
            break;
          }
        }
      } else if (payload.startsWith("sendBackKeyToEPG:")) {
        // set whether the web view receives the back key (as a "B" keypress)
        String value = payload.split(":")[1];
        // send as an event to EpgFragment
        EventBus.getInstance().post(new EventBus.SendBackKeyToEpgEvent(value.equalsIgnoreCase("true")));
      } else if (payload.startsWith("getFavorites")) {
        try {
          ws.send(getFavorites());
        } catch (IOException e) {
          Log.e(TAG, "Error sending favorites:" + e);
        }
      } else if (payload.contains("editFavorites")) {
        editFavorites(payload);
      }
    }

    /**
     * Process a request to search the EPG cache.
     *
     * @param payload message payload.
     * @return response in JSON format.
     */
    String searchEpgCache(String payload) {
      // parse request from JSON
      SearchEpgCacheRequest request = new Gson().fromJson(payload, SearchEpgCacheRequest.class);

      List<VideoProgram> epgData;
      if (request.getData().getTimes().size() == 2 && request.getData().getChannels().size() > 0) {

        // extract dates
        Date startDate = new Date(Long.parseLong(request.getData().getTimes().get(0)));
        Date endDate = new Date(Long.parseLong(request.getData().getTimes().get(1)));

        // get data
        epgData = dlnaCache.searchEpg(udn, request.getData().getChannels(), startDate, endDate);

      } else {
        Log.e(TAG, "Invalid request data.");
        epgData = new ArrayList<>();
      }

      // create response
      SearchEpgCacheResponse response = new SearchEpgCacheResponse(
          epgData,
          settingsHelper.getFavoriteChannels(),
          getChannelIds(),
          settingsHelper.getCurrentChannel().getChannelId()
      );

      // convert to JSON
      Log.d(TAG, "Search EPG cache. Building JSON response.");
      long time = System.currentTimeMillis();
      String json = new GsonBuilder().
          registerTypeAdapter(VideoProgram.class, new VideoProgram.WebSerializer())
          .disableHtmlEscaping()
          .create()
          .toJson(response);
      Log.d(TAG, "Search EPG cache. JSON response took " + (System.currentTimeMillis() - time) + "ms to build.");
      return json;
    }

    /**
     * Process a request for EPG data
     *
     * @param payload message payload
     * @return response in JSON format
     */
    String processEpgRequest(String payload) {
      // parse request from JSON
      EpgRequest request = new Gson().fromJson(payload, EpgRequest.class);

      // extract dates
      Date startDate = new Date(Long.parseLong(request.getData().getTimes().get(0)));
      Date endDate = new Date(Long.parseLong(request.getData().getTimes().get(1)));

      // get data
      String json = getEpgData(request.getData().getChannels(), startDate, endDate);
      return json;
    }

    /**
     * Get EPG data for the requested channels and date interval
     *
     * @param channels  List of channel IDs
     * @param startDate Date for data to start
     * @param endDate   Date for data to end
     * @return response in JSON format
     */
    private String getEpgData(List<String> channels, Date startDate, Date endDate) {
      // create calendar to iterate dates
      Calendar calendar = Calendar.getInstance();
      // the structure we build and return is a map of channelId -> days -> programs
      Map<String, Map<String, Map<String, VideoProgram>>> channelMap = new LinkedHashMap<>();
      for (String channelId : channels) {
        Map<String, Map<String, VideoProgram>> dayMap = new LinkedHashMap<>();
        for (calendar.setTime(startDate); calendar.getTime().getTime() <= endDate.getTime(); calendar.add(Calendar.DAY_OF_MONTH, 1)) {
          DateFormat format = new SimpleDateFormat("M-d");
          String day = format.format(calendar.getTime());
          // get programs for the requested day
          List<DlnaObjects.VideoProgram> programs = dlnaHelper.getChildren(udn, "0/EPG/" + channelId + "/" + day, DlnaObjects.VideoProgram.class, null, true);
          // iterate programs and generate map entries
          Map<String, VideoProgram> programMap = new LinkedHashMap<>();
          for (VideoProgram program : programs) {
            if (program.getScheduledStartTime().getTime() <= endDate.getTime() && program.getScheduledEndTime().getTime() >= startDate.getTime()) {
              programMap.put(String.valueOf(program.getScheduledStartTime().getTime()), program);
            }
          }
          // add programs to the day map
          dayMap.put(day, programMap);
        }
        // add the day to the channel map
        channelMap.put(channelId, dayMap);
      }

      // build response
      EpgResponse response = new EpgResponse(
          channelMap,
          settingsHelper.getFavoriteChannels(),
          getChannelIds(),
          settingsHelper.getCurrentChannel().getChannelId()
      );

      // convert to JSON with special serializer
      return new GsonBuilder().
          registerTypeAdapter(VideoProgram.class, new VideoProgram.WebSerializer())
          .disableHtmlEscaping()
          .create()
          .toJson(response);
    }

    /**
     * Return the list of channels as a String set containing just channel IDs.
     *
     * @return
     */
    private List<String> getChannelIds() {
      List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);
      List<String> channelIds = new ArrayList<>();
      for (VideoBroadcast channel : channels) {
        channelIds.add(channel.getChannelId());
      }
      return channelIds;
    }

    /**
     * Return the list of channels as a JSON string.
     *
     * @return Channel list JSON.
     */
    String getChannels() {

      // build response
      ChannelResponse response = new ChannelResponse(
          settingsHelper.getFavoriteChannels(),
          dlnaHelper.getChannels(udn, null, true),
          settingsHelper.getCurrentChannel().getChannelId()
      );

      // convert to JSON with special serializer
      return new GsonBuilder().
          registerTypeAdapter(VideoBroadcast.class, new VideoBroadcast.WebSerializer())
          .disableHtmlEscaping()
          .create()
          .toJson(response);
    }

    /**
     * Return the list of program recordings, series recordings and favorite programs as JSON.
     *
     * @return Formatted JSON.
     */
    String getRecordingsAndFavorites() {

      RecordingsAndFavoritesResponse response = new RecordingsAndFavoritesResponse(
          settingsHelper.getProgramsToRecord(),
          settingsHelper.getSeriesToRecord(),
          settingsHelper.getFavoritePrograms()
      );

      // convert to JSON with special serializer
      return new GsonBuilder().
          registerTypeAdapter(VideoBroadcast.class, new VideoBroadcast.WebSerializer())
          .disableHtmlEscaping()
          .create()
          .toJson(response);
    }

    /**
     * Return the list of channels favorite as a JSON string.
     *
     * @return favorite list JSON.
     */
    String getFavorites() {
      Set<String> favorites = settingsHelper.getFavoriteChannels();
      JSONObject json = new JSONObject();
      JSONObject result = new JSONObject();
      JSONArray jarray = new JSONArray();
      for (String fav : favorites) {
        jarray.put(fav);
      }
      String currentplaying = settingsHelper.getCurrentChannel().getChannelId();
      try {
        json.put("FAVORITES", jarray);
        json.put("CURRENT_PLAYING", currentplaying);
        result.put("CURRENT", json);

        return result.toString();
      } catch (Exception e) {
        Log.e(TAG, "Exception parsing json: " + e);
        return "{CURRENT:{}}";
      }
    }

    void editFavorites(String payload) {
      EditFavoritesRequest request = new Gson().fromJson(payload, EditFavoritesRequest.class);
      EditFavoritesRequest.RequestData data = request.getRequestData();
      if (data.getAddFavoriteChannels() != null) {
        for (String channelId : data.getAddFavoriteChannels()) {
          settingsHelper.addFavoriteChannel(channelId);
        }
      }
      if (data.getRemoveFavoriteChannels() != null) {
        for (String channelId : data.getRemoveFavoriteChannels()) {
          settingsHelper.removeFavoriteChannel(channelId);
        }
      }
      if (data.getAddFavoritePrograms() != null) {
        for (String id : data.getAddFavoritePrograms()) {
          settingsHelper.addFavoriteProgram(id);
        }
      }
      if (data.getRemoveFavoritePrograms() != null) {
        for (String id : data.getRemoveFavoritePrograms()) {
          settingsHelper.removeFavoriteProgram(id);
        }
      }
      if (data.getAddProgramsToRecord() != null) {
        for (String id : data.getAddProgramsToRecord()) {
          settingsHelper.addRecording(id);
        }
      }
      if (data.getRemoveProgramsToRecord() != null) {
        for (String id : data.getRemoveProgramsToRecord()) {
          settingsHelper.removeRecording(id);
        }
      }
      if (data.getAddSeriesToRecord() != null) {
        for (String id : data.getAddSeriesToRecord()) {
          settingsHelper.addSeriesRecording(id);
        }
      }
      if (data.getRemoveProgramsToRecord() != null) {
        for (String id : data.getRemoveProgramsToRecord()) {
          settingsHelper.removeSeriesRecording(id);
        }
      }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {

    }

    @Override
    protected void onException(IOException exception) {

    }

  }

  /**
   * Class for parsing incoming epg requests from JSON
   */
  static class EpgRequestData {
    @SerializedName("CHANNELLIST")
    List<String> channels;
    @SerializedName("TIMELIST")
    List<String> times;


    public EpgRequestData(List<String> channels, List<String> times) {
      this.channels = channels;
      this.times = times;
    }

    public List<String> getChannels() {
      return channels;
    }

    public List<String> getTimes() {
      return times;
    }
  }

  /**
   * Class for parsing incoming epg requests from JSON
   */
  static class EpgRequest {
    @SerializedName("browseEPGData")
    EpgRequestData data;

    public EpgRequestData getData() {
      return data;
    }

    public EpgRequest(List<String> channels, List<String> times) {
      this.data = new EpgRequestData(channels, times);
    }
  }

  /**
   * Class for parsing incoming epg requests from JSON
   */
  static class SearchEpgCacheRequest {
    @SerializedName("searchEPGCache")
    EpgRequestData data;

    public EpgRequestData getData() {
      return data;
    }

    public SearchEpgCacheRequest(List<String> channels, List<String> times) {
      this.data = new EpgRequestData(channels, times);
    }
  }

  /**
   * Class for the EPG response JSON.
   */
  static class EpgResponse {
    @SerializedName("EPG")
    Map<String, Map<String, Map<String, VideoProgram>>> channelMap;
    @SerializedName("FAVORITES")
    Set<String> favorites;
    @SerializedName("CHANNELS")
    List<String> channels;
    @SerializedName("CURRENT")
    String currentChannel;

    public EpgResponse(Map<String, Map<String, Map<String, VideoProgram>>> channelMap, Set<String> favorites, List<String> channels, String currentChannel) {
      this.channelMap = channelMap;
      this.favorites = favorites;
      this.channels = channels;
      this.currentChannel = currentChannel;
    }
  }

  static class SearchEpgCacheResponse {
    @SerializedName("PROGRAMS")
    List<VideoProgram> programs;
    @SerializedName("FAVORITES")
    Set<String> favorites;
    @SerializedName("CHANNELS")
    List<String> channels;
    @SerializedName("CURRENT")
    String currentChannel;

    public SearchEpgCacheResponse(List<VideoProgram> programs, Set<String> favorites, List<String> channels, String currentChannel) {
      this.programs = programs;
      this.favorites = favorites;
      this.channels = channels;
      this.currentChannel = currentChannel;
    }
  }

  /**
   * Class for the channel list response JSON.
   */
  private static class ChannelResponse {
    @SerializedName("FAVORITES")
    Set<String> favorites;
    @SerializedName("STATIONS")
    List<VideoBroadcast> channels;
    @SerializedName("CURRENT")
    String currentChannel;

    public ChannelResponse(Set<String> favorites, List<VideoBroadcast> channels, String currentChannel) {
      this.favorites = favorites;
      this.channels = channels;
      this.currentChannel = currentChannel;
    }
  }

  private static class RecordingsAndFavoritesResponse {
    @SerializedName("RECORD_PROGRAMS")
    Set<String> programsToRecord;
    @SerializedName("RECORD_SERIES")
    Set<String> seriesToRecord;
    @SerializedName("FAVORITE_PROGRAMS")
    Set<String> favoritePrograms;

    public RecordingsAndFavoritesResponse(Set<String> programsToRecord, Set<String> seriesToRecord, Set<String> favoritePrograms) {
      this.programsToRecord = programsToRecord;
      this.seriesToRecord = seriesToRecord;
      this.favoritePrograms = favoritePrograms;
    }
  }

  private static class EditFavoritesRequest {

    @SerializedName("editFavorites")
    private RequestData requestData;

    public RequestData getRequestData() {
      return requestData;
    }

    private static class RequestData {
      @SerializedName("ADD_FAVORITE_PROGRAMS")
      private Set<String> addFavoritePrograms;
      @SerializedName("REMOVE_FAVORITE_PROGRAMS")
      private Set<String> removeFavoritePrograms;
      @SerializedName("ADD_FAVORITE_CHANNELS")
      private Set<String> addFavoriteChannels;
      @SerializedName("REMOVE_FAVORITE_CHANNELS")
      private Set<String> removeFavoriteChannels;
      @SerializedName("ADD_PROGRAMS_TO_RECORD")
      private Set<String> addProgramsToRecord;
      @SerializedName("REMOVE_PROGRAMS_TO_RECORD")
      private Set<String> removeProgramsToRecord;
      @SerializedName("ADD_SERIES_TO_RECORD")
      private Set<String> addSeriesToRecord;
      @SerializedName("REMOVE_SERIES_TO_RECORD")
      private Set<String> removeSeriesToRecord;

      public Set<String> getAddFavoritePrograms() {
        return addFavoritePrograms;
      }

      public Set<String> getRemoveFavoritePrograms() {
        return removeFavoritePrograms;
      }

      public Set<String> getAddProgramsToRecord() {
        return addProgramsToRecord;
      }

      public Set<String> getRemoveProgramsToRecord() {
        return removeProgramsToRecord;
      }

      public Set<String> getAddFavoriteChannels() {
        return addFavoriteChannels;
      }

      public Set<String> getRemoveFavoriteChannels() {
        return removeFavoriteChannels;
      }

      public Set<String> getAddSeriesToRecord() {
        return addSeriesToRecord;
      }

      public Set<String> getRemoveSeriesToRecord() {
        return removeSeriesToRecord;
      }
    }
  }

}
