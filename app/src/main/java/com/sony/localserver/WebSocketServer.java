package com.sony.localserver;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
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
  private SettingsHelper settingsHelper;

  public WebSocketServer(String host, int port, DlnaInterface dlnaHelper, SettingsHelper settingsHelper) {
    super(host, port);
    this.dlnaHelper = dlnaHelper;
    this.settingsHelper = settingsHelper;
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
        t.setPriority(Thread.MIN_PRIORITY);
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
        List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null);
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
          Log.e(TAG, "Error sending Channels:" + e);
        }
      }
    }

    /**
     * Process a request for EPG data
     *
     * @param payload message payload
     * @return response in JSON format
     */
    private String processEpgRequest(String payload) {
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
     * @return
     */
    private Set<String> getChannelIds() {
      List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null);
      Set<String> channelIds = new HashSet<>();
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
          dlnaHelper.getChannels(udn, null),
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
  private static class EpgRequestData {
    @SerializedName("CHANNELLIST")
    List<String> channels;
    @SerializedName("TIMELIST")
    List<String> times;

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
  private static class EpgRequest {
    @SerializedName("browseEPGData")
    EpgRequestData data;

    public EpgRequestData getData() {
      return data;
    }
  }

  /**
   * Class for the EPG response JSON.
   */
  private static class EpgResponse {
    @SerializedName("EPG")
    Map<String, Map<String, Map<String, VideoProgram>>> channelMap;
    @SerializedName("FAVORITES")
    Set<String> favorites;
    @SerializedName("CHANNELS")
    Set<String> channels;
    @SerializedName("CURRENT")
    String currentChannel;

    public EpgResponse(Map<String, Map<String, Map<String, VideoProgram>>> channelMap, Set<String> favorites, Set<String> channels, String currentChannel) {
      this.channelMap = channelMap;
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

}
