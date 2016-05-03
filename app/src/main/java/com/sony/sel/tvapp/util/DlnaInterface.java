package com.sony.sel.tvapp.util;

import android.database.ContentObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Interface to DLNA helpers.
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

  /**
   * Start and bind the DLNA service.
   *
   * @param observer Observer to receive notification when the service has started.
   */
  void startDlnaService(DlnaServiceObserver observer);

  /**
   * Unregister a content observer that has been watching for changes.
   *
   * @param contentObserver Observer to unregister.
   */
  void unregisterContentObserver(ContentObserver contentObserver);

  /**
   * Has the DLNA service been started and bound?
   */
  boolean isDlnaServiceStarted();

  /**
   * Stop the DLNA service. If a ContentObserver was provided to {@link #startDlnaService(DlnaServiceObserver)}
   * the observer will be notified after stop is complete.
   */
  void stopDlnaService();

  /**
   * Return the list of DLNA devices. Since this list is volatile, it is important
   * to register a ContentObserver to watch for changes to the list.
   *
   * @param observer       Observer to receive change notifications.
   * @param showAllDevices True to show all device types. False to show only devices that host a ContentDirectory service.
   * @return List of devices, or an empty list if none are yet discovered.
   */
  @NonNull
  List<DlnaObjects.UpnpDevice> getDeviceList(@Nullable ContentObserver observer, boolean showAllDevices);

  /**
   * Return the DLNA child objects of a given parent by performing a Browse action
   * on the ContentDirectory service of the requested device.
   *
   * @param udn             Device UDN.
   * @param parentId        Parent ID. "0" is the "root parent".
   * @param childClass      Expected {@link com.sony.sel.tvapp.util.DlnaObjects.DlnaObject} subclass of child elements.
   * @param contentObserver Observer to receive notification if the contents change.
   * @param useCache        Use cached data if available.
   * @param <T>             Expected class of child elements.
   * @return List of child objects, or empty list if none are found or an error occurs.
   */
  @NonNull
  <T extends DlnaObjects.DlnaObject> List<T> getChildren(String udn, String parentId, Class<T> childClass, @Nullable ContentObserver contentObserver, boolean useCache);

  /**
   * Search for items on a DLNA server.
   *
   * @param udn        Device UDN.
   * @param parentId   Parent ID to search. "0" is the "root parent".
   * @param query      Search query string.
   * @param childClass Expected {@link com.sony.sel.tvapp.util.DlnaObjects.DlnaObject} subclass of child elements.
   * @param <T>        Expected class of child elements.
   * @return List of found items, or empty list if none are found or an error occurs.
   */
  @NonNull
  <T extends DlnaObjects.DlnaObject> List<T> search(String udn, String parentId, String query, final Class<T> childClass);

  /**
   * Return the list of EPG channels.
   *
   * @param udn             Device UDN.
   * @param contentObserver Observer to receive notification of list changes.
   * @return The list of channels, or empty list if none are found, or an error.
   */
  @NonNull
  List<DlnaObjects.VideoBroadcast> getChannels(@NonNull String udn, @Nullable ContentObserver
      contentObserver);

  /**
   * Return the current EPG program on a given channel.
   *
   * @param udn     Device UDN.
   * @param channel Channel ID.
   * @return Current EPG program, or null if none is found.
   */
  @Nullable
  DlnaObjects.VideoProgram getCurrentEpgProgram(String udn, DlnaObjects.VideoBroadcast channel);

  /**
   * Return the list of EPG programs available for a current channel and time frame.
   *
   * @param udn       Device UDN.
   * @param channel   Channel ID.
   * @param startDate Starting date/time.
   * @param endDate   Ending date/time.
   * @return List of EPG programs, or an empty list if none are found.
   */
  @NonNull
  List<DlnaObjects.VideoProgram> getEpgPrograms(String udn, DlnaObjects.VideoBroadcast channel, Date startDate, Date endDate);

  /**
   * Set the list of favorite channels. Affects the sort order of all subsequent {@link #getChannels(String, ContentObserver)} requests.
   *
   * @param channelIds
   */
  void setFavoriteChannels(Set<String> channelIds);
}
