package com.sony.sel.tvapp;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Tests for DLNA helper & cache.
 */
public class DlnaTest extends InstrumentationTestCase {

  private static final String TAG = DlnaTest.class.getSimpleName();

  public void test_epgCache() {

    Context context = getInstrumentation().getTargetContext();
    DlnaInterface dlnaHelper = DlnaHelper.getHelper(context);
    DlnaCache dlnaCache = DlnaHelper.getCache(context);
    String udn = SettingsHelper.getHelper(context).getEpgServer();
    assertNotNull("Server UDN is not defined.", udn);
    Log.d(TAG, "Server UDN: " + udn + ".");

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, -12);
    Date start = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 24);
    Date end = calendar.getTime();

    List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);
    assertTrue("No channels returned from server.", channels.size() > 0);
    Log.d(TAG, String.format("%d channels found.", channels.size()));

    List<String> channelIds = new ArrayList<>();
    List<VideoBroadcast> epgChannels = new ArrayList<>();
    final int MAX_EPG_CHANNELS = 6;
    for (int i = 0; i < channels.size() && i < MAX_EPG_CHANNELS; i++) {
      VideoBroadcast channel = channels.get(i);
      channelIds.add(channel.getChannelId());
      epgChannels.add(channel);
    }
    Log.d(TAG, "Searching channels: " + new Gson().toJson(channelIds));

    long time = System.currentTimeMillis();
    List<VideoProgram> epgData = dlnaCache.searchEpg(udn, channelIds, start, end);
    time = System.currentTimeMillis() - time;
    Log.d(TAG, String.format("EPG cache returned %d matching programs in %d ms.", epgData.size(), time));

    time = System.currentTimeMillis();
    epgData.clear();
    for (VideoBroadcast channel : epgChannels) {
      epgData.addAll(dlnaHelper.getEpgPrograms(udn, channel, start, end));
    }
    time = System.currentTimeMillis() - time;
    Log.d(TAG, String.format("EPG normal search returned %d matching programs in %d ms.", epgData.size(), time));

  }
}
