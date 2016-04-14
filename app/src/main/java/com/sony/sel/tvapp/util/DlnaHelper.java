package com.sony.sel.tvapp.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.sony.huey.dlna.DlnaCdsStore;
import com.sony.huey.dlna.IUpnpServiceCp;
import com.sony.huey.dlna.UpnpServiceCp;
import com.sony.sel.util.NetworkHelper;
import com.sony.sel.util.ObserverSet;
import com.sony.sel.util.SsdpServiceHelper;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import static com.sony.sel.tvapp.util.DlnaObjects.EpgContainer;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Helper for DLNA service and content provider.
 */
public class DlnaHelper {

  public static final String TAG = DlnaHelper.class.getSimpleName();

  // object ID for dlna root directory
  public static final String DLNA_ROOT = "0";

  // name of DLNA service
  private static final String HUEY_PACKAGE = "com.sony.huey.dlna.module";

  private Context context;
  private ContentResolver contentResolver;
  private NetworkHelper networkHelper;

  private Object lock = new Object();

  private IUpnpServiceCp hueyService;
  private ServiceConnection hueyConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(TAG, "DLNA Service connected.");
      hueyService = IUpnpServiceCp.Stub.asInterface(service);
      serviceObservers.proxy().onServiceConnected();

      // refresh device list once connected
      // this enables the service to function as content provider
      try {
        hueyService.refreshDeviceList(2, null);
      } catch (RemoteException e) {
        Log.e(TAG, "Error refreshing device list: " + e);
        serviceObservers.proxy().onError(e);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(TAG, "DLNA Service disconnected.");
      hueyService = null;
      serviceObservers.proxy().onServiceDisconnected();
    }
  };

  private ObserverSet<DlnaServiceObserver> serviceObservers = new ObserverSet<DlnaServiceObserver>(DlnaServiceObserver.class);

  private static DlnaHelper INSTANCE;

  /**
   * Get the helper instance.
   */
  public static DlnaHelper getHelper(Context context) {
    if (INSTANCE == null) {
      // ensure application context is used to prevent leaks
      INSTANCE = new DlnaHelper(context.getApplicationContext());
    }
    return INSTANCE;
  }

  private DlnaHelper(Context context) {
    this.context = context.getApplicationContext();
    this.contentResolver = context.getContentResolver();
    networkHelper = NetworkHelper.getHelper(this.context);
  }

  /**
   * AsyncTaskObserver for the state of the DLNA service.
   */
  public interface DlnaServiceObserver {

    /**
     * Service connected successfully.
     */
    void onServiceConnected();

    /**
     * Service disconnected.
     */
    void onServiceDisconnected();

    /**
     * Error occurred managing the server state.
     *
     * @param error The error that occurred.
     */
    void onError(Exception error);
  }

  /**
   * Start the DLNA service. Until the service is started, data routines
   * will return empty results.
   *
   * @param observer AsyncTaskObserver to receive state notifications about the DLNA service.
   */
  public void startDlnaService(DlnaServiceObserver observer) {

    if (observer != null) {
      // add observer
      serviceObservers.add(observer);
    }

    if (hueyService != null) {
      // already started
      if (observer != null) {
        // notify the new observer immediately
        observer.onServiceConnected();
      }
      return;
    }

    try {

      // start background service
      context.startService(getDlnaServiceIntent());

      // bind the service
      Intent hueyIntent = new Intent(getDlnaServiceContext(), UpnpServiceCp.class);
      context.bindService(hueyIntent, hueyConnection, Context.BIND_AUTO_CREATE);

    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Error starting DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    } catch (IOException e) {
      Log.e(TAG, "Error starting DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    }

    Cursor c = context.getContentResolver().query(UpnpServiceCp.CONTENT_URI,
        UpnpServiceCp.PROJECTION,
        true ?
            UpnpServiceCp.DEVICE_TYPE + " LIKE '%'" :
            UpnpServiceCp.DEVICE_TYPE + " LIKE '%:" + UpnpServiceCp.REMOTE_UI_SERVER_DEVICE + ":%'",
        null, null);

    Bundle bundle = new Bundle();
    bundle.putInt(UpnpServiceCp.RESPOND_IOCTL_GET_CONTROL_POINT_STATUS, 0);
    Bundle ret = c.respond(bundle);
    if ((ret.getInt(UpnpServiceCp.RESPOND_IOCTL_GET_CONTROL_POINT_STATUS) & UpnpServiceCp.GENERIC_DEVICE_CP) != 0) {
      Log.d(TAG, "Control point started.");
    } else {
      Log.d(TAG, "Control point starting.");
      Bundle bundle1 = new Bundle();
      bundle1.putInt(UpnpServiceCp.RESPOND_IOCTL_CONTROL_POINT, UpnpServiceCp.GENERIC_DEVICE_CP | UpnpServiceCp.IOCTL_START_CONTROL_POINT);
      ret = c.respond(bundle1);
      Log.d(TAG, "Control point starting. Response = " + ret + ".");
    }
    c.close();

  }

  /**
   * Returns true if the DNLA service is currently started & connected.
   */
  public boolean isDlnaServiceStarted() {
    return hueyService != null;
  }

  /**
   * Stop and disconnect the DLNA service.
   */
  public void stopDlnaService() {
    if (hueyService == null) {
      // already stopped
      return;
    }
    try {
      Context serviceContext = getDlnaServiceContext();
      Intent intent = new Intent(serviceContext, UpnpServiceCp.class);
      context.stopService(intent);
      return;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Error stopping DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    }
  }

  /**
   * Retrieve the DLNA child objects for a given parent.
   *
   * @param udn        UDN of the DLNA server to query.
   * @param parentId   Parent object ID (use {@link #DLNA_ROOT} for the top level)
   * @param childClass Class of child objects expected. Used to define query columns.
   * @param <T>        Class of child object.
   * @return List of children, or an empty list if no children were found.
   */
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId, Class<T> childClass) {


    Cursor cursor = null;
    List<T> children = new ArrayList<>();

    synchronized (lock) {
      try {
        Log.d(TAG, "Get DLNA child objects. UDN = " + udn + ", ID = " + parentId + ".");
        Uri uri = DlnaCdsStore.getObjectUri(udn, parentId);
        Log.d(TAG, "URI = " + uri);
        String[] columns = DlnaObject.getColumnNames(childClass);
        Log.d(TAG, "Querying. UDN = " + udn + ", ID = " + parentId + ".");
        cursor = contentResolver.query(uri, columns, null, null, null);
        Log.d(TAG, "Response received. UDN = " + udn + ", ID = " + parentId + ".");
        if (cursor == null) {
          // no data
          Log.w(TAG, "No data.");
          return children;
        } else {
          Log.d(TAG, String.format(Locale.getDefault(), "%d child items found.", cursor.getCount()));
        }
        if (cursor.moveToFirst()) {
          do {
            String upnpClass = cursor.getString(cursor.getColumnIndex(DlnaCdsStore.CLASS));
            DlnaObject dlnaObject;
            if (upnpClass.equals(VideoBroadcast.CLASS)) {
              // channel
              dlnaObject = new VideoBroadcast(cursor);
            } else if (upnpClass.equals(EpgContainer.CLASS)) {
              // container
              dlnaObject = new EpgContainer(cursor);
            } else if (upnpClass.equals(VideoProgram.CLASS)) {
              // epg program (tv show)
              dlnaObject = new VideoProgram(cursor);
            } else {
              // generic/unknown DLNA object
              dlnaObject = new DlnaObject(cursor);
            }
            children.add((T) dlnaObject);
          } while (cursor.moveToNext());
        }
      } catch (Throwable e) {
        e.printStackTrace();
      } finally {
        if (cursor != null) {
          // close cursor
          cursor.close();
        }
      }
      return children;
    }
  }

  public List<VideoBroadcast> getChannels(String udn) {
    return getChildren(udn, "0/Channels", VideoBroadcast.class);
  }

  public List<VideoProgram> getEpgPrograms(String udn, Set<VideoBroadcast> channels, Date startDateTime, Date endDateTime) {
    List<VideoProgram> programs = new ArrayList<>();

    // create list of days needed for EPG data
    List<String> days = new ArrayList<>();
    Calendar calendar = Calendar.getInstance();
    for (calendar.setTime(startDateTime); calendar.getTimeInMillis() < endDateTime.getTime(); calendar.add(Calendar.DAY_OF_MONTH, 1)) {
      DateFormat format = new SimpleDateFormat("M-d");
      days.add(format.format(calendar.getTime()));
    }

    // iterate requested channels
    for (VideoBroadcast channel : channels) {
      for (String day : days) {
        // shortcut for creating the container ID to directly access EPG programs for channel & day
        // this may not work for non-Google EPG
        String containerId = "0/EPG/" + channel.getChannelId() + "/" + day;
        List<VideoProgram> shows = getChildren(udn, containerId, VideoProgram.class);
        for (VideoProgram show : shows) {
          Date showStart = show.getScheduledStartTime();
          Date showEnd = show.getScheduledEndTime();
          if (startDateTime.before(showEnd) && endDateTime.after(showStart)) {
            // show is in time range
            programs.add(show);
          }
        }
      }
    }
    return programs;
  }

  public VideoProgram getCurrentEpgProgram(String udn, VideoBroadcast channel) {
    DateFormat format = new SimpleDateFormat("M-d");
    Date now = new Date();
    String day = format.format(now);
    List<VideoProgram> shows = getChildren(udn, "0/EPG/" + channel.getChannelId() + "/" + day, VideoProgram.class);
    for (VideoProgram show : shows) {
      if (show.getScheduledStartTime().before(now) && show.getScheduledEndTime().after(now)) {
        // show found
        return show;
      }
    }
    return null;
  }

  private Context getDlnaServiceContext() throws PackageManager.NameNotFoundException {
    return context.createPackageContext(HUEY_PACKAGE, 0);
  }

  private Intent getDlnaServiceIntent() throws PackageManager.NameNotFoundException, IOException {
    String interfaceNames = networkHelper.getActiveIfNamesInCsvFormat();
    Context pkgCtxt = getDlnaServiceContext();
    Intent intent = new Intent(pkgCtxt, UpnpServiceCp.class);

    // startup settings for huey service
    intent.putExtra(UpnpServiceCp.DEVICE_LISTUP_MODE, UpnpServiceCp.MASK_OFFLINE_DEVICE /*or UNMASK_OFFLINE_DEVICE*/);
    intent.putExtra(UpnpServiceCp.NETWORK_INTERFACE, interfaceNames);
    intent.putExtra(UpnpServiceCp.CONTROL_POINT_TYPE, UpnpServiceCp.GENERIC_DEVICE_CP /* OR, UpnpServiceCp.DMS_CP | UpnpServiceCp.DMR_CP */);
    intent.putExtra(UpnpServiceCp.XAV_CLIENT_INFO_AV, "5.0");
    intent.putExtra(UpnpServiceCp.XAV_CLIENT_INFO_HN, "");
    intent.putExtra(UpnpServiceCp.XAV_CLIENT_INFO_CN, "Sony Corp.");
    intent.putExtra(UpnpServiceCp.XAV_CLIENT_INFO_MN, "Huey");
    intent.putExtra(UpnpServiceCp.XAV_CLIENT_INFO_MV, "2.0");
    intent.putExtra(UpnpServiceCp.XAV_PHYSICAL_UNIT_INFO_PA, "Huey_Pa");
    intent.putExtra(UpnpServiceCp.XAV_PHYSICAL_UNIT_INFO_PL, "Huey_Pl");
    intent.putExtra(UpnpServiceCp.EVENT_PORT_DMS_CP, networkHelper.getFreePort());
    intent.putExtra(UpnpServiceCp.SOAP_NUM, 8);
    intent.putExtra(UpnpServiceCp.INITIAL_MSEARCH_TIME, 2);

    // Following values should be optimized for your application UI/UX.
    intent.putExtra(UpnpServiceCp.INITIAL_BROWSE_REQUESTED_COUNT, 50);
    intent.putExtra(UpnpServiceCp.MAXIMUM_BROWSE_REQUESTED_COUNT, 50);

    return intent;
  }

  /**
   * Test task for extracting random EPG data from database.
   */
  private static class FindEpgDataTask extends AsyncTask<Void, Void, Void> {

    public static final String LOG_TAG = "DlnaTest";

    private DlnaHelper helper;
    private final String udn;

    public FindEpgDataTask(DlnaHelper helper, String udn) {
      this.helper = helper;
      this.udn = udn;
    }

    @Override
    protected Void doInBackground(Void... params) {
      // get channel list
      List<VideoBroadcast> allChannels = helper.getChannels(udn);
      if (allChannels.size() == 0) {
        Log.e(LOG_TAG, "No channels found.");
        return null;
      } else {
        Log.d(LOG_TAG, String.format("%d channels found.", allChannels.size()));
      }

      // pick some random channels
      Set<VideoBroadcast> searchChannels = new HashSet<>();
      for (int i = 0; i < 5; i++) {
        searchChannels.add(allChannels.get(Math.abs(new Random().nextInt()) % allChannels.size()));
      }

      // set the time interval
      Calendar start = Calendar.getInstance();
      start.add(Calendar.HOUR, -1);
      Calendar end = Calendar.getInstance();
      end.setTime(start.getTime());
      end.add(Calendar.HOUR, 6);

      // query EPG data
      long time = System.currentTimeMillis();
      Log.d(LOG_TAG, "Finding shows on " + searchChannels.size() + " channels from " + start.getTime() + " to " + end.getTime() + ":");
      List<VideoProgram> shows = helper.getEpgPrograms(udn, searchChannels, start.getTime(), end.getTime());
      Log.d(LOG_TAG, "Query finished in " + (System.currentTimeMillis() - time) + "msec.");

      // print search results
      for (VideoProgram show : shows) {
        Log.d(LOG_TAG, show.toString());
      }

      return null;
    }
  }

}
