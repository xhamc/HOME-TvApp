package com.sony.sel.tvapp.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.RecordEpgFragment;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.Date;

/**
 * Activity to receive intents for System Searched EPG results.
 * <p/>
 * Decodes the EPG item requested, changes to the channel if the program is currently
 * playing, or displays a recording UI if the program is not currently playing.
 */
public class RecordEpgActivity extends BaseActivity {

  public static final String TAG = RecordEpgActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.record_epg_activity);

    final SettingsHelper settingsHelper = SettingsHelper.getHelper(this);
    final DlnaInterface dlnaHelper = DlnaHelper.getHelper(this);

    Uri uri = getIntent().getData();
    Log.d(TAG, "Search intent received. Data = " + uri + ".");
    // get the object id
    String id = uri.getPath().substring(1);

    // get the program
    final VideoProgram program = DlnaHelper.getCache(this).getItemById(settingsHelper.getEpgServer(), id);

    Date now = new Date();
    if (program.getScheduledStartTime().before(now) && program.getScheduledEndTime().after(now)) {
      // program is playing now, switch to channel
      new AsyncTask<Void, Void, DlnaObjects.VideoBroadcast>() {

        @Override
        protected DlnaObjects.VideoBroadcast doInBackground(Void... params) {
          return dlnaHelper.getChannel(settingsHelper.getEpgServer(), program.getChannelId());
        }

        @Override
        protected void onPostExecute(DlnaObjects.VideoBroadcast channel) {
          super.onPostExecute(channel);
          // launch MainActivity with channel intent
          Intent intent = new Intent(RecordEpgActivity.this, MainActivity.class);
          intent.setAction(MainActivity.INTENT_ACTION_VIEW_CHANNEL);
          intent.putExtra(MainActivity.INTENT_EXTRA_CHANNEL, channel.toString());
          startActivity(intent);
          finish();
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    } else {
      // program is not currently playing, allow user to record the program
      new AsyncTask<Void, Void, DlnaObjects.VideoBroadcast>() {

        @Override
        protected DlnaObjects.VideoBroadcast doInBackground(Void... params) {
          return dlnaHelper.getChannel(settingsHelper.getEpgServer(), program.getChannelId());
        }

        @Override
        protected void onPostExecute(DlnaObjects.VideoBroadcast channel) {
          super.onPostExecute(channel);
          // show the recording UI
          RecordEpgFragment fragment = RecordEpgFragment.newInstance(program, channel);
          FragmentTransaction transaction = getFragmentManager().beginTransaction();
          transaction.add(R.id.contentFrame, fragment, RecordEpgFragment.TAG);
          transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
          transaction.commit();
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

  }

}
