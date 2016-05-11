package com.sony.sel.tvapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;

import java.util.ArrayList;
import java.util.List;

/**
 * DLNA cache backed by an SQLite database.
 */
public class DlnaSqlCache extends SQLiteOpenHelper implements DlnaCache {

  private static final String DATABASE_NAME = "dlnacache.db";
  public static final int DATABASE_VERSION = 4;

  private final String CREATE_DLNA_OBJECTS_TABLE = "CREATE TABLE `DLNAObjects` (\n" +
      "\t`UDN`\tTEXT,\n" +
      "\t`ParentID`\tTEXT,\n" +
      "\t`ID`\tTEXT,\n" +
      "\t`Title`\tTEXT,\n" +
      "\t`UPNPClass`\tTEXT,\n" +
      "\t`JSON`\tTEXT,\n" +
      "\t`ChildIndex`\tINTEGER,\n" +
      "\tPRIMARY KEY(UDN,ID)\n" +
      ");";

  SQLiteDatabase db;

  DlnaSqlCache(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    db = getWritableDatabase();
  }


  @Nullable
  @Override
  public <T extends DlnaObject> List<T> getChildren(String udn, String parentId) {
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
      } else {
        return null;
      }
    } finally {
      cursor.close();
    }
  }

  @Override
  public void add(String udn, String parentID, List<DlnaObject> children) {
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
      db.insert(
          "DLNAObjects",
          null,
          values
      );
    }
  }

  @Override
  public <T extends DlnaObject> List<T> search(String udn, String parentId, String searchText) {
    return new ArrayList<>();
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
}
