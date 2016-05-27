package com.sony.sel.tvinput;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.SelectServerActivity;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvinput.syncadapter.SyncUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity called by the system for setting up our Streaming Channels.
 */
public class SetupActivity extends Activity {

  public static final String TAG = SetupActivity.class.getSimpleName();

  @Bind(R.id.textViewSetup)
  TextView messageText;
  @Bind(R.id.scanChannelsButton)
  View scanButton;
  @Bind(R.id.selectEpgServerButton)
  View epgServerButton;
  @Bind(R.id.syncEpgButton)
  View syncEpg;

  private DlnaInterface dlnaHelper;
  private SettingsHelper settingsHelper;
  private SyncUtils syncUtils;
  private String inputId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_setup);
    ButterKnife.bind(this);

    dlnaHelper = DlnaHelper.getHelper(this);
    settingsHelper = SettingsHelper.getHelper(this);
    syncUtils = SyncUtils.getInstance();

    inputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
    Log.d(TAG, "Input ID is " + inputId + ".");

    epgServerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // launch EPG server selection activity
        Log.d(TAG, "Select EPG server.");
        startActivity(new Intent(SetupActivity.this, SelectServerActivity.class));
      }
    });

    scanButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // scan for channels
        Log.d(TAG, "Scan for channels.");
        messageText.setText(R.string.scanning);
        scanButton.setEnabled(false);
        new GetChannelsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });

    syncEpg.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // sync EPG now
        Log.d(TAG, "Sync EPG.");
        syncUtils.requestSync(inputId, false);
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (settingsHelper.getEpgServer() == null) {
      messageText.setText(R.string.noEpgServerSet);
      scanButton.setEnabled(false);
      epgServerButton.requestFocus();
    } else {
      messageText.setText(R.string.setupChannelsPrompt);
      scanButton.setEnabled(true);
      scanButton.requestFocus();
    }
  }

  private class GetChannelsTask extends AsyncTask<Void, Void, List<VideoBroadcast>> {

    @Override
    protected List<VideoBroadcast> doInBackground(Void... params) {
      List<VideoBroadcast> channels = dlnaHelper.getChannels(settingsHelper.getEpgServer(), null, true);
      if (channels.size() > 0) {
        Log.d(TAG, "Found " + channels.size() + " channels.");

        TvInputUtil tvInputUtil = new TvInputUtil(inputId, SetupActivity.this);

        // register the channels
        tvInputUtil.registerChannels(channels);

        // Set up EPG sync to happen once per minute
        // TODO adjust frequency for a "real" release.
        syncUtils.setUpPeriodicSync(getApplicationContext(), inputId, 60);

      } else {
        Log.e(TAG, "No channels found");
      }
      return channels;
    }

    @Override
    protected void onPostExecute(List<VideoBroadcast> channels) {
      if (channels.size() > 0) {
        messageText.setText(getString(R.string.channelsFound, channels.size()));
      } else {
        messageText.setText(R.string.noChannelsFound);
      }
      scanButton.setEnabled(true);
    }
  }

}
