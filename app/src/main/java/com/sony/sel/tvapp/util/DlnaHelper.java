package com.sony.sel.tvapp.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.huey.dlna.DlnaCdsStore;
import com.sony.huey.dlna.IUpnpServiceCp;
import com.sony.huey.dlna.UpnpServiceCp;
import com.sony.sel.util.NetworkHelper;
import com.sony.sel.util.ObservableAsyncTask;
import com.sony.sel.util.ObserverSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import static com.sony.sel.tvapp.util.DlnaObjects.EpgContainer;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import static com.sony.sel.tvapp.util.DlnaObjects.Container;

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
  private NetworkHelper networkHelper;

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
  private EpgServerIteratorTask iteratorTask;

  private Map<String,List<EpgContainer>> epgCache = new HashMap<>();
  private Map<String,List<VideoBroadcast>> channelCache = new HashMap<>();

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

    Log.d(TAG, "Get DLNA child objects. UDN = " + udn + ", ID = " + parentId + ".");
    Cursor cursor = null;

    List<T> children = new ArrayList<>();

    try {
      Uri uri = DlnaCdsStore.getObjectUri(udn, parentId);
      String[] columns = DlnaObject.getColumnNames(childClass);
      cursor = context.getContentResolver().query(uri, columns, null, null, null);
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
          children.add((T)dlnaObject);
        } while (cursor.moveToNext());
      }
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return children;
  }

  public interface EpgIterationListener extends ObservableAsyncTask.AsyncTaskObserver<Integer,Integer> {
  }

  public void iterateEpgServer(String serverUdn, EpgIterationListener observer) {
    if (iteratorTask == null) {
      iteratorTask = new EpgServerIteratorTask();
      iteratorTask.getObservers().add(new ObservableAsyncTask.AsyncTaskObserver<Integer, Integer>() {
        @Override
        public void onStarting() {
          Log.d(TAG, "EPG iterator starting.");
        }

        @Override
        public void onProgress(Integer... values) {
          Log.d(TAG, "EPG iterator progress: "+values[0]+" items.");
        }

        @Override
        public void onCanceled(Integer integer) {
          Log.w(TAG, "EPG iterator canceled: "+integer+" items.");
          // clear task on completion
          iteratorTask = null;
        }

        @Override
        public void onComplete(Integer integer) {
          Log.d(TAG, "EPG iterator complete: "+integer+" items.");
          // clear task on completion
          iteratorTask = null;
        }

        @Override
        public void onError(Throwable error) {
          Log.e(TAG, "EPG iterator error: "+error);
        }
      });
      iteratorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverUdn);
    }
    if (observer != null) {
      iteratorTask.getObservers().add(observer);
    }
  }

  private class EpgServerIteratorTask extends ObservableAsyncTask<String, Integer, Integer> {

    @Override
    protected Integer doInBackground(String... params) {
      String udn = params[0];
      int objectCount = 0;
      List<Container> rootObjects = getChildren(
          udn,
          DlnaHelper.DLNA_ROOT,
          Container.class
      );
      objectCount += rootObjects.size();
      for (DlnaObject child : rootObjects) {
        if (isCancelled()) {
          // canceled
          return objectCount;
        } else {
          // report progress
          publishProgress(objectCount);
        }
        if (child.getTitle().equals("Channels")) {
          List<VideoBroadcast> channels = getChildren(
              udn,
              child.getId(),
              VideoBroadcast.class
          );
          objectCount += channels.size();
        } else if (child.getTitle().equals("EPG")) {
          // get epg channel containers
          List<EpgContainer> epgChannels = getChildren(
              udn,
              child.getId(),
              EpgContainer.class
          );
          objectCount += epgChannels.size();
          for (DlnaObject epgChannel : epgChannels) {
            // check if canceled
            if (isCancelled()) {
              // canceled
              return objectCount;
            } else {
              // report progress
              publishProgress(objectCount);
            }
            // get containers for each day
            List<EpgContainer> epgDays = getChildren(
                udn,
                epgChannel.getId(),
                EpgContainer.class
            );
            objectCount += epgDays.size();
            for (DlnaObject epgDay : epgDays) {
              if (isCancelled()) {
                // canceled
                return objectCount;
              } else {
                // report progress
                publishProgress(objectCount);
              }
              // get the video programs for each day
              List<VideoProgram> epgPrograms = getChildren(
                  udn,
                  epgDay.getId(),
                  VideoProgram.class
              );
              objectCount += epgPrograms.size();
            }
          }
        }
      }
      return objectCount;
    }
  }

  public List<VideoBroadcast> getChannels(String udn) {
    return getChildren(udn, "0/Channels", VideoBroadcast.class);
  }

  public List<VideoProgram> getEpgPrograms(String udn, Set<String> channelIds, Date startDateTime, Date endDateTime) {
    // make a copy so we can delete channels that have been scanned
    Set<String> mutableChannelIds = new HashSet<>(channelIds);
    List<EpgContainer> epgChannels = getChildren(udn, "0/EPG", EpgContainer.class);
    List<VideoProgram> programs = new ArrayList<>();
    for (EpgContainer channel : epgChannels) {
      if (mutableChannelIds.contains(channel.getChannelId())) {
        List<EpgContainer> days = getChildren(udn, channel.getId(), EpgContainer.class);
        for (EpgContainer day : days) {
          Date dayStart = day.getDateTimeRangeStart();
          Date dayEnd = day.getDateTimeRangeEnd();
          if (startDateTime.before(dayEnd) && endDateTime.after(dayStart)) {
            // dates overlap, parse shows
            List<VideoProgram> shows = getChildren(udn, day.getId(), VideoProgram.class);
            for (VideoProgram show : shows) {
              Date showStart = show.getScheduledStartTime();
              Date showEnd = show.getScheduledEndTime();
              if (startDateTime.before(showEnd) && endDateTime.after(showStart)) {
                // show is in date range
                programs.add(show);
              }
            }
          }
        }
        mutableChannelIds.remove(channel.getChannelId());
        if (mutableChannelIds.size() == 0) {
          // done
          break;
        }
      }
    }
    return programs;
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
