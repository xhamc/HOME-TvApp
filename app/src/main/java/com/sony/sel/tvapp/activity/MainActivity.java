package com.sony.sel.tvapp.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

import com.google.gson.Gson;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.BaseFragment;
import com.sony.sel.tvapp.fragment.ChannelInfoFragment;
import com.sony.sel.tvapp.fragment.NavigationFragment;
import com.sony.sel.tvapp.fragment.SearchFragment;
import com.sony.sel.tvapp.fragment.VideoFragment;
import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.EventBus.ChannelChangedEvent;
import com.sony.sel.tvapp.util.EventBus.PlayVodEvent;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * Main Activity
 */
public class MainActivity extends BaseActivity {

  public static final String TAG = MainActivity.class.getSimpleName();

  // intent action & extra for playing VOD
  public static final String INTENT_ACTION_PLAY_VOD = "com.sony.sel.tvapp.PLAY_VOD";
  public static final String INTENT_EXTRA_VOD_ITEM = "VideoProgram";

  // intent action & extra for viewing a channel
  public static final String INTENT_ACTION_VIEW_CHANNEL = "com.sony.sel.tvapp.VIEW_CHANNEL";
  public static final String INTENT_EXTRA_CHANNEL = "VideoBroadcast";

  private VideoFragment videoFragment;
  private ChannelInfoFragment channelInfoFragment;
  private BaseFragment currentFragment;
  private NavigationFragment navigationFragment;

  /// short timeout for hiding UI after quick interactions
  public static final long HIDE_UI_TIMEOUT = 5000;

  /// long timeout allows time for more viewing/longer processes but still times out eventually
  public static final long HIDE_UI_TIMEOUT_LONG = 30000;

  // request code for speech recognition
  private static final int RECOGNIZE_SPEECH = 7893;

  private SettingsHelper settingsHelper;

  private final Handler handler = new Handler();
  private Runnable hideUiRunnable = new Runnable() {
    @Override
    public void run() {
      hideUi();
    }
  };

  @Override
  protected void onPause() {
    super.onPause();
    hideUi();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (INTENT_ACTION_PLAY_VOD.equals(intent.getAction())) {
      VideoProgram vod = new Gson().fromJson(intent.getStringExtra(INTENT_EXTRA_VOD_ITEM), VideoProgram.class);
      EventBus.getInstance().post(new PlayVodEvent(vod));
    }
  }

  @Override
  public boolean onSearchRequested() {
    startSpeechRecognition();
    return true;
  }

  private void startSpeechRecognition() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voicePromptText));
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startActivityForResult(intent, RECOGNIZE_SPEECH);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult(): requestCode = "+requestCode+", resultCode = "+resultCode+", data = "+data);
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RECOGNIZE_SPEECH && resultCode == RESULT_OK) {
      ArrayList<String> results = data.getStringArrayListExtra(
          RecognizerIntent.EXTRA_RESULTS);
      if (results.size() > 0) {
        String query = results.get(0);
        settingsHelper.setCurrentSearchQuery(query);
      }
      showNavigation();
      onNavigate(new EventBus.NavigationClickedEvent(NavigationItem.SEARCH));
    }
  }

  @Override
  public void onVisibleBehindCanceled() {
    super.onVisibleBehindCanceled();
    videoFragment.onVisibleBehindCanceled();
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
