package com.sony.sel.tvapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.util.KeySequenceDetector;

/**
 * Base class for all app activities.
 */
public abstract class BaseActivity extends Activity {

  public static final String LOG_TAG = BaseActivity.class.getSimpleName();

  private boolean inBackground = true;
  private boolean registeredOnEventBus;
  private KeySequenceDetector settingsKeySequenceDetector;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // detector to go to settings
    settingsKeySequenceDetector = new KeySequenceDetector(new int[]{
        KeyEvent.KEYCODE_1,
        KeyEvent.KEYCODE_2,
        KeyEvent.KEYCODE_3,
        KeyEvent.KEYCODE_4
    }, 10000L);
  }

  @Override
  protected void onResume() {
    super.onResume();
    inBackground = false;
    if (!registeredOnEventBus) {
      EventBus.getInstance().register(this);
      registeredOnEventBus = true;
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (registeredOnEventBus) {
      // need to unregister here
      // event bus crashes sending events to Activities after onSaveInstanceState is called
      EventBus.getInstance().unregister(this);
      registeredOnEventBus = false;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    inBackground = true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (registeredOnEventBus) {
      EventBus.getInstance().unregister(this);
      registeredOnEventBus = false;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (settingsKeySequenceDetector.onKeyDown(keyCode, event) == true) {
      // matched secret key sequence to open settings
      startActivity(new Intent(this, SelectServerActivity.class));
      // handled
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public boolean isInBackground() {
    return inBackground;
  }
}
