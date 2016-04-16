package com.sony.sel.tvapp.app;

import android.app.Application;

import com.sony.sel.tvapp.util.DlnaHelper;

/**
 * Virtual STB application
 */
public class TVApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // start the DLNA service when app starts
    DlnaHelper.getHelper(this).startDlnaService(null);
  }

}
