package com.sony.sel.tvapp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;

import java.util.List;

/**
 * Background service for setting up UPnP services, verifying the EPG server is available,
 * and performing EPG and VOD caching.
 */
public class DlnaService extends Service {

  public static final String TAG = DlnaService.class.getSimpleName();

  /**
   * Intent action to start the service.
   * <p/>
   * It is also valid to send this Intent action when the service is already running.
   * In this case, the server will be validated and EPG and VOD caching tasks will be re-run.
   * <p/>
   * The response to this aciton is a broadcast Intent with action {@link #SERVICE_STARTED} when successful,
   * or {@link #SERVICE_ERROR if a problem occurs.}
   */
  private static final String START = "com.sony.sel.tvapp.DlnaService.START";

  /**
   * Intent action to stop the service.
   * <p/>
   * It is also valid to send this Intent action when the service is already stopped.
   * <p/>
   * A broadcast Intent with action {@link #SERVICE_STOPPED} is sent when the service is stopped.
   */
  private static final String STOP = "com.sony.sel.tvapp.DlnaService.STOP";

  /**
   * Broadcast Intent action sent when server is successfully started.
   */
  public static final String SERVICE_STARTED = "com.sony.sel.tvapp.DlnaService.STARTED";

  /**
   * Broadcast Intent action sent when server is successfully stopped.
   */
  public static final String SERVICE_STOPPED = "com.sony.sel.tvapp.DlnaService.STOPPED";

  /**
   * Broadcast Intent action sent when a server error occurs.
   * <p/>
   * This Intent is sent with a Serializable Extra {@link #EXTRA_ERROR} that
   * will contain the Throwable representing the error tht occurred.
   */
  public static final String SERVICE_ERROR = "com.sony.sel.tvapp.DlnaService.ERROR";

  /**
   * Intent extra for SERVICE_ERROR intent.
   * <p/>
   * The contents will be a Throwable, accessed via {@link Intent#getSerializableExtra(String)}
   *
   * @<code> Throwable error = intent.getSerializableExtra(DlnaService.EXTRA_ERROR);
   * </code>
   */
  public static final String EXTRA_ERROR = "error";

  private SettingsHelper settingsHelper;
  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;

  private boolean serverAvailable;

