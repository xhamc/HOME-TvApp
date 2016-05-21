package com.sony.sel.tvapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.SettingsHelper;

/**
 * Activity to receive intents for System Searched VOD results.
 * <p/>
 * Decodes the VOD item requested and launches {@link MainActivity}
 * using {@link MainActivity#INTENT_ACTION_PLAY_VOD} with the
 * epg VideoItem in the intent extras.
 */
public class VodSearchActivity extends BaseActivity {

  public static final String TAG = VodSearchActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SettingsHelper settingsHelper = SettingsHelper.getHelper(this);
    Uri uri = getIntent().getData();
    Log.d(TAG, "Search intent received. Data = " + uri + ".");
    // get the object id
    String id = uri.getPath().substring(1);
    VideoItem vod = DlnaHelper.getCache(this).getItemById(settingsHelper.getEpgServer(), id);
    Intent intent = new Intent(this, MainActivity.class);
    intent.setAction(MainActivity.INTENT_ACTION_PLAY_VOD);
    intent.putExtra(MainActivity.INTENT_EXTRA_VIDEO_ITEM, vod.toString());
    startActivity(intent);
    finish();
  }

}
