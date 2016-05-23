package com.sony.sel.tvapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver to start the DlnaService when the device starts up.
 */
public class BootReceiver extends BroadcastReceiver {

  public static final String TAG = BootReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "Received Intent. Action = " + (intent.getAction() != null ? intent.getAction() : "null"));

    switch (intent.getAction()) {
      case DlnaService.SERVICE_STARTED:
        Log.d(TAG, "DLNA service started.");
        break;
      case DlnaService.SERVICE_STOPPED:
        Log.d(TAG, "DLNA service stopped.");
        break;
      case DlnaService.SERVICE_ERROR:
        Log.d(TAG, "DLNA service error: " + intent.getSerializableExtra(DlnaService.EXTRA_ERROR));
        break;
      case Intent.ACTION_BOOT_COMPLETED:
        Log.d(TAG, "ACTION_BOOT_COMPLETED received.");
        startServer(context);
        break;
    }
  }

  private void startServer(Context context) {
    Log.d(TAG, "Starting web service.");
    context.registerReceiver(this, DlnaService.getServerIntentFilter());
    DlnaService.startService(context);
  }
}
