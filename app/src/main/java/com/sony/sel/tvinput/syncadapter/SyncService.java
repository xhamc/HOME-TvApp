/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sony.sel.tvinput.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service which provides the SyncAdapter implementation to the framework on request.
 */
public class SyncService extends Service {

  public static final String TAG = SyncService.class.getSimpleName();

  private SyncAdapter syncAdapter = null;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "Sync service onCreate().");
    if (syncAdapter == null) {
      syncAdapter = new SyncAdapter(this, true);
    }
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "Sync service onDestroy().");
    super.onDestroy();
    syncAdapter = null;
   }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "Sync service onBind().");
    return syncAdapter.getSyncAdapterBinder();
  }
}
