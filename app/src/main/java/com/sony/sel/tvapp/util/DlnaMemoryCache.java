package com.sony.sel.tvapp.util;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

/**
 * Simple in-memory cache for DLNA child data
 */
public class DlnaMemoryCache implements DlnaCache {

  private Map<String, List<DlnaObject>> dlnaCache = new HashMap<>();

  @Override
  @Nullable
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId) {
    synchronized (dlnaCache) {
      List<DlnaObject> cachedChildren = dlnaCache.get(udn + "/" + parentId);
      if (cachedChildren != null) {
        return (List<T>) cachedChildren;
      }
    }
    return null;
  }

  @Override
  public void add(String udn, String parentId, List<DlnaObject> children) {
    synchronized (dlnaCache) {
      dlnaCache.put(udn + "/" + parentId, children);
    }
  }

  @Override
  public <T extends DlnaObject> List<T> search(String udn, String parentId, String searchText) {
    final List<DlnaObject> results = new ArrayList<>();
    synchronized (dlnaCache) {
      for (String key : dlnaCache.keySet()) {
        if (key.startsWith(udn + "/" + parentId)) {
          // matching parent, check for matching content
          List<DlnaObject> contents = dlnaCache.get(key);
          for (DlnaObject item : contents) {
            if (item.getTitle().toLowerCase().contains(searchText.toLowerCase())) {
              results.add(item);
            }
          }
        }
      }
    }
    return (List<T>) results;
  }

  @Override
  public List<DlnaObjects.VideoProgram> searchEpg(String udn, List<String> channels, Date startDateTime, Date endDateTime) {
    return null;
  }
}
