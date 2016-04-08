package com.sony.sel.tvapp.util;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class EventBus extends Bus {

  public static class EpgServerChangedEvent {
    private String epgServer;

    public EpgServerChangedEvent(String epgServer) {
      this.epgServer = epgServer;
    }

    public String getEpgServer() {
      return epgServer;
    }
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