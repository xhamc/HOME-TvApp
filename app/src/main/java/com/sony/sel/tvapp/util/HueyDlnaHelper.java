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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

/**
 * DLNA implementation based on Huey libraries.
 */
public class HueyDlnaHelper extends BaseDlnaHelper {

  public static final String TAG = HueyDlnaHelper.class.getSimpleName();

  // object ID for dlna root directory
  public static final String DLNA_ROOT = "0";

  // name of DLNA service
  private static final String HUEY_PACKAGE = "com.sony.huey.dlna.module";

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

  public HueyDlnaHelper(Context context) {
    super(context);
    this.contentResolver = context.getContentResolver();
    networkHelper = NetworkHelper.getHelper(getContext());
  }

  /**
   * Start the DLNA service. Until the service is started, data routines
   * will return empty results.
   *
   * @param observer AsyncTaskObserver to receive state notifications about the DLNA service.
   */
  @Override
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
      getContext().startService(getDlnaServiceIntent());

      // bind the service
      Intent hueyIntent = new Intent(getDlnaServiceContext(), UpnpServiceCp.class);
      getContext().bindService(hueyIntent, hueyConnection, Context.BIND_AUTO_CREATE);

    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Error starting DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    } catch (IOException e) {
      Log.e(TAG, "Error starting DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    }

    Cursor c = getContext().getContentResolver().query(UpnpServiceCp.CONTENT_URI,
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

  @Override
  public void unregisterContentObserver(ContentObserver contentObserver) {
    contentResolver.unregisterContentObserver(contentObserver);
  }

  /**
   * Returns true if the DNLA service is currently started & connected.
   */
  @Override
  public boolean isDlnaServiceStarted() {
    return hueyService != null;
  }

  /**
   * Stop and disconnect the DLNA service.
   */
  @Override
  public void stopDlnaService() {
    if (hueyService == null) {
      // already stopped
      return;
    }
    try {
      Context serviceContext = getDlnaServiceContext();
      Intent intent = new Intent(serviceContext, UpnpServiceCp.class);
      getContext().stopService(intent);
      return;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Error stopping DLNA service: " + e);
      serviceObservers.proxy().onError(e);
    }
  }

  @Override
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

  @Override
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

  @NonNull
  @Override
  public <T extends DlnaObject> List<T> search(String udn, String parentId, String query, Class<T> childClass) {
    return null;
  }

  private Context getDlnaServiceContext() throws PackageManager.NameNotFoundException {
    return getContext().createPackageContext(HUEY_PACKAGE, 0);
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
