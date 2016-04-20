package com.sony.sel.tvapp.util;

import android.database.ContentObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by peterv on 4/19/16.
 */
public interface DlnaInterface {

  /**
   * Observer for the state of the DLNA service.
   */
  interface DlnaServiceObserver {

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

  void startDlnaService(DlnaServiceObserver observer);

  void unregisterContentObserver(ContentObserver contentObserver);

  boolean isDlnaServiceStarted();

  void stopDlnaService();

  List<DlnaObjects.UpnpDevice> getDeviceList(@Nullable ContentObserver observer, boolean showAllDevices);

  @NonNull
  <T extends DlnaObjects.DlnaObject> List<T> getChildren(String udn, String parentId, Class<T> childClass, @Nullable ContentObserver contentObserver, boolean useCache);

  @NonNull
  List<DlnaObjects.VideoBroadcast> getChannels(@NonNull String udn, @Nullable ContentObserver
      contentObserver);

  DlnaObjects.VideoProgram getCurrentEpgProgram(String udn, DlnaObjects.VideoBroadcast channel);
}
