package com.sony.sel.tvapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver to receive broadcast intents from Android and start/restart the {@link DlnaService}.
 */
public class BootReceiver extends BroadcastReceiver {

  public static final String TAG = BootReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "Received Intent. Action = " + (intent.getAction() != null ? intent.getAction() : "null"));

    switch (intent.getAction()) {
      case Intent.ACTION_BOOT_COMPLETED:
        Log.d(TAG, "ACTION_BOOT_COMPLETED received.");
        startServer(context);
        break;
      case Intent.ACTION_DATE_CHANGED:
        Log.d(TAG, "ACTION_DATE_CHANGED received.");
        startServer(context);
        break;
      case Intent.ACTION_TIME_CHANGED:
        Log.d(TAG, "ACTION_TIME_CHANGED received.");
        startServer(context);
        break;
      case Intent.ACTION_TIMEZONE_CHANGED:
        Log.d(TAG, "ACTION_TIMEZONE_CHANGED received.");
        startServer(context);
        break;
    }
  }

  private void startServer(Context context) {
    Log.d(TAG, "Starting DLNA background service.");
    DlnaService.startService(context);
  }
}
