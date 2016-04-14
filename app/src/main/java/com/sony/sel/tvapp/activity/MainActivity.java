package com.sony.sel.tvapp.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.NavigationFragment;
import com.sony.sel.tvapp.fragment.VideoFragment;
import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;

import java.util.Stack;

/**
 * Main Activity
 */
public class MainActivity extends BaseActivity {

  public static final String TAG = MainActivity.class.getSimpleName();

  private VideoFragment videoFragment;
  private Fragment currentFragment;
  private NavigationFragment navigationFragment;

  private Stack<View> focusStack = new Stack<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String urn = SettingsHelper.getHelper(this).getEpgServer();
    if (urn == null) {
      // go to server selection
      startActivity(new Intent(this, SelectServerActivity.class));
      finish();
      return;
    }

    setContentView(R.layout.main_activity);

    initFragments();

  }

  private void initFragments() {
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();

    // background video fragment
    videoFragment = new VideoFragment();
    transaction.add(R.id.videoFrame, videoFragment);

    // navigation fragment
    navigationFragment = new NavigationFragment();
    transaction.add(R.id.navigationFrame, navigationFragment);

    transaction.commit();
  }

  @Subscribe
  public void onNavigate(EventBus.NavigationClickedEvent event) {

    if (currentFragment != null && currentFragment.getTag().equals(event.getItem().getTag())) {
      // same item
      return;
    }

    pushFocus();

    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();

    if (currentFragment != null) {
      transaction.hide(currentFragment);
    }

    NavigationItem item = event.getItem();
    currentFragment = fragmentManager.findFragmentByTag(item.getTag());
    if (currentFragment != null) {
      // fragment already exists, just show it
      transaction.show(currentFragment);
    } else {
      // create the fragment
      currentFragment = event.getItem().getFragment();
      if (currentFragment != null) {
        transaction.add(R.id.contentFrame, currentFragment, item.getTag());
      }
    }

    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.commit();
  }

  /**
   * Push the currently focused view onto a focus "back stack".
   */
  private void pushFocus() {

    View v = null;
    if (currentFragment != null) {
      v = currentFragment.getView().findFocus();
    }
    if (v == null && navigationFragment != null) {
      v = navigationFragment.getView().findFocus();
    }
    if (v != null) {
      focusStack.push(v);
    }
  }

  /**
   * Pop the focus back to View from the last {@link #pushFocus()} call.
   *
   * @return true if focus was changed.
   */
  private boolean popFocus() {
    if (focusStack.size() > 0) {
      View v = focusStack.pop();
      v.requestFocus();
      return true;
    } else {
      return false;
    }
  }

  private void hideCurrentFragment() {
    // pop the detail fragment
    if (currentFragment != null) {
      FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
      fragmentTransaction.hide(currentFragment);
      fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
      fragmentTransaction.commit();
      currentFragment = null;
      popFocus();
    }
  }

  @Subscribe
  public void onChannelChanged(EventBus.ChannelChangedEvent event) {
    hideCurrentFragment();
    navigationFragment.hide();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_BACK: {
          if (currentFragment != null) {
            hideCurrentFragment();
            return true;
          } else if (navigationFragment.isShown()) {
            navigationFragment.hide();
            return true;
          } else {
            // back when navigation fragment focused, confirm quitting the app.
            new AlertDialog.Builder(this).setMessage(R.string.quitConfirmation).setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                finish();
              }
            }).setNegativeButton(R.string.cancel, null).create().show();
            return true;
          }
        }
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER: {
          if (!navigationFragment.isShown()) {
            navigationFragment.show();
            return true;
          }
        }
      }
    }
    return super.onKeyDown(keyCode, event);
  }

}
