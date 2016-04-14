package com.sony.sel.tvapp.app;

import android.app.Application;
import android.content.Intent;

import com.sony.localserver.ServerService;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;

/**
 * Virtual STB application
 */
public class TVApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // start the DLNA service when app starts
    DlnaHelper.getHelper(this).startDlnaService(null);

    // start epg web server
    String udn = SettingsHelper.getHelper(this).getEpgServer();
    Intent intent = new Intent(this, ServerService.class);
    intent.putExtra("start", true);
    if (udn != null) {
      intent.putExtra("udn", udn);
      intent.putExtra("location", ""); // this is ignored
    }
    startService(intent);
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    // tell the epg server about the new server UDN
    String udn = event.getServerUdn();
    Intent intent = new Intent(this, ServerService.class);
    intent.putExtra("serve", true);
    intent.putExtra("udn", udn);
    intent.putExtra("location", ""); // this is ignored
    startService(intent);
  }

}
