package com.sony.localserver;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.SettingsHelper;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.sony.localserver.WebSocketServer.*;
import static com.sony.localserver.WebSocketServer.TAG;

/**
 * Tests for web socket server.
 */
public class WebSocketServerTest extends InstrumentationTestCase {

  private final String TAG = WebSocketServerTest.class.getSimpleName();

  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;
  private SettingsHelper settingsHelper;
  private WebSocketServer.LocalWebSocket webSocket;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Context context = getInstrumentation().getTargetContext();

    dlnaHelper = DlnaHelper.getHelper(context);
    assertNotNull("DlnaHelper is null.", dlnaHelper);

    dlnaCache = DlnaHelper.getCache(context);
    assertNotNull("DlnaCache is null.", dlnaCache);

    settingsHelper = SettingsHelper.getHelper(context);
    assertNotNull("SettingsHelper is null.", settingsHelper);
    assertNotNull("Server UDN is not defined.", settingsHelper.getEpgServer());

    WebSocketServer server = new WebSocketServer("127.0.0.1", 9000, dlnaHelper, dlnaCache, settingsHelper);
    server.setUdn(settingsHelper.getEpgServer());
    webSocket = (WebSocketServer.LocalWebSocket) server.openWebSocket(new NullSession());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    webSocket = null;
  }

  public void test_searchEpgCache() throws Exception {

    // setup
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Date start = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 1);
    Date end = calendar.getTime();

    List<DlnaObjects.VideoBroadcast> channels = dlnaHelper.getChannels(settingsHelper.getEpgServer(), null);
    assertTrue("No channels returned from server.", channels.size() > 0);
    Log.d(TAG, String.format("%d channels found.", channels.size()));
    List<String> channelIds = new ArrayList<>();
    final int MAX_EPG_CHANNELS = 1;
    for (int i = 0; i < channels.size() && i < MAX_EPG_CHANNELS; i++) {
      DlnaObjects.VideoBroadcast channel = channels.get(i);
      channelIds.add(channel.getChannelId());
    }
    Log.d(TAG, "Searching channels: " + new Gson().toJson(channelIds));

    // given
    List<String> times = new ArrayList<>();
    times.add(String.valueOf(start.getTime()));
    times.add(String.valueOf(end.getTime()));
    SearchEpgCacheRequest request = new SearchEpgCacheRequest(channelIds, times);
    String json = new Gson().toJson(request);
    Log.d(TAG, "Socket request: "+json);

    // when
    long time = System.currentTimeMillis();
    String response = webSocket.searchEpgCache(json);
    time = System.currentTimeMillis() - time;
    Log.d(TAG, "Socket response: "+response);

    // then
    Log.d(TAG, String.format("Response processed in " + time + " ms."));
    assertNotNull("Response was null.", response);
    assertTrue("Response was empty.", response.length() > 0);

  }

  /**
   * Stub session to send to {@link WebSocketServer#openWebSocket(IHTTPSession)}.
   */
  private static class NullSession implements IHTTPSession {
    @Override
    public void execute() throws IOException {

    }

    @Override
    public Map<String, String> getParms() {
      return null;
    }

    @Override
    public Map<String, String> getHeaders() {
      return null;
    }

    @Override
    public String getUri() {
      return null;
    }

    @Override
    public String getQueryParameterString() {
      return null;
    }

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public InputStream getInputStream() {
      return null;
    }

    @Override
    public NanoHTTPD.CookieHandler getCookies() {
      return null;
    }

    @Override
    public void parseBody(Map<String, String> files) throws IOException, ResponseException {

    }
  }
}