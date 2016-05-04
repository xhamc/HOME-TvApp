package com.sony.sel.tvapp.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Android content provider for searching EPG data.
 */
public class EpgSearchContentProvider extends ContentProvider {

  public static final String TAG = EpgSearchContentProvider.class.getSimpleName();

  @Override
  public boolean onCreate() {
    Log.d(TAG, "Content provider created.");
    return true;
  }

  @Nullable
  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.d(TAG, "Query URI: "+uri+".");
    return null;
  }

  @Nullable
  @Override
  public String getType(Uri uri) {
    Log.d(TAG, "Get type for URI "+uri+".");
    return null;
  }

  @Nullable
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    // not implemented
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    // not implemented
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    // not implemented
    return 0;
  }
}
