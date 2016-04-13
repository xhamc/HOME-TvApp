package com.sony.sel.tvapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class SettingsHelper {

  private static final String LOG_TAG = SettingsHelper.class.getSimpleName();

  private static final String PREFS_FILE = "prefs";

  private static final String EPG_SERVER = "EpgServer";
  private static final String CURRENT_CHANNEL = "CurrentChannel";

  private static SettingsHelper INSTANCE;

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
}