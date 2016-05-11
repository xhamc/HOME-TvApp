package com.sony.localserver;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

/**
 * Service to host EPG grid browsing via HTML.
 */
public class ServerService extends IntentService {

  public static final String TAG = "CVP-2";

  /**
   * Incoming Intent actions
   */
  public static final String START = "com.sony.localserver.ServerService.START";
  public static final String STOP = "com.sony.localserver.ServerService.STOP";

  /**
   * Intent extra for UDN
   */
  public static final String EXTRA_UDN = "udn";

  /**
   * Broadcast Intent actions
   */
  public static final String SERVICE_STARTED = "com.sony.localserver.ServerService.STARTED";
  public static final String SERVICE_STOPPED = "com.sony.localserver.ServerService.STOPPED";
  public static final String UDN_UPDATED = "com.sony.localserver.ServerService.UDN_UPDATED";

  private static WebSocketServer webSocketServer;
  public Context applicationContext;

  @Override
  public void onHandleIntent(Intent intent) {
    Log.d(TAG, "Service Intent: " + intent.getAction());
    applicationContext=getApplicationContext();
    if (intent.getAction().equals(START)) {
      start();

    } else if (intent.getAction().equals(STOP)) {
      stop();
    }
    if (intent.hasExtra(EXTRA_UDN)) {
      String udn = intent.getStringExtra(EXTRA_UDN);
      webSocketServer.setUdn(udn);
      sendBroadcast(new Intent(UDN_UPDATED));
    }
  }

  public ServerService() {
    super("ServerService");
  }

  private void start() {
    if (webSocketServer == null) {
      Log.d(TAG, "ServerService::start");
      int port = 9000;
      String host = "127.0.0.1";
      if (webSocketServer == null) {
        webSocketServer = new WebServer(host, port, getApplicationContext());
        try {
          webSocketServer.start(0);
          sendBroadcast(new Intent(SERVICE_STARTED));
        } catch (IOException e) {
          Log.e(TAG, "Error starting server: " + e);
        }
      }
    } else {
      Log.w(TAG, "Server already started.");
      sendBroadcast(new Intent(SERVICE_STARTED));
    }
  }

  private void stop() {
    if (webSocketServer != null) {
      Log.d(TAG, "ServerService::stop");
      webSocketServer.stop();
      webSocketServer = null;
      sendBroadcast(new Intent(SERVICE_STOPPED));
    } else {
      Log.w(TAG, "Stop issued but server not started.");
      sendBroadcast(new Intent(SERVICE_STOPPED));
    }
  }
}
