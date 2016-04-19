package com.sony.sel.tvapp.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.huey.dlna.DlnaCdsStore;
import com.sony.huey.dlna.IUpnpServiceCp;
import com.sony.huey.dlna.UpnpServiceCp;
import com.sony.sel.util.NetworkHelper;
import com.sony.sel.util.ObserverSet;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;
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
  private Map<Uri, List<DlnaObject>> dlnaCache = new HashMap<>();

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

  public void unregisterContentObserver(ContentObserver contentObserver) {
    contentResolver.unregisterContentObserver(contentObserver);
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
   * Get the current list of known UPnP devices.
   *
   * @param observer       Oberver to receive notification if the device list changes after this query.
   * @param showAllDevices Show all UpnP devices, or only media servers?
   * @return The device list, or an empty list when no devices are found.
   */
  public List<UpnpDevice> getDeviceList(@Nullable ContentObserver observer, boolean showAllDevices) {
    List<UpnpDevice> devices = new ArrayList<>();
    Uri uri = UpnpServiceCp.CONTENT_URI;
    Cursor cursor = contentResolver.query(UpnpServiceCp.CONTENT_URI,
        UpnpServiceCp.PROJECTION,
        showAllDevices ?
            UpnpServiceCp.DEVICE_TYPE + " LIKE '%'" :
            UpnpServiceCp.DEVICE_TYPE + " LIKE '%:MediaServer:%'",
        null, null);
    if (cursor != null) {
      Log.d(TAG, "Device column names: " + new Gson().toJson(cursor.getColumnNames()));
      while (cursor.moveToNext()) {
        UpnpDevice device = new DlnaObjects.UpnpDevice();
        device.loadFromCursor(cursor);
        Log.d(TAG, device.toString());
        devices.add(device);
      }
      cursor.close();
    }
    if (observer != null) {
      contentResolver.registerContentObserver(uri, false, observer);
    }
    return devices;
  }

  /**
   * Get all DLNA child objects for a given parent.
   *
   * @param udn             UDN of the DLNA server to query.
   * @param parentId        Parent object ID (use {@link #DLNA_ROOT} for the top level)
   * @param childClass      Class of child objects expected. Used to define query columns.
   * @param contentObserver Observer to receive notifications if the contents of the parent object changes.
   * @param <T>             Class of child object.
   * @param useCache        Use cached content if available? Use with caution, DLNA nodes whose contents change often are not useful to cache.
   * @return List of children, or an empty list if no children were found.
   */
  @NonNull
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId, Class<T> childClass, @Nullable ContentObserver contentObserver, boolean useCache) {

    Uri uri = DlnaCdsStore.getObjectUri(udn, parentId);

    if (useCache && contentObserver == null) {
      List<DlnaObject> cachedContent = dlnaCache.get(uri);
      if (cachedContent != null) {
        Log.d(TAG, "Returning cached content for " + uri + ".");
        return (List<T>) cachedContent;
      }
    }

    Cursor cursor = null;
    List<T> children = new ArrayList<>();

    try {
      Log.d(TAG, "Get DLNA child objects. UDN = " + udn + ", ID = " + parentId + ".");
      Log.d(TAG, "URI = " + uri);
      String[] columns = DlnaObject.getColumnNames(childClass);
      Log.d(TAG, "Querying. UDN = " + udn + ", ID = " + parentId + ".");
      cursor = contentResolver.query(uri, columns, null, null, null);
      Log.d(TAG, "Response received. UDN = " + udn + ", ID = " + parentId + ".");
      if (contentObserver != null) {
        contentResolver.registerContentObserver(uri, false, contentObserver);
      }
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
          DlnaObject dlnaObject = DlnaObjects.DlnaClass.newInstance(upnpClass);
          dlnaObject.loadFromCursor(cursor);
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
      if (children != null) {
        dlnaCache.put(uri, (List<DlnaObject>) children);
      }
    }
    return children;
  }

  /**
   * Return the list of channels.
   *
   * @param udn             Server UDN.
   * @param contentObserver Optional ContentObserver to receive notification when channel list changes.
   * @return List of channels, or an empty list if no channels found.
   */
  @NonNull
  public List<VideoBroadcast> getChannels(@NonNull String udn, @Nullable ContentObserver
      contentObserver) {
    return getChildren(udn, "0/Channels", VideoBroadcast.class, contentObserver, false);
  }

  /**
   * Return the EPG program on the requested channel for the current date/time.
   *
   * @param udn     Server UDN.
   * @param channel Channel to get EPG data for.
   * @return The current program, or null if no program found.
   */
  public VideoProgram getCurrentEpgProgram(String udn, VideoBroadcast channel) {
    DateFormat format = new SimpleDateFormat("M-d");
    Date now = new Date();
    String day = format.format(now);
    List<VideoProgram> shows = getChildren(udn, "0/EPG/" + channel.getChannelId() + "/" + day, VideoProgram.class, null, true);
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
}
