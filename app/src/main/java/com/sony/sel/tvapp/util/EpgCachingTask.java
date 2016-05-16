package com.sony.sel.tvapp.util;

import android.os.AsyncTask;
import android.util.Log;

import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Background task for populating the EPG cache.
 */
public class EpgCachingTask extends AsyncTask<Void, Void, Void> {

  private final String TAG = EpgCachingTask.class.getSimpleName();

  private final DlnaInterface dlnaHelper;
  private final DlnaCache dlnaCache;
  private final String udn;

  public EpgCachingTask(DlnaInterface dlnaHelper, DlnaCache dlnaCache, String udn) {
    this.dlnaHelper = dlnaHelper;
    this.dlnaCache = dlnaCache;
    this.udn = udn;
  }

  @Override
  protected Void doInBackground(Void... params) {

    if (isCancelled()) {
      // task was cancelled, bail out
      return null;
    }

    Log.d(TAG, "Start caching EPG for server " + udn + ".");

    List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null);
    if (channels.size() == 0) {
      Log.e(TAG, "No channels found.");
      return null;
    }
    Log.d(TAG, String.format("%d channels found.", channels.size()));
    List<String> channelIds = new ArrayList<>();
    for (VideoBroadcast channel : channels) {
      channelIds.add(channel.getChannelId());
    }

    if (isCancelled()) {
      // task was cancelled, bail out
      return null;
    }

    // start calendar at current date/time
    Calendar calendar = Calendar.getInstance();

    // cache today's programs
    int programCount = cachePrograms(calendar.getTime(), channelIds);
    if (programCount == 0) {
      Log.e(TAG, "No EPG data found.");
      return null;
    } else if (isCancelled()) {
      return null;
    }

    // cache yesterday's programs
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    programCount = cachePrograms(calendar.getTime(), channelIds);
    if (programCount == 0) {
      Log.e(TAG, "No EPG data found.");
      return null;
    } else if (isCancelled()) {
      return null;
    }

    // cache ahead
    calendar.add(Calendar.DAY_OF_MONTH, 2);
    do {
      programCount = cachePrograms(calendar.getTime(), channelIds);
      calendar.add(Calendar.DAY_OF_MONTH, 1);
    } while (programCount > 0 && !isCancelled());

    return null;
  }

  /**
   * Cache EPG programs for a date and set of channels.
   *
   * @param date       Date to cache.
   * @param channelIds Channel IDs to cache.
   * @return Total number of items cached (or found already cached).
   */
  private int cachePrograms(Date date, List<String> channelIds) {
    final DateFormat format = new SimpleDateFormat("M-d");

    DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    Log.d(TAG, "Cache programs for " + isoFormat.format(date) + ".");

    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date start = calendar.getTime();
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MILLISECOND, -1);
    Date end = calendar.getTime();

    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    Log.d(TAG, "Searching cache from " + isoFormat.format(start) + " to " + isoFormat.format(end) + ".");

    int programCount = 0;

    for (String channelId : channelIds) {

      // convert to UTC before parsing out date
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      final String day = format.format(start);

      List<String> idList = new ArrayList<>();
      idList.add(channelId);

      Log.d(TAG, "Caching EPG data for channel " + channelId + " " + day + ".");

      // get container
      String parentId = "0/EPG/" + channelId + "/" + day;

      // get EPG data from server
      List<VideoProgram> programs = dlnaHelper.getChildren(udn, parentId, VideoProgram.class, null, false);
      programCount += programs.size();

      if (isCancelled()) {
        return programCount;
      }

    }
    return programCount;
  }

  @Override
  protected void onCancelled(Void aVoid) {
    super.onCancelled(aVoid);
    Log.w(TAG, "Caching EPG cancelled for server " + udn + ".");

  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
    Log.d(TAG, "Done caching EPG for server " + udn + ".");
  }
}
