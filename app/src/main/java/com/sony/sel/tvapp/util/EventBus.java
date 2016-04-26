package com.sony.sel.tvapp.util;

import com.sony.sel.tvapp.ui.NavigationItem;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

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