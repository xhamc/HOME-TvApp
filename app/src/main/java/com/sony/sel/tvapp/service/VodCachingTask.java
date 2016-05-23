package com.sony.sel.tvapp.service;

import android.os.AsyncTask;
import android.util.Log;

import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;

import java.util.List;

/**
 * Task that browses the VOD directories to allow the DlnaHelper to cache them.
 */
public class VodCachingTask extends AsyncTask<Void, Void, Void> {

  private final String TAG = VodCachingTask.class.getSimpleName();

  private final DlnaInterface dlnaHelper;
  private final String udn;

  public VodCachingTask(DlnaInterface dlnaHelper, String udn) {
    this.dlnaHelper = dlnaHelper;
    this.udn = udn;
  }

  @Override
  protected Void doInBackground(Void... params) {
    Log.d(TAG, "Caching VOD containers.");
    List<DlnaObjects.DlnaObject> vodContainers = dlnaHelper.getChildren(udn, "0/VOD", DlnaObjects.DlnaObject.class, null, true);
    for (DlnaObjects.DlnaObject container : vodContainers) {
      Log.d(TAG, "Caching " + container.getId() + ".");
      dlnaHelper.getChildren(udn, container.getId(), DlnaObjects.DlnaObject.class, null, true);
      if (isCancelled()) {
        // bail out, task was cancelled
        return null;
      }
    }
    return null;
  }

  @Override
  protected void onCancelled(Void aVoid) {
    super.onCancelled(aVoid);
    Log.v(TAG, "Caching VOD containers was cancelled");
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
    Log.d(TAG, "Finished caching VOD containers.");
  }
}