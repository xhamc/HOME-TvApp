package com.sony.sel.tvapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * DLNA cache backed by an SQLite database.
 */
public class DlnaSqlCache extends SQLiteOpenHelper implements DlnaCache {

  public static final String TAG = DlnaSqlCache.class.getSimpleName();

  private static final String DATABASE_NAME = "dlnacache.db";
  public static final int DATABASE_VERSION = 7;

  private final String CREATE_DLNA_OBJECTS_TABLE = "CREATE TABLE `DLNAObjects` (\n" +
      "\t`UDN`\tTEXT,\n" +
      "\t`ParentID`\tTEXT,\n" +
      "\t`ID`\tTEXT,\n" +
      "\t`Title`\tTEXT,\n" +
      "\t`UPNPClass`\tTEXT,\n" +
      "\t`JSON`\tTEXT,\n" +
      "\t`ChildIndex`\tINTEGER,\n" +
      "\t`ScheduledStartTime`\tINTEGER,\n" +
      "\t`ScheduledEndTime`\tINTEGER,\n" +
      "\t`ChannelID`\tSTRING,\n" +
      "\tPRIMARY KEY(UDN,ParentID,ID)\n" +
      "); \n" +
      "CREATE INDEX DLNAObjects_Index ON DLNAObjects(ChannelID,ScheduledStartTime,ScheduledEndTime);";

  SQLiteDatabase db;
  Map<String, SaveToCacheTask> cachingTasks = new HashMap<>();

  DlnaSqlCache(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    db = getWritableDatabase();
  }


