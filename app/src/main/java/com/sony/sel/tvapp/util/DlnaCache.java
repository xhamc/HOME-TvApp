package com.sony.sel.tvapp.util;

import android.support.annotation.Nullable;

import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

/**
 * Interface to a cache for DLNA data.
 */
public interface DlnaCache {

  @Nullable
  <T extends DlnaObject> List<T> getChildren(String udn, String parentId);

  void add(String udn, String parentID, List<DlnaObject> children);

  <T extends DlnaObject> List<T> search(String udn, String parentId, String searchText);
}
