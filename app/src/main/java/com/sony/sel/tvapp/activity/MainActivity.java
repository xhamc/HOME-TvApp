package com.sony.sel.tvapp.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.BaseFragment;
import com.sony.sel.tvapp.fragment.ChannelInfoFragment;
import com.sony.sel.tvapp.fragment.NavigationFragment;
import com.sony.sel.tvapp.fragment.VideoFragment;
import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.EpgCachingTask;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.EventBus.ChannelChangedEvent;
import com.sony.sel.tvapp.util.EventBus.PlayVodEvent;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.util.VodCachingTask;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;

/**
 * Main Activity
 */
public class MainActivity extends BaseActivity {

  public static final String TAG = MainActivity.class.getSimpleName();

  private VideoFragment videoFragment;
  private ChannelInfoFragment channelInfoFragment;
  private BaseFragment currentFragment;
  private NavigationFragment navigationFragment;

  /// short timeout for hiding UI after quick interactions
  public static final long HIDE_UI_TIMEOUT = 5000;

  /// long timeout allows time for more viewing/longer processes but still times out eventually
  public static final long HIDE_UI_TIMEOUT_LONG = 30000;


  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;
  private SettingsHelper settingsHelper;

  private final Handler handler = new Handler();
  private Runnable hideUiRunnable = new Runnable() {
    @Override
    public void run() {
      hideUi();
    }
  };
  private final Runnable epgCachingRunnable = new Runnable() {
    @Override
    public void run() {
      epgCachingTask = new EpgCachingTask(dlnaHelper, dlnaCache, settingsHelper.getEpgServer());
      epgCachingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  };
  private EpgCachingTask epgCachingTask;
  private Runnable vodCachingRunnable = new Runnable() {
    @Override
    public void run() {
      vodCachingTask = new VodCachingTask(dlnaHelper, settingsHelper.getEpgServer());
      vodCachingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  };
  private VodCachingTask vodCachingTask;


  @Override
  protected void onPause() {
    super.onPause();
    hideUi();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // stop the DLNA service on the way out
    DlnaHelper.getHelper(this).stopDlnaService();
    // cancel caching in the background
    cancelEpgCaching();
    cancelVodCaching();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    dlnaHelper = DlnaHelper.getHelper(this);
    dlnaCache = DlnaHelper.getCache(this);
    settingsHelper = SettingsHelper.getHelper(this);

    String urn = settingsHelper.getEpgServer();
    if (urn == null) {
      // go to server selection
      startActivity(new Intent(this, SelectServerActivity.class));
      finish();
      return;
    }

    setContentView(R.layout.main_activity);
    ButterKnife.bind(this);

    initFragments();

    // start caching EPG 10 seconds after starting
    startEpgCaching(10000);

    // start caching VOD 1 minute later
    startVodCaching(70000);
  }

  private void startVodCaching(long delay) {
    // cancel any caching in progress
    cancelVodCaching();
    // start new caching task
    handler.postDelayed(vodCachingRunnable, delay);
  }

  private void cancelVodCaching() {
    handler.removeCallbacks(vodCachingRunnable);
    if (vodCachingTask != null) {
      vodCachingTask.cancel(true);
      vodCachingTask = null;
    }
  }

  private void startEpgCaching(long delay) {
    // cancel any caching in progress
    cancelEpgCaching();
    // start new caching task
    handler.postDelayed(epgCachingRunnable, delay);
  }

  private void cancelEpgCaching() {
    handler.removeCallbacks(epgCachingRunnable);
    if (epgCachingTask != null) {
      epgCachingTask.cancel(true);
      epgCachingTask = null;
    }
  }

  @Override
  public void onVisibleBehindCanceled() {
    super.onVisibleBehindCanceled();
    videoFragment.onVisibleBehindCanceled();
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    // restart caching when server changes
    startEpgCaching(0);
    startVodCaching(0);
  }

  /**
   * Initialize fragments.
   * <p/>
   * Note that this also can be called when Fragments have automatically been restored by Android.
   * In this case we need to attach and configure existing Fragments instead of making new ones.
   */
  private void initFragments() {
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();

    // video playback fragment
    videoFragment = (VideoFragment) fragmentManager.findFragmentByTag(VideoFragment.TAG);
    if (videoFragment == null) {
      // create a new fragment and add it
      videoFragment = new VideoFragment();
      transaction.add(R.id.videoFrame, videoFragment, VideoFragment.TAG);
    }

    // channel info overlay fragment
    channelInfoFragment = (ChannelInfoFragment) fragmentManager.findFragmentByTag(ChannelInfoFragment.TAG);
    if (channelInfoFragment == null) {
      // create a new fragment and add it
      channelInfoFragment = new ChannelInfoFragment();
      transaction.add(R.id.videoFrame, channelInfoFragment, ChannelInfoFragment.TAG);
    }
    // intially hidden
    transaction.hide(channelInfoFragment);

    // navigation fragment
    navigationFragment = (NavigationFragment) fragmentManager.findFragmentByTag(NavigationFragment.TAG);
    if (navigationFragment == null) {
      // create a new fragment and add it
      navigationFragment = new NavigationFragment();
      transaction.add(R.id.navigationFrame, navigationFragment, NavigationFragment.TAG);
    }
    // intially hidden
    transaction.hide(navigationFragment);

    // check if any navigation fragments are visible, and hide them if they are
    for (NavigationItem item : NavigationItem.values()) {
      if (item.getTag() != null) {
        Fragment fragment = fragmentManager.findFragmentByTag(item.getTag());
        if (fragment != null) {
          // android restored the fragment for us, so hide it
          transaction.hide(fragment);
        }
      }
    }

    transaction.commit();
  }

  @Subscribe
  public void onNavigate(EventBus.NavigationClickedEvent event) {

    if (currentFragment != null && currentFragment.getTag().equals(event.getItem().getTag())) {
      // same item
      return;
    }

    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();

    if (currentFragment != null) {
      transaction.hide(currentFragment);
    }

    NavigationItem item = event.getItem();
    currentFragment = (BaseFragment) fragmentManager.findFragmentByTag(item.getTag());
    if (currentFragment != null) {
      // fragment already exists, just show it
      transaction.show(currentFragment);
    } else {
      // create the fragment
      currentFragment = (BaseFragment) event.getItem().getFragment();
      if (currentFragment != null) {
        transaction.add(R.id.contentFrame, currentFragment, item.getTag());
      }
    }
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.commit();

  }

  private void hideCurrentFragment() {
    // pop the detail fragment
    if (currentFragment != null) {
      FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
      fragmentTransaction.hide(currentFragment);
      fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
      fragmentTransaction.commit();
      currentFragment = null;
      navigationFragment.requestFocus();
    }
  }

  @Subscribe
  public void onChannelChanged(ChannelChangedEvent event) {
    showChannelInfo();
  }

  @Subscribe
  public void onPlayVod(PlayVodEvent event) {
    // hide all UI on VOD playback
    hideUi();
    // then show channel info
    showChannelInfo();
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // send seek events to video fragment
    switch (keyCode) {
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
      case KeyEvent.KEYCODE_MEDIA_REWIND:
      case KeyEvent.KEYCODE_MEDIA_NEXT:
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
      case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
      case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
      case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
      case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
        videoFragment.seek(event);
        return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    boolean handled = false;

    // send seek events to video fragment
    switch (keyCode) {
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
      case KeyEvent.KEYCODE_MEDIA_REWIND:
      case KeyEvent.KEYCODE_MEDIA_NEXT:
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
      case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
      case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
      case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
      case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
        videoFragment.seek(event);
        return true;
    }
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_BACK: {
          if (currentFragment != null) {
            hideCurrentFragment();
            handled = true;
          } else if (isUiVisible()) {
            hideUi();
            handled = true;
          } else {
            // back when navigation fragment focused, confirm quitting the app.
            new AlertDialog.Builder(this).setMessage(R.string.quitConfirmation).setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                finish();
              }
            }).setNegativeButton(R.string.cancel, null).create().show();
            handled = true;
          }
        }
        break;
        case KeyEvent.KEYCODE_CHANNEL_UP:
          channelInfoFragment.nextChannel();
          handled = true;
          break;
        case KeyEvent.KEYCODE_CHANNEL_DOWN:
          channelInfoFragment.previousChannel();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER: {
          if (!navigationFragment.isVisible()) {
            showNavigation();
            handled = true;
          }
        }
        break;
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        case KeyEvent.KEYCODE_MEDIA_PAUSE:
          videoFragment.pause();
          handled = true;
          break;
        case KeyEvent.KEYCODE_MEDIA_STOP:
          videoFragment.stop(true);
          handled = true;
          break;
        case KeyEvent.KEYCODE_MEDIA_PLAY:
          videoFragment.play();
          handled = true;
          break;
        case KeyEvent.KEYCODE_INFO:
          if (channelInfoFragment.isVisible()) {
            hideUi();
          } else {
            showChannelInfo();
          }
          handled = true;
          break;
      }
    }
    return handled ? handled : super.onKeyDown(keyCode, event);
  }

  @Subscribe
  public void onResetUiTimer(EventBus.ResetUiTimerEvent event) {
    resetUiTimer(event.getDelay());
  }

  void resetUiTimer(long ms) {
    handler.removeCallbacks(hideUiRunnable);
    handler.postDelayed(hideUiRunnable, ms);
  }

  @Subscribe
  public void onCancelUiTimer(EventBus.CancelUiTimerEvent event) {
    cancelUiTimer();
  }

  void cancelUiTimer() {
    handler.removeCallbacks(hideUiRunnable);
  }

  boolean isUiVisible() {
    return navigationFragment.isVisible() || channelInfoFragment.isVisible() || (currentFragment != null);
  }

  void hideUi() {
    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.hide(channelInfoFragment);
    transaction.hide(navigationFragment);
    if (currentFragment != null) {
      transaction.hide(currentFragment);
      currentFragment = null;
    }
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.commit();
    handler.removeCallbacks(hideUiRunnable);
  }

  void showNavigation() {
    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
    fragmentTransaction.show(navigationFragment);
    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    fragmentTransaction.commit();
    navigationFragment.requestFocus();
    resetUiTimer(HIDE_UI_TIMEOUT);
  }

  void showChannelInfo() {
    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
    fragmentTransaction.show(channelInfoFragment);
    fragmentTransaction.hide(navigationFragment);
    if (currentFragment != null) {
      fragmentTransaction.hide(currentFragment);
      currentFragment = null;
    }
    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    fragmentTransaction.commit();
    resetUiTimer(HIDE_UI_TIMEOUT);
    channelInfoFragment.requestFocus();
  }
}