  private final Handler handler = new Handler();
  private final Runnable epgCachingRunnable = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "Starting EPG caching task.");
      epgCachingTask = new EpgCachingTask(dlnaHelper, dlnaCache, settingsHelper.getEpgServer()) {
        @Override
        protected void onPostExecute(Void aVoid) {
          super.onPostExecute(aVoid);
          // clear task when complete
          Log.d(TAG, "EPG caching complete.");
          epgCachingTask = null;
        }
      };
      epgCachingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  };
  private EpgCachingTask epgCachingTask;
  private Runnable vodCachingRunnable = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "Starting VOD caching task.");
      vodCachingTask = new VodCachingTask(dlnaHelper, settingsHelper.getEpgServer()) {
        @Override
        protected void onPostExecute(Void aVoid) {
          super.onPostExecute(aVoid);
          // clear task when complete
          Log.d(TAG, "VOD caching complete.");
          vodCachingTask = null;
        }
      };
      vodCachingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  };
  private VodCachingTask vodCachingTask;

  /**
   * Return an Intent Filter to receive all server broadcasts.
   */
  public static IntentFilter getServerIntentFilter() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(SERVICE_STARTED);
    filter.addAction(SERVICE_STOPPED);
    filter.addAction(SERVICE_ERROR);
    return filter;
  }

  /**
   * Send an Intent to start the service.
   */
  public static void startService(Context context) {
    Intent intent = new Intent(context, DlnaService.class);
    intent.setAction(DlnaService.START);
    context.startService(intent);
  }

  /**
   * Send an Intent to stop the service.
   */
  public static void stopService(Context context) {
    Intent intent = new Intent(context, DlnaService.class);
    intent.setAction(DlnaService.STOP);
    context.startService(intent);
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "Service onCreate().");
    super.onCreate();
    settingsHelper = SettingsHelper.getHelper(this);
    dlnaHelper = DlnaHelper.getHelper(this);
    dlnaCache = DlnaHelper.getCache(this);
    EventBus.getInstance().register(this);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "Service onDestroy().");
    super.onDestroy();
    stop();
    EventBus.getInstance().unregister(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    int result =  super.onStartCommand(intent, flags, startId);
    onHandleIntent(intent);
    return result;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /**
   * Handle intents sent to the service.
   * @param intent
   */
  private void onHandleIntent(Intent intent) {
    Log.d(TAG, "Service onHandleIntent(): action = " + (intent.getAction() != null ? intent.getAction() : "null"));
    switch (intent.getAction()) {
      case START:
        start();
        break;
      case STOP:
        stopSelf();
        break;
      default:
        break;
    }
  }

  private boolean isRunning() {
    // TODO more comprehensive criteria for whether service is running?
    return dlnaHelper.isDlnaServiceStarted() && serverAvailable;
  }

  private void start() {
    Log.d(TAG, "Service start requested.");
    // begin step 1 of service startup
    startUpnpService();
  }


  private void stop() {
    Log.d(TAG, "Service stop requested.");
    if (isRunning()) {
      cancelEpgCaching();
      cancelVodCaching();
      stopDlnaService();
    } else {
      // just echo that we are stopped
      broadcastServerStopped();
    }
  }

  /**
   * Send an Intent to all BroadcastListeners that the service has started.
   */
  private void broadcastServerStarted() {
    Log.d(TAG, "Broadcasting SERVICE_STARTED.");
    Intent intent = new Intent(SERVICE_STARTED);
    sendBroadcast(intent);
  }

  /**
   * Send an Intent to all BroadcastListeners that the service encountered an error.
   */
  private void broadcastError(Throwable error) {
    Log.e(TAG, "Broadcasting SERVICE_ERROR: " + error);
    Intent intent = new Intent(SERVICE_ERROR);
    intent.putExtra(EXTRA_ERROR, error);
    sendBroadcast(intent);
  }

  /**
   * Send an Intent to all BroadcastListeners that the service has stopped.
   */
  private void broadcastServerStopped() {
    Log.d(TAG, "Broadcasting SERVICE_STOPPED.");
    Intent intent = new Intent(SERVICE_STOPPED);
    sendBroadcast(intent);
  }

  /**
   * Step 1: Start the DLNA/UPnP service for the Cling libraries.
   */
  private void startUpnpService() {
    if (!dlnaHelper.isDlnaServiceStarted()) {
      Log.d(TAG, "Starting UPnP service.");
      // start the DLNA service when app starts
      dlnaHelper.startDlnaService(new DlnaInterface.DlnaServiceObserver() {
        @Override
        public void onServiceConnected() {
          Log.d(TAG, "UPnP Service Connected.");
          verifyServer();
        }

        @Override
        public void onServiceDisconnected() {
          Log.d(TAG, "UPnP Service Disconnected.");
          broadcastServerStopped();
        }

        @Override
        public void onError(Exception error) {
          Log.d(TAG, "UPnP Service error: " + error);
          broadcastError(error);
        }
      });
    } else {
      // proceed to next step
      Log.d(TAG, "UPnP service already started.");
      verifyServer();
    }
  }

  /**
   * Stop the DLNA service. Service will broadcast {@link #SERVICE_STOPPED} when DLNA service is unbound.
   */
  private void stopDlnaService() {
    if (dlnaHelper.isDlnaServiceStarted()) {
      dlnaHelper.stopDlnaService();
    }
  }

  /**
   * Step 2: Verify that the DLNA server is available.
   */
  private void verifyServer() {
    // initially set unavailable
    serverAvailable = false;
    if (settingsHelper.getEpgServer() == null) {
      Log.e(TAG, "Server has not been selected.");
      broadcastError(new Exception("Server has not been selected."));
    } else {
      // check the server
      Log.d(TAG, "Verifying server.");
      new VerifyServerTask(dlnaHelper, settingsHelper.getEpgServer()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  /**
   * Receive notification that server check completed.
   *
   * @param available true if the server was found, otherwise false.
   */
  private void onServerVerified(boolean available) {
    serverAvailable = available;
    if (serverAvailable) {
      Log.d(TAG, "DLNA server was verified.");
      // send broadcast we are "started"
      broadcastServerStarted();
      // next step
      scheduleCachingTasks();
    } else {
      Log.e(TAG, "DLNA server could not be verified.");
      broadcastError(new Exception("DLNA server could not be verified."));
    }
  }

  /**
   * Step 3: Schedule the DLNA caching tasks, if they are not already running.
   */
  void scheduleCachingTasks() {
    if (epgCachingTask == null && vodCachingTask == null) {
      Log.d(TAG, "Scheduling caching tasks.");
      // start epg caching after 10 seconds
      startEpgCaching(10000);
      // start vod caching 1 minute after epg
      startVodCaching(70000);
    } else {
      Log.d(TAG, "Caching tasks are already running.");
    }
  }

  /**
   * Receive an event that the EPG server has changed.
   * @param event
   */
  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    Log.d(TAG, "Received server changed event. UDN = " + event.getServerUdn() + ".");
    // cancel any existing caching tasks
    cancelVodCaching();
    cancelEpgCaching();
    // re-verify server and restart caching against new server
    verifyServer();
  }

  private void startVodCaching(long delay) {
    // cancel any caching in progress
    cancelVodCaching();
    // start new caching task
    handler.postDelayed(vodCachingRunnable, delay);
  }

  private void cancelVodCaching() {
    handler.removeCallbacks(vodCachingRunnable);
    if (vodCachingTask != null) {
      vodCachingTask.cancel(true);
      vodCachingTask = null;
    }
  }

  private void startEpgCaching(long delay) {
    // cancel any caching in progress
    cancelEpgCaching();
    // start new caching task
    handler.postDelayed(epgCachingRunnable, delay);
  }

  private void cancelEpgCaching() {
    handler.removeCallbacks(epgCachingRunnable);
    if (epgCachingTask != null) {
      epgCachingTask.cancel(true);
      epgCachingTask = null;
    }
  }

  private class VerifyServerTask extends AsyncTask<Void, Void, Boolean> {

    private String udn;
    private DlnaInterface dlnaHelper;

    public VerifyServerTask(DlnaInterface dlnaHelper, String udn) {
      this.udn = udn;
      this.dlnaHelper = dlnaHelper;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
      do {

        // browse server root
        Log.d(TAG, "Checking root of DLNA server " + udn + ".");
        List<DlnaObject> root = dlnaHelper.getChildren(udn, DlnaHelper.DLNA_ROOT, DlnaObject.class, null, false);
        Log.d(TAG, "DLNA server root = " + new Gson().toJson(root));
        if (root != null && root.size() > 0) {
          return true;
        }

        // sleep and try again
        try {
          Log.d(TAG, "Waiting for retry...");
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
        Log.d(TAG, "Server " + udn + " was verified.");
      } else {
        Log.e(TAG, "Server " + udn + " could not be verified.");
      }
      onServerVerified(success);
    }
  }

}