  @Nullable
  @Override
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId) {
    waitForCache(udn, parentId);
    // query the cache
    Cursor cursor = db.query(
        "DLNAObjects",
        new String[]{"UPNPClass", "JSON"},
        "UDN = '" + udn + "' AND ParentID = '" + parentId + "'",
        null,
        null,
        null,
        "ChildIndex"
    );
    try {
      if (cursor.getCount() > 0) {
        return buildResults(cursor);
      } else {
        return null;
      }
    } finally {
      cursor.close();
    }
  }

  <T extends DlnaObject> List<T> buildResults(Cursor cursor) {
    Gson gson = new Gson();
    List<DlnaObject> results = new ArrayList<>();
    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
      String upnpClass = cursor.getString(cursor.getColumnIndex("UPNPClass"));
      String json = cursor.getString(cursor.getColumnIndex("JSON"));
      Class<? extends DlnaObject> clazz = DlnaObjects.DlnaClass.classOf(upnpClass);
      DlnaObject item = gson.fromJson(json, clazz);
      results.add(item);
    }
    return (List<T>) results;
  }

  @Override
  public void add(final String udn, final String parentID, final List<DlnaObject> children) {
    String key = udn + "/" + parentID;
    synchronized (cachingTasks) {
      if (cachingTasks.containsKey(key)) {
        // already in process of caching
        return;
      }
    }
    SaveToCacheTask task = new SaveToCacheTask(udn, parentID, children);
    synchronized (cachingTasks) {
      cachingTasks.put(udn + "/" + parentID, task);
    }
    task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
  }

  @Override
  public <T extends DlnaObject> List<T> search(String udn, String parentId, String searchText) {
    // query the cache
    Cursor cursor = db.query(
        "DLNAObjects",
        new String[]{"UPNPClass", "JSON"},
        "UDN = '" + udn + "' AND ParentID LIKE '" + parentId + "%' AND Title LIKE '%" + searchText.replaceAll("[^a-zA-Z0-9]","%") + "%'",
        null,
        null,
        null,
        null
    );
    try {
      if (cursor.getCount() > 0) {
        return buildResults(cursor);
      } else {
        return new ArrayList<>();
      }
    } finally {
      cursor.close();
    }
  }

  @Override
  public List<VideoProgram> searchEpg(String udn, final List<String> channels, Date startDateTime, Date endDateTime) {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    Log.d(TAG, String.format("Search EPG cache. udn = " + udn + ", channels = " + channels + ", start = " + format.format(startDateTime) + ", end = " + format.format(endDateTime) + "."));
    long time = System.currentTimeMillis();
    Cursor cursor = getEpgItems(udn, channels, startDateTime, endDateTime);
    Log.d(TAG, String.format("Search EPG cache. Query time = " + (System.currentTimeMillis() - time) + "ms."));
    try {
      if (cursor.getCount() > 0) {
        List<VideoProgram> results = buildResults(cursor);
        Log.d(TAG, String.format("Search EPG cache. Results time = " + (System.currentTimeMillis() - time) + "ms."));
        return results;
      } else {
        return new ArrayList<>();
      }
    } finally {
      cursor.close();
    }
  }

  @Override
  public int countEpgItems(@NonNull String udn, @NonNull List<String> channels, @NonNull Date startDateTime, @NonNull Date endDateTime) {
    Cursor cursor = getEpgItems(udn, channels, startDateTime, endDateTime);
    try {
      return cursor.getCount();
    } finally {
      cursor.close();
    }
  }

  @Nullable
  @Override
  public <T extends DlnaObject> T getItemById(@NonNull String udn, @NonNull String id) {
    Cursor cursor = null;
    try {
      cursor = db.query(
          "DLNAObjects",
          new String[]{"UPNPClass", "JSON"},
          "UDN = '" + udn + "' AND ID = '" + id + "'",
          null,
          null,
          null,
          null
      );
      if (cursor.getCount() > 0) {
        cursor.moveToFirst();
        String upnpClass = cursor.getString(cursor.getColumnIndex("UPNPClass"));
        String json = cursor.getString(cursor.getColumnIndex("JSON"));
        Class<? extends DlnaObject> clazz = DlnaObjects.DlnaClass.classOf(upnpClass);
        return (T) new Gson().fromJson(json, clazz);
      } else {
        return null;
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  private Cursor getEpgItems(@NonNull String udn, @Nullable List<String> channels, @NonNull Date startDateTime, @NonNull Date endDateTime) {
    // build channel list string for sql statement
    StringBuilder channelsString = new StringBuilder();
    if (channels != null) {
      for (String channel : channels) {
        if (channelsString.length() > 0) {
          channelsString.append(", ");
        }
        channelsString.append("'" + channel + "'");
      }
    }

    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    Log.d(TAG, "Find EPG items from " + format.format(startDateTime) + " to " + format.format(endDateTime) + (channels != null ? " in channels " + new Gson().toJson(channels) : "") + ".");

    // build query
    return db.query(
        "DLNAObjects",
        new String[]{"UPNPClass", "JSON"},
        "UDN = '" + udn + "'"
            + (channels != null ? " AND ChannelID IN (" + channelsString.toString() + ")" : "")
            + " AND ScheduledStartTime <= " + endDateTime.getTime()
            + " AND ScheduledEndTime > " + startDateTime.getTime(),
        null,
        null,
        null,
        "ChannelID, ScheduledStartTime"
    );
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_DLNA_OBJECTS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE DLNAObjects;");
    onCreate(db);
  }

  @Override
  public void reset() {
    this.onUpgrade(db, db.getVersion(), DATABASE_VERSION);
  }


  private void waitForCache(String udn, String parentId) {
    String key = udn + "/" + parentId;
    if (cachingTasks.containsKey(key)) {
      SaveToCacheTask task = cachingTasks.get(key);
      synchronized (task) {
        Log.d(TAG, "Waiting for caching task to complete.");
        try {
          task.wait();
          Log.d(TAG, "Caching task completed, continuing.");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private class SaveToCacheTask extends AsyncTask<Void, Void, Void> {

    private final String udn;
    private final String parentID;
    private final List<DlnaObject> children;

    public SaveToCacheTask(String udn, String parentID, List<DlnaObject> children) {
      this.udn = udn;
      this.parentID = parentID;
      this.children = children;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        // delete any existing cached children at this parent
        int deleted = db.delete(
            "DLNAObjects",
            "UDN = ? AND ParentID = ?",
            new String[]{udn, parentID}
        );
        if (deleted > 0) {
          Log.d(TAG, String.format("%d items deleted in cache before update.", deleted));
        }
        // insert new child records
        Gson gson = new Gson();
        int childIndex = 0;
        synchronized (this) {
          for (DlnaObject child : children) {
            ContentValues values = new ContentValues();
            values.put("UDN", udn);
            values.put("ParentID", parentID);
            values.put("ID", child.getId());
            values.put("Title", child.getTitle());
            values.put("UPNPClass", child.getUpnpClass());
            values.put("JSON", gson.toJson(child));
            values.put("ChildIndex", childIndex++);
            if (child instanceof VideoProgram) {
              // save EPG-specific fields
              VideoProgram videoProgram = (VideoProgram) child;
              values.put("ScheduledStartTime", videoProgram.getScheduledStartTime().getTime());
              values.put("ScheduledEndTime", videoProgram.getScheduledEndTime().getTime());
              values.put("ChannelID", videoProgram.getChannelId());
            }
            db.insert(
                "DLNAObjects",
                null,
                values
            );
          }
          notifyAll();
        }
        Log.d(TAG, String.format("%d items added to cache.", children.size()));
      } catch (SQLiteException error) {
        Log.e(TAG, "Error adding children to cache. udn = " + udn + " parent = " + parentID + " error = " + error);
      }
      synchronized (cachingTasks) {
        cachingTasks.remove(udn + "/" + parentID);
      }
      return null;
    }
  }
}
