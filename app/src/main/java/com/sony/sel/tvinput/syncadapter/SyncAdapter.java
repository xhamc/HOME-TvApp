package com.sony.sel.tvinput.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.sony.sel.tvinput.TvInputUtil;

/**
 * Created by breeze on 4/13/16.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {

  public static final String TAG = SyncAdapter.class.getSimpleName();

  /**
   * Set up the sync adapter
   */
  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
    Log.d(TAG, "Sync adapter created.");
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Log.d(TAG, "Sync adapter onPerformSync()");
    TvInputUtil tvInputUtil = new TvInputUtil(getContext());
    tvInputUtil.addProgramData();
  }

}
