package com.sony.sel.tvapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {

  private static final String LOG_TAG = SettingsHelper.class.getSimpleName();

  private static final String PREFS_FILE = "prefs";

  private static final String EPG_SERVER = "EpgServer";

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
}