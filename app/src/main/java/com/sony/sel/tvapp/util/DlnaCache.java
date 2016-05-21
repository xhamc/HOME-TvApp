package com.sony.sel.tvapp.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

import java.util.Date;
import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

/**
 * Interface to a cache for DLNA data.
 */
public interface DlnaCache {

  @Nullable
  <T extends DlnaObject> List<T> getChildren(String udn, String parentId);

  /**
   * Add a list of child objects to the cache.
   *
   * @param udn      Server UDN.
   * @param parentID Parent object ID.
   * @param children Child objects to add to the cache.
   */
  void add(@NonNull String udn, @NonNull String parentID, @NonNull List<DlnaObject> children);

  /**
   * Perform a generic search of the cache and return a list of found objects.
   *
   * @param udn        Server UDN.
   * @param parentId   Parent object ID. All levels below this parent will be searched.
   * @param searchText Text to search for in object titles.
   * @param <T>        Class of search results expected. If in doubt, use {@link DlnaObject} base class.
   * @return List of matching items, or an empty list if none found.
   */
  @NonNull
  <T extends DlnaObject> List<T> search(@NonNull String udn, @NonNull String parentId, @NonNull String searchText);

  /**
   * Perform an EPG specific search of the cache.
   *
   * @param udn           Server UDN.
   * @param channels      List of channel IDs to search or null for all channels.
   * @param startDateTime Starting date/time to search for. (Overlapping programs will be returned.)
   * @param endDateTime   Ending date/time to search for. (Overlapping programs will be returned.)
   * @return A list of EPG programs sorted by channel and date, or empty list of none found.
   */
  @NonNull
  List<VideoProgram> searchEpg(@NonNull String udn, @Nullable List<String> channels, @NonNull Date startDateTime, @NonNull Date endDateTime);

  /**
   * Check the cache contents and return a count of programs for the specified search criteria.
   *
   * @param udn           Server UDN.
   * @param channels      List of channel IDs to search.
   * @param startDateTime Starting date/time to search for. (Overlapping programs will be returned.)
   * @param endDateTime   Ending date/time to search for. (Overlapping programs will be returned.)
   * @return The total number of EPG programs matching the given criteria.
   */
  int countEpgItems(@NonNull String udn, @NonNull List<String> channels, @NonNull Date startDateTime, @NonNull Date endDateTime);

  /**
   * Retrieve a single item from the cache by ID.
   *
   * @param id  Unique ID.
   * @param <T> Item type to return.
   * @return The item, if found in the cache.
   */
  @Nullable
  <T extends DlnaObject> T getItemById(@NonNull String udn, @NonNull String id);

  /**
   * Clear and reset the cache.
   */
  void reset();
}
