package com.sony.sel.tvapp.fragment;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.ChannelEpgView;
import com.sony.sel.tvapp.view.ProgramInfoView;
import com.squareup.otto.Subscribe;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Fragment for displaying channel info and receiving channel change key presses
 */
public class ChannelInfoFragment extends BaseFragment {

  public static final String TAG = ChannelInfoFragment.class.getSimpleName();

  @Bind(R.id.channelEpgInfo)
  ChannelEpgView channelEpgInfo;

  private List<VideoBroadcast> channels = new ArrayList<>();
  private VideoBroadcast currentChannel;
  private Map<String, List<VideoProgram>> currentPrograms = new HashMap<>();

  private GetCurrentProgramsTask epgTask;

  private DlnaInterface dlnaHelper;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

    // inflate view
    View contentView = inflater.inflate(R.layout.channel_info_fragment, null);
    ButterKnife.bind(this, contentView);

    // start fetching channels
    getChannels();

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (epgTask != null) {
      // cancel EPG task if running
      epgTask.cancel(true);
      // stop listening for channel list changes
      dlnaHelper.unregisterContentObserver(channelObserver);
    }
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    // refresh the channel list when the server changes
    getChannels();
  }

  /**
   * Fetch the list of channels
   */
  private void getChannels() {
    String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
    new GetChannelsTask(udn).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Subscribe
  public void onFavoriteChannelsChanged(EventBus.FavoriteChannelsChangedEvent event) {
    // refresh the channel list
    this.channels = dlnaHelper.getChannels(SettingsHelper.getHelper(getActivity()).getEpgServer(),null);
  }

  /**
   * Refresh the list of channels after loading.
   *
   * @param channels New channel list.
   */
  private void setChannels(List<VideoBroadcast> channels) {
    if (channels != null) {
      // set channel grid
      this.channels = channels;

      // setup current channel
      VideoBroadcast channel = SettingsHelper.getHelper(getActivity()).getCurrentChannel();
      if (channel == null && channels.size() > 0) {
        // default to first channel
        channel = channels.get(0);
      }
      EventBus.getInstance().post(new EventBus.ChannelChangedEvent(channel));

      // fetch current programs for all channels
      if (epgTask != null) {
        // cancel existing EPG task
        epgTask.cancel(true);
      }
      epgTask = new GetCurrentProgramsTask();
      epgTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  @Subscribe
  public void onChannelChanged(EventBus.ChannelChangedEvent event) {
    setCurrentChannel(event.getChannel());
  }

  public void setCurrentChannel(VideoBroadcast channel) {
    currentChannel = channel;
    updateChannelInfo();
  }

  private void updateChannelInfo() {
    if (currentChannel != null) {
      channelEpgInfo.bind(currentChannel, currentPrograms.get(currentChannel.getChannelId()));
    }
  }

  public void requestFocus() {
    channelEpgInfo.requestFocus();
  }

  private VideoProgram getCurrentProgram() {
    Date now = new Date();
    List<VideoProgram> programs = currentPrograms.get(currentChannel.getChannelId());
    if (programs != null) {
      for (VideoProgram program : programs) {
        if (program.getScheduledStartTime().before(now) && program.getScheduledEndTime().after(now)) {
          return program;
        }
      }
    }
    // not found
    return null;
  }

  public void nextChannel() {
    if (currentChannel != null && channels != null && channels.size() > 0) {
      int i = channels.indexOf(currentChannel) + 1;
      if (i >= channels.size()) {
        i = 0;
      }
      VideoBroadcast channel = channels.get(i);
      // save new channel to settings
      SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
    }
  }

  public void previousChannel() {
    if (currentChannel != null && channels != null && channels.size() > 0) {
      int i = channels.indexOf(currentChannel) - 1;
      if (i < 0) {
        i = channels.size() - 1;
      }
      VideoBroadcast channel = channels.get(i);
      // save new channel to settings
      SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
    }
  }

  private class GetCurrentProgramsTask extends AsyncTask<Void, VideoProgram, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.HOUR_OF_DAY, -12);
      Date start = calendar.getTime();
      calendar.add(Calendar.HOUR_OF_DAY, 24);
      Date end = calendar.getTime();
      for (VideoBroadcast channel : channels) {
        Log.d(TAG, "Get EPG for channel " + channel.getCallSign() + ".");
        List<VideoProgram> epgData = dlnaHelper.getEpgPrograms(udn, channel, start, end);
        if (epgData != null && epgData.size() > 0) {
          Log.d(TAG, String.format("%d programs found.", epgData.size()));
          VideoProgram[] programs = new VideoProgram[epgData.size()];
          publishProgress(epgData.toArray(programs));
        } else {
          Log.e(TAG, "No current program found.");
        }
        if (isCancelled()) {
          Log.w(TAG, "Canceling EPG task.");
          break;
        }
      }
      return null;
    }

    @Override
    protected void onProgressUpdate(VideoProgram... values) {
      super.onProgressUpdate(values);
      List<VideoProgram> programs = Arrays.asList(values);
      currentPrograms.put(values[0].getChannelId(), programs);
      if (currentChannel != null && currentChannel.getChannelId().equals(values[0].getChannelId())) {
        updateChannelInfo();
      }
    }
  }

  /**
   * Observer to receive notification of changes to the channel list.
   */
  private ContentObserver channelObserver = new ContentObserver(null) {
    @Override
    public void onChange(boolean selfChange, Uri uri) {
      super.onChange(selfChange, uri);
      // reload channels on content changes
      Log.d(TAG, "Received notification channel list changed.");
      getChannels();
    }
  };

  /**
   * Async task to load channel list.
   */
  private class GetChannelsTask extends AsyncTask<Void, Void, List<VideoBroadcast>> {

    private final String udn;

    public GetChannelsTask(String udn) {
      this.udn = udn;
    }

    @Override
    protected List<VideoBroadcast> doInBackground(Void... params) {
      // get channels and register observer for future changes
      return dlnaHelper.getChannels(udn, channelObserver);
    }

    @Override
    protected void onPostExecute(List<VideoBroadcast> channels) {
      super.onPostExecute(channels);
      Log.d(TAG, String.format("%d channels found.", channels.size()));
      if (channels.size() > 0) {
        setChannels(channels);
      } else {
        // try again after a delay
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            new GetChannelsTask(udn).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
          }
        }, 1000);
      }
    }
  }

}
