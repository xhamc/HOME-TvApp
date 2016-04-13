package com.sony.sel.tvapp.fragment;

import android.app.Fragment;
import android.os.Bundle;

import com.sony.sel.tvapp.util.EventBus;


/**
 * Base class for all app fragments.
 */
public abstract class BaseFragment extends Fragment {

  private boolean inBackground = true;

  protected BaseFragment() {
    super();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EventBus.getInstance().register(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    inBackground = false;
  }

  @Override
  public void onPause() {
    super.onPause();
    inBackground = true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    EventBus.getInstance().unregister(this);
  }

  protected boolean isInBackground() {
    return inBackground;
  }

}
