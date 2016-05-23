package com.sony.sel.tvapp.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.service.DlnaService;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.Container;
import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

/**
 * Activity to perform startup checks before continuing to {@link MainActivity}
 */
public class StartupActivity extends BaseActivity {

  public static final String LOG_TAG = StartupActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.startup_activity);
    // set up to receive service intents
    registerReceiver(receiver, DlnaService.getServerIntentFilter());
  }

  @Override
  protected void onResume() {
    super.onResume();
    // attempt to start the DLNA service
    DlnaService.startService(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // stop receiving
    unregisterReceiver(receiver);
  }

  private BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      switch (intent.getAction()) {
        case DlnaService.SERVICE_STARTED:
          // service is started, proceed to main activity
          startActivity(new Intent(StartupActivity.this, MainActivity.class));
          finish();
          break;
        case DlnaService.SERVICE_STOPPED:
          // service is stopped, shouldn't receive this
          Log.e(LOG_TAG, "DLNA service stopped.");
          break;
        case DlnaService.SERVICE_ERROR:
          // display error, allow user to quit
          if (SettingsHelper.getHelper(getApplicationContext()).getEpgServer() == null) {
            // server not selected, go to server selection Activity
            startActivity(new Intent(StartupActivity.this, SelectServerActivity.class));
          } else {
            // some other error, show an alert
            new AlertDialog.Builder(StartupActivity.this)
                .setMessage(getString(R.string.startupError, intent.getSerializableExtra(DlnaService.EXTRA_ERROR)))
                .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    finish();
                  }
                }).create().show();
          }
          break;
      }
    }
  };


}
