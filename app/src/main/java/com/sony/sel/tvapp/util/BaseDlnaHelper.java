package com.sony.sel.tvapp.util;

import android.content.Context;
import android.database.ContentObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base helper functionality independent of implementation.
 */
public abstract class BaseDlnaHelper implements DlnaInterface {

  private final Context context;
  private ContentObserver channelObserver;

  public BaseDlnaHelper(Context context) {
    this.context = context.getApplicationContext();
  }

  protected Context getContext() {
    return context;
  }

  @NonNull
  @Override
  public List<DlnaObjects.VideoBroadcast> getChannels(@NonNull String udn, @Nullable ContentObserver contentObserver) {
    channelObserver = contentObserver;
    List<DlnaObjects.VideoBroadcast> channels = getChildren(udn, "0/Channels", DlnaObjects.VideoBroadcast.class, contentObserver, false);
    final Set<String> favoriteChannels = SettingsHelper.getHelper(getContext()).getFavoriteChannels();
    Collections.sort(channels, new Comparator<DlnaObjects.VideoBroadcast>() {
      @Override
      public int compare(DlnaObjects.VideoBroadcast lhs, DlnaObjects.VideoBroadcast rhs) {
        if (favoriteChannels.contains(lhs.getChannelId()) != favoriteChannels.contains(rhs.getChannelId())) {
          // favorite channel is sorted before non-favorite
          return favoriteChannels.contains(lhs.getChannelId()) ? -1 : 1;
        }
        // sort by channel number
        int lnum = Integer.valueOf(lhs.getChannelNumber());
        int rnum = Integer.valueOf(rhs.getChannelNumber());
        return lnum - rnum;
      }
    });
    return channels;
  }

  @Nullable
  @Override
  public DlnaObjects.VideoProgram getCurrentEpgProgram(String udn, DlnaObjects.VideoBroadcast channel) {
    DateFormat format = new SimpleDateFormat("M-d");
    Date now = new Date();
    String day = format.format(now);
    List<DlnaObjects.VideoProgram> shows = getChildren(udn, "0/EPG/" + channel.getChannelId() + "/" + day, DlnaObjects.VideoProgram.class, null, true);
    for (DlnaObjects.VideoProgram show : shows) {
      if (show.getScheduledStartTime().before(now) && show.getScheduledEndTime().after(now)) {
        // show was found
        return show;
      }
    }
    return null;
  }

  @NonNull
  @Override
  public List<DlnaObjects.VideoProgram> getEpgPrograms(String udn, DlnaObjects.VideoBroadcast channel, Date startDate, Date endDate) {
    List<DlnaObjects.VideoProgram> shows = new ArrayList<>();
    DateFormat format = new SimpleDateFormat("M-d");
    // use a Calendar to iterate
    // we need to expand the date range at both ends by 24 hours to account for all possible time zones
    long ONE_DAY_MS = 1000 * 60 * 60 * 24;
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    for (calendar.setTime(startDate); calendar.getTimeInMillis() < endDate.getTime() + ONE_DAY_MS; calendar.add(Calendar.DAY_OF_MONTH, 1)) {
      String day = format.format(calendar.getTime());
      List<DlnaObjects.VideoProgram> dayShows = getChildren(udn, "0/EPG/" + channel.getChannelId() + "/" + day, DlnaObjects.VideoProgram.class, null, true);
      for (DlnaObjects.VideoProgram show : dayShows) {
        if (show.getScheduledStartTime().before(endDate) && show.getScheduledEndTime().after(startDate)) {
          // matching date/time range, add to results
          shows.add(show);
        }
      }
    }
    return shows;
  }

  @Override
  public void setFavoriteChannels(Set<String> favoriteChannels) {
    if (channelObserver != null) {
      channelObserver.dispatchChange(false, null);
    }
  }
}
