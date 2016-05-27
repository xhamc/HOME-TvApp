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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;

/**
 * Static helper methods for working with the SyncAdapter framework.
 */
@TargetApi(21)
public class SyncUtils {

  public static final String TAG = SyncUtils.class.getSimpleName();

  private static final String CONTENT_AUTHORITY = TvContract.AUTHORITY;
  private static final String ACCOUNT_TYPE = "com.sony.sel.tvinput.account";
  private static final String BUNDLE_KEY_INPUT_ID = "bundle_key_input_id";
  private static final String BUNDLE_KEY_CURRENT_PROGRAM_ONLY = "bundle_key_current_program_only";

  private static SyncUtils INSTANCE;

  public static SyncUtils getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SyncUtils();
    }
    return INSTANCE;
  }

  /**
   * Set up a periodic sync on the requested time interval (in seconds)
   *
   * @param context       App context.
   * @param inputId       Input ID of our service.
   * @param pollFrequency Polling interval in seconds.
   */
  public void setUpPeriodicSync(Context context, String inputId, long pollFrequency) {
    Log.d(TAG, "setUpPeriodicSync");
    Account account = DummyAccountService.getAccount(ACCOUNT_TYPE);
    AccountManager accountManager =
        (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
    if (!accountManager.addAccountExplicitly(account, null, null)) {
      Log.e(TAG, "Account already exists.");
    }
    ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
    Bundle bundle = new Bundle();
    bundle.putString(BUNDLE_KEY_INPUT_ID, inputId);
    ContentResolver.cancelSync(account, CONTENT_AUTHORITY);
    ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, bundle,
        pollFrequency);
  }

  /**
   * Request an EPG sync right away.
   *
   * @param inputId            Input ID
   * @param currentProgramOnly Sync only the current showing program?
   */
  public void requestSync(String inputId, boolean currentProgramOnly) {
    Log.d(TAG, "requestSync");
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    bundle.putString(BUNDLE_KEY_INPUT_ID, inputId);
    bundle.putBoolean(BUNDLE_KEY_CURRENT_PROGRAM_ONLY, currentProgramOnly);
    ContentResolver.requestSync(DummyAccountService.getAccount(ACCOUNT_TYPE), CONTENT_AUTHORITY,
        bundle);
  }
}
