package com.sony.sel.tvapp.util;

import android.content.Context;

/**
 * Factory class for DLNA interface.
 */
public class DlnaHelper {

  public static final String TAG = DlnaHelper.class.getSimpleName();

  // object ID for dlna root directory
  public static final String DLNA_ROOT = "0";

  private static DlnaInterface INSTANCE;
  /**
   * Get the helper instance.
   */
  public static DlnaInterface getHelper(Context context) {
    if (INSTANCE == null) {
      // ensure application context is used to prevent leaks
      INSTANCE = new ClingDlnaHelper(context.getApplicationContext());
    }
    return INSTANCE;
  }
}
