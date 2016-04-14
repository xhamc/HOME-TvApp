package com.sony.sel.tvapp.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.Container;
import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

/**
 * Activity to perform startup checks before continuing
 */
public class StartupActivity extends BaseActivity {

  public static final String LOG_TAG = StartupActivity.class.getSimpleName();

  private SettingsHelper settingsHelper;
  private DlnaHelper dlnaHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.startup_activity);

    settingsHelper = SettingsHelper.getHelper(this);
    dlnaHelper = DlnaHelper.getHelper(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (settingsHelper.getEpgServer() == null) {
      // select the server first
      startActivity(new Intent(this, SelectServerActivity.class));
    } else {
      new CheckServerTask(dlnaHelper, settingsHelper.getEpgServer()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

  }

  private class CheckServerTask extends AsyncTask<Void, Void, Boolean> {

    private String udn;
    private DlnaHelper dlnaHelper;

    public CheckServerTask(DlnaHelper dlnaHelper, String udn) {
      this.udn = udn;
      this.dlnaHelper = dlnaHelper;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
      do {

        // browse server root
        Log.d(LOG_TAG, "Checking root of server " + udn + ".");
        List<Container> root = dlnaHelper.getChildren(udn, DlnaHelper.DLNA_ROOT, Container.class);
        Log.d(LOG_TAG, "Root = " + root != null ? new Gson().toJson(root) : "null");
        if (root != null) {
          return true;
        }

        // sleep and try again
        try {
          Log.d(LOG_TAG, "Waiting for retry...");
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          return false;
        }

      } while (isCancelled() == false);

      return true;
    }


    @Override
    protected void onPostExecute(Boolean success) {
      super.onPostExecute(success);
      if (success) {
        // continue to main activity
        startActivity(new Intent(StartupActivity.this, MainActivity.class));
        finish();
      } else {
        Log.e(LOG_TAG, "Server " + udn + " could not be validated.");
      }
    }
  }

  private static class IterateServerTask extends AsyncTask<Void, Void, Void> {

    public static final String LOG_TAG = "DlnaTest";

    private DlnaHelper helper;
    private final String udn;

    public IterateServerTask(DlnaHelper helper, String udn) {
      this.helper = helper;
      this.udn = udn;
    }

    @Override
    protected Void doInBackground(Void... params) {
      // get channel list
      int count = iterateChildren("0");
      Log.d(LOG_TAG, "Iteration complete, count = "+count+" objects.");
      return null;
    }

    int iterateChildren(String parentId) {
      List<DlnaObject> children = helper.getChildren(udn,parentId, DlnaObject.class);
      int count = children.size();
      for (DlnaObject child : children) {
        if (child.getUpnpClass().startsWith(Container.CLASS)) {
          // drill down
          count += iterateChildren(child.getId());
        }
      }
      return count;
    }
  }


}
