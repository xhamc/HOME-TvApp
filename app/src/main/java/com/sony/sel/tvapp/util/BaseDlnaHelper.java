package com.sony.sel.tvapp.util;

import android.database.ContentObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Base helper functionality independent of implementation.
 */
public abstract class BaseDlnaHelper implements DlnaInterface {

  @NonNull
  @Override
  public List<DlnaObjects.VideoBroadcast> getChannels(@NonNull String udn, @Nullable ContentObserver contentObserver) {
    return getChildren(udn, "0/Channels", DlnaObjects.VideoBroadcast.class, contentObserver, false);
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
    calendar.add(Calendar.DAY_OF_MONTH, - 1);
    for (calendar.setTime(startDate); calendar.getTimeInMillis() < endDate.getTime()+ONE_DAY_MS; calendar.add(Calendar.DAY_OF_MONTH,1)) {
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

}
