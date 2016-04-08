package com.sony.sel.tvapp.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Background service for kicking off EPG synchronization
 */
public class EpgService extends IntentService {

  public static final String TAG = EpgService.class.getSimpleName();

  public EpgService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent intent) {

  }
}
