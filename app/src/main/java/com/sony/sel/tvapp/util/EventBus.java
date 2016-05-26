package com.sony.sel.tvapp.util;

import com.sony.sel.tvapp.activity.MainActivity;
import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.util.Set;

public class EventBus extends Bus {

  public static class EpgServerChangedEvent {
    private final String serverUdn;

    public EpgServerChangedEvent(String serverUdn) {
      this.serverUdn = serverUdn;
    }

    public String getServerUdn() {
      return serverUdn;
    }
  }

  public static class NavigationFocusedEvent {
    private final NavigationItem item;

    public NavigationFocusedEvent(NavigationItem item) {
      this.item = item;
    }

    public NavigationItem getItem() {
      return item;
    }
  }

  public static class NavigationClickedEvent {
    private final NavigationItem item;

    public NavigationClickedEvent(NavigationItem item) {
      this.item = item;
    }

    public NavigationItem getItem() {
      return item;
    }
  }

  public static class ChannelChangedEvent {
    private final DlnaObjects.VideoBroadcast channel;

    public ChannelChangedEvent(DlnaObjects.VideoBroadcast channel) {
      this.channel = channel;
    }

    public DlnaObjects.VideoBroadcast getChannel() {
      return channel;
    }
  }

  /**
   * Event to tell the UI timer to stop waiting to hide the UI.
   */
  public static class CancelUiTimerEvent {

  }

  public static class ResetUiTimerEvent {
    private final long delay;

    public ResetUiTimerEvent(long delay) {
      this.delay = delay;
    }

    public long getDelay() {
      return delay;
    }
  }

  /**
   * Reset the UI timer to keep the UI visible for a standard (short) interval.
   */
  public static class ResetUiTimerShortEvent extends ResetUiTimerEvent {
    public ResetUiTimerShortEvent() {
      super(MainActivity.HIDE_UI_TIMEOUT);
    }
  }

  /**
   * Set the UI "long timer" to keep the UI visible for a longer interval.
   */
  public static class ResetUiTimerLongEvent extends ResetUiTimerEvent {

    public ResetUiTimerLongEvent() {
      super(MainActivity.HIDE_UI_TIMEOUT_LONG);
    }
  }

  public static class FavoriteChannelsChangedEvent {
    private final Set<String> favoriteChannels;

    public FavoriteChannelsChangedEvent(Set<String> favoriteChannels) {
      this.favoriteChannels = favoriteChannels;
    }

    public Set<String> getFavoriteChannels() {
      return favoriteChannels;
    }
  }

  public static class SendBackKeyToEpgEvent {
    private final boolean shouldSend;

    public SendBackKeyToEpgEvent(boolean shouldSend) {
      this.shouldSend = shouldSend;
    }

    public boolean shouldSend() {
      return shouldSend;
    }
  }

  public static class RecordingsChangedEvent {

  }

  public static class PlayVodEvent {
    private final VideoProgram videoProgram;

    public PlayVodEvent(VideoProgram videoProgram) {
      this.videoProgram = videoProgram;
    }

    public VideoProgram getVideoProgram() {
      return videoProgram;
    }
  }

  public static class FavoriteProgramsChangedEvent {

  }

  private static EventBus instance;

  public static EventBus getInstance() {
    if (instance == null) {
      instance = new EventBus(ThreadEnforcer.ANY);
    }
    return instance;
  }

  private EventBus(ThreadEnforcer enforcer) {
    super(enforcer);
  }

}