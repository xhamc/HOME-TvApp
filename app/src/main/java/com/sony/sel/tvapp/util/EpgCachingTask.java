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
    boolean cached = cachePrograms(calendar.getTime(), channelIds);
    if (!cached || isCancelled()) {
      return null;
    }

    // cache yesterday's programs
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    cached = cachePrograms(calendar.getTime(), channelIds);
    if (!cached || isCancelled()) {
      return null;
    }

    // cache ahead
    calendar.add(Calendar.DAY_OF_MONTH, 2);
    do {
      cached = cachePrograms(calendar.getTime(), channelIds);
      calendar.add(Calendar.DAY_OF_MONTH, 1);
    } while (cached && !isCancelled());

    return null;
  }

  /**
   * Cache EPG programs for a date and set of channels.
   *
   * @param date       Date to cache.
   * @param channelIds Channel IDs to cache.
   * @return true if items were cached, false if no EPG items are available for the channels/date requested.
   */
  private boolean cachePrograms(Date date, List<String> channelIds) {
    final DateFormat format = new SimpleDateFormat("M-d");

    DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    Log.d(TAG, "Cache programs for local date " + isoFormat.format(date) + ".");

    // create UTC dates for start & end of day
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

    // convert format to UTC before parsing the day string
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    final String day = format.format(start);
    Log.d(TAG, "UTC date is " + day + ".");

    // search the cache for programs in the EPG right before midnight UTC
    // if they are found for all channels we assume that the entire day has been cached
    int cacheCount = dlnaCache.countEpgItems(
        udn,
        channelIds,
        end,
        end
    );
    if (cacheCount == channelIds.size()) {
      Log.d(TAG, "EPG data already cached for " + day + ".");
      return true;
    }

    // need to cache EPG for this day
    int programCount = 0;
    for (String channelId : channelIds) {
      Log.d(TAG, "Caching EPG data for channel " + channelId + " " + day + ".");

      // get container ID for channel & day
      String parentId = "0/EPG/" + channelId + "/" + day;

      // get EPG data from server
      List<VideoProgram> programs = dlnaHelper.getChildren(udn, parentId, VideoProgram.class, null, false);
      programCount += programs.size();

      if (isCancelled()) {
        return programCount > 0;
      }
    }
    return programCount > 0;
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
