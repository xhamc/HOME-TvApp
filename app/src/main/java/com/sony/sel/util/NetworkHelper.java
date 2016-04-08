package com.sony.sel.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Helper for TV Care app connectivity. Maintains a "heartbeat" async task that
 * checks the server heartbeat URL at specified intervals.
 * <p/>
 * Network state is determined from a combination of ConnectivityManager's network state
 * and the status of the heartbeat URL connection/accessibility
 * <p/>
 * {@link NetworkHelper.NetworkObserver} is used to receive
 * network state change notifications from the helper.
 * <p/>
 * Clients who want to retry the heartbeat one or more times before showing failure UI to the
 * user can call {@link #retryNetworkConnection(String, RetryObserver, int)} upon connectivity loss.
 */
public class NetworkHelper extends BroadcastReceiver {

  public static final String TAG = NetworkHelper.class.getSimpleName();

  // sleep intervals for heartbeat check when connected/disconnected
  private static final long HEARTBEAT_SLEEP_CONNECTED = 60000;
  private static final long HEARTBEAT_SLEEP_DISCONNECTED = 5000;

  private static NetworkHelper INSTANCE;

  private ConnectivityManager connectivityManager;
  private ObserverSet<NetworkObserver> observers = new ObserverSet<>(NetworkObserver.class);
  private boolean firstNetworkStateReceived;

  private CheckHeartbeatUrlTask heartbeatTask;
  private Boolean heartbeat;

  public interface NetworkObserver {
    void onNetworkEnabled(boolean enabled);
  }

  public interface RetryObserver {
    void onNetworkRetry(boolean success);
  }

  /**
   * Get helper instance.
   */
  public static NetworkHelper getHelper(Context context) {
    if (INSTANCE == null) {
      // ensure application context is used to prevent leaks
      INSTANCE = new NetworkHelper(context.getApplicationContext());
    }
    return INSTANCE;
  }

  private NetworkHelper(Context context) {
    Log.d(TAG, "Creating network helper.");
    connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    Log.d(TAG, "Registering broadcast receiver.");
    IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    context.registerReceiver(this, filter);
  }

  public void pauseHeartbeatTask() {
    if (heartbeatTask != null) {
      heartbeatTask.pause();
    }
  }

  public void resumeHeartbeatTask() {
    if (heartbeatTask != null) {
      heartbeatTask.resume();
    }
  }

  public boolean isHeartbeatPaused() {
    if (heartbeatTask != null) {
      return heartbeatTask.isPaused();
    }
    return false;
  }

  /**
   * Test for network connectivity.
   * Returns true if active network is connected and the heartbeat URL is reachable.
   */
  public boolean isNetworkEnabled() {
    return (isActiveNetworkConnected() && (heartbeat == null || heartbeat == true));
  }

  /**
   * Return 0 or more IP addresses for the current device on all active network interfaces.
   * Filters out any loopback addresses.
   * If the device is connected normally, the size of the list will usually be 1.
   * If there are no active networks, will return an empty, but non-null list.
   */
  public List<InetAddress> getLocalIpAddresses() {
    List<InetAddress> addresses = new ArrayList<>();
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface
          .getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf
            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            addresses.add(inetAddress);
          }
        }
      }
    } catch (SocketException e) {
      Log.e(TAG, "Error enumerating network interfaces:" + e);
    }
    return addresses;
  }

  /**
   * Register observers to receive notifications when the network state changes.
   */
  public ObserverSet<NetworkObserver> getObservers() {
    return observers;
  }

  /**
   * Retry the network connection.
   *
   * @param observer   Observer to receive result of retries.
   * @param numRetries Number of times to retry before returning a failure.
   */
  public void retryNetworkConnection(@NonNull String heartbeatUrl, @NonNull RetryObserver observer, int numRetries) {
    new NetworkRetryTask(observer, heartbeatUrl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, numRetries);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "Network connectivity change");
    if (intent.getExtras() != null) {
      NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
      if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
        Log.i(TAG, "Network " + ni.getTypeName() + " connected");

        // suppress first notification
        if (firstNetworkStateReceived) {
          observers.proxy().onNetworkEnabled(true);
        } else {
          firstNetworkStateReceived = true;
        }

      } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
        Log.d(TAG, "There's no network connectivity");

        // suppress first notification
        if (firstNetworkStateReceived) {
          observers.proxy().onNetworkEnabled(false);
        } else {
          firstNetworkStateReceived = true;
        }
      }
    }
  }

  /**
   * Returns true if active network is connected. Ignores heartbeat result.
   */
  private boolean isActiveNetworkConnected() {
    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
    return (info != null && info.isConnected());
  }

  private void startHeartbeatTask(String heartbeatUrl) {
    if (heartbeatTask == null) {
      Log.v(TAG, "Starting heartbeat task.");
      heartbeat = null;
      heartbeatTask = new CheckHeartbeatUrlTask(heartbeatUrl);
      heartbeatTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
  }

  private void stopHeartbeatTask() {
    if (heartbeatTask != null) {
      Log.v(TAG, "Stopping heartbeat task.");
      heartbeat = null;
      heartbeatTask.cancel(true);
      heartbeatTask = null;
    }
  }

  /**
   * Check the heartbeat URL (synchronously)
   *
   * @return The HTTP status code if connected
   * @throws IOException if there's a connection error
   */
  private int checkHeartbeatUrl(String url) throws IOException {
    // test the heartbeat URL
    Log.v(TAG, "Checking heartbeat URL " + url);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod("GET");
    connection.setReadTimeout(5000);
    connection.setConnectTimeout(5000);
    connection.connect();
    return connection.getResponseCode();
  }

  /**
   * Receive the heartbeat state and manage notifications to observers.
   *
   * @param heartbeat Current state of heartbeat URL connection.
   */
  private void onHeartbeat(boolean heartbeat) {
    if (this.heartbeat == null) {
      // first heartbeat update
      this.heartbeat = heartbeat;
      Log.v(TAG, "First heartbeat status is " + heartbeat + ".");
      if (heartbeat != isActiveNetworkConnected()) {
        // mismatch between active network state & heartbeat, so send update
        observers.proxy().onNetworkEnabled(heartbeat);
      }
    } else if (this.heartbeat.equals(heartbeat) == false) {
      // heartbeat status changed
      this.heartbeat = heartbeat;
      if (heartbeat) {
        // status changed from false to true
        Log.w(TAG, "Heartbeat status changed to " + heartbeat + ".");
        observers.proxy().onNetworkEnabled(true);
      } else {
        // status changed from true to false
        Log.w(TAG, "Heartbeat status changed to " + heartbeat + ".");
        observers.proxy().onNetworkEnabled(false);
      }
    }
  }

  private class CheckHeartbeatUrlTask extends AsyncTask<Void, Boolean, Void> {

    private boolean paused;
    private final Object pauseLock = new Object();
    private String heartbeatUrl;

    public CheckHeartbeatUrlTask(String heartbeatUrl) {
      this.heartbeatUrl = heartbeatUrl;
    }

    @Override
    protected Void doInBackground(Void... params) {
      Log.v(TAG, "Heartbeat task starting.");
      // loop until cancelled or interrupted
      while (!isCancelled()) {
        boolean success = false;
        boolean isPaused;
        synchronized (pauseLock) {
          // copy here, keep the sync block small
          isPaused = paused;
        }
        if (!isPaused) {
          try {
            int responseCode = checkHeartbeatUrl(heartbeatUrl);
            if (responseCode == HttpURLConnection.HTTP_OK) {
              // success
              success = true;
              publishProgress(success);
            } else {
              // server returned error, consider that a loss of connection
              Log.e(TAG, "Heartbeat URL returned HTTP response " + responseCode + ".");
              success = false;
              publishProgress(success);
            }
          } catch (IOException e) {
            // exception occurred, probably because the internet connection is broken
            Log.e(TAG, "Exception checking heartbeat URL: " + e);
            success = false;
            publishProgress(success);
          }
        } else {
          Log.d(TAG, "Heartbeat task paused, skipping check.");
        }
        try {
          // wait and try again
          Thread.sleep(success ? HEARTBEAT_SLEEP_CONNECTED : HEARTBEAT_SLEEP_DISCONNECTED);
        } catch (InterruptedException e) {
          // interrupted, return null
          return null;
        }
      }
      // cancelled, return null
      return null;
    }

    public boolean isPaused() {
      return paused;
    }

    public void pause() {
      synchronized (pauseLock) {
        if (!paused) {
          Log.d(TAG, "Pausing heartbeat task.");
          paused = true;
        }
      }
    }

    public void resume() {
      synchronized (pauseLock) {
        if (paused) {
          Log.d(TAG, "Resuming heartbeat task.");
          paused = false;
        }
      }
    }

    @Override
    protected void onCancelled(Void aVoid) {
      super.onCancelled(aVoid);
      super.onCancelled();
      Log.w(TAG, "Heartbeat task cancelled.");
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
      Log.w(TAG, "Heartbeat task cancelled.");
    }

    @Override
    protected void onProgressUpdate(Boolean... values) {
      super.onProgressUpdate(values);
      Log.v(TAG, "Heartbeat test returned " + values[0] + ".");
      if (!isCancelled() && heartbeatTask != null) {
        // only send if not cancelled
        onHeartbeat(values[0]);
      }
    }
  }

  /**
   * Class for retrying the network connection before displaying no-network.
   */
  private class NetworkRetryTask extends AsyncTask<Integer, Void, Boolean> {

    private final RetryObserver observer;
    private final String heartbeatUrl;

    public NetworkRetryTask(RetryObserver observer, String heartbeatUrl) {
      this.observer = observer;
      this.heartbeatUrl = heartbeatUrl;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      // pause heartbeat while retrying
      pauseHeartbeatTask();
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
      int numRetries = params.length > 0 ? params[0] : 1;
      Log.d(TAG, "Retrying heartbeat URL " + numRetries + " times.");
      for (int i = 0; i < numRetries; i++) {
        Log.d(TAG, "Heartbeat URL retry attempt number " + (i + 1) + '.');
        try {
          int responseCode = checkHeartbeatUrl(heartbeatUrl);
          if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Success retrying heartbeat url.");
            return true;
          }
        } catch (IOException e) {
          Log.e(TAG, "Error retrying heartbeat url: " + e);
        }
      }
      // failed after retrying
      return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
      super.onPostExecute(success);
      observer.onNetworkRetry(success);
      // resume heartbeat on completion
      resumeHeartbeatTask();
    }
  }

}
