package com.sony.sel.tvapp;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * Tests for content provider.
 */
public class ContentProviderTest extends InstrumentationTestCase {

  public static final String TAG = ContentProviderTest.class.getSimpleName();

  /**
   * Emulate an Android EPG system search against the built in content provider.
   */
  public void test_contentProvider_shouldReturnEpg() {
    final String query = "maverick";
    ContentResolver resolver = getInstrumentation().getTargetContext().getContentResolver();
    Cursor cursor = resolver.query(Uri.parse("content://com.sony.sel.tvapp"), null, "EPG = ?", new String[]{query}, null, null);
    assertNotNull("Cursor was null.", cursor);
    assertTrue("Results were empty.", cursor.getCount() > 0);
    Log.d(TAG, String.format("Search for %s returned %d EPG results.", query, cursor.getCount()));
  }

  /**
   * Emulate an Android VOD system search against the built in content provider.
   */
  public void test_contentProvider_shouldReturnVod() {
    final String query = "maverick";
    ContentResolver resolver = getInstrumentation().getTargetContext().getContentResolver();
    Cursor cursor = resolver.query(Uri.parse("content://com.sony.sel.tvapp"), null, "VOD = ?", new String[]{query}, null, null);
    assertNotNull("Cursor was null.", cursor);
    assertTrue("Results were empty.", cursor.getCount() > 0);
    Log.d(TAG, String.format("Search for %s returned %d VOD results.", query, cursor.getCount()));
  }
}
