package com.sony.sel.tvapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DLNA cache backed by an SQLite database.
 */
public class DlnaSqlCache extends SQLiteOpenHelper implements DlnaCache {

  private static final String DATABASE_NAME = "dlnacache.db";
  public static final int DATABASE_VERSION = 6;

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
      ");";

  SQLiteDatabase db;

  DlnaSqlCache(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    db = getWritableDatabase();
  }


  @Nullable
  @Override
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId) {
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
    // perform caching on a separate thread so we can return quickly
    new Thread(new Runnable() {
      @Override
      public void run() {
        // delete any existing cached children at this parent
        db.delete(
            "DlnaObjects",
            "UDN = '" + udn + "' AND ParentID = '" + parentID + "'",
            null
        );
        // insert new child records
        Gson gson = new Gson();
        int childIndex = 0;
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
      }
    }).start();
  }

  @Override
  public <T extends DlnaObject> List<T> search(String udn, String parentId, String searchText) {
    // query the cache
    Cursor cursor = db.query(
        "DLNAObjects",
        new String[]{"UPNPClass", "JSON"},
        "UDN = '" + udn + "' AND ParentID LIKE '" + parentId + "%' AND Title LIKE '%" + searchText + "%'",
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
    // build channel list string for sql statement
    StringBuilder channelsString = new StringBuilder();
    for (String channel : channels) {
      if (channelsString.length() > 0) {
        channelsString.append(", ");
      }
      channelsString.append("'" + channel + "'");
    }
    // build query
    Cursor cursor = db.query(
        "DLNAObjects",
        new String[]{"UPNPClass", "JSON"},
        "UDN = '" + udn + "'"
            + " AND ChannelID IN (" + channelsString.toString() + ")"
            + " AND ScheduledStartTime <= " + endDateTime.getTime()
            + " AND ScheduledEndTime > " + startDateTime.getTime(),
        null,
        null,
        null,
        "ChannelID, ScheduledStartTime"
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
    db.execSQL("TRUNCATE TABLE DlnaObjects");
  }
}
