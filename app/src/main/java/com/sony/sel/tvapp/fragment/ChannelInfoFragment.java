package com.sony.sel.tvapp.fragment;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.MainActivity;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.ChannelEpgView;
import com.sony.sel.tvapp.view.VodInfoView;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
  @Bind(R.id.vodVideoInfo)
  VodInfoView vodVideoInfo;

  private List<VideoBroadcast> channels = new ArrayList<>();
  private VideoBroadcast currentChannel;
  private VideoProgram currentVod;
  private List<VideoProgram> currentEpg;

  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;
  private SettingsHelper settingsHelper;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());
    dlnaCache = DlnaHelper.getCache(getActivity());
    settingsHelper = SettingsHelper.getHelper(getActivity());

    // inflate view
    View contentView = inflater.inflate(R.layout.channel_info_fragment, null);
    ButterKnife.bind(this, contentView);

    // start fetching channels
    getChannels();

    return contentView;
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
    new GetChannelsTask(settingsHelper.getEpgServer()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Subscribe
  public void onFavoriteChannelsChanged(EventBus.FavoriteChannelsChangedEvent event) {
    // refresh the channel list
    this.channels = dlnaHelper.getChannels(settingsHelper.getEpgServer(), null, true);
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

      if (getActivity().getIntent().getAction() != null && getActivity().getIntent().getAction().equals(MainActivity.INTENT_ACTION_PLAY_VOD)) {
        // play VOD instead of setting channel
        VideoProgram vod = new Gson().fromJson(getActivity().getIntent().getStringExtra(MainActivity.INTENT_EXTRA_VIDEO_ITEM), VideoProgram.class);
        EventBus.getInstance().post(new EventBus.PlayVodEvent(vod));
      } else if (getActivity().getIntent().getAction() != null && getActivity().getIntent().getAction().equals(MainActivity.INTENT_ACTION_VIEW_CHANNEL)) {
        // received intent to set channel, so set it
        VideoBroadcast channel = new Gson().fromJson(getActivity().getIntent().getStringExtra(MainActivity.INTENT_EXTRA_CHANNEL), VideoBroadcast.class);
        SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
      } else {
        // setup the current channel
        VideoBroadcast channel = SettingsHelper.getHelper(getActivity()).getCurrentChannel();
        if (channel == null && channels.size() > 0) {
          // default to first channel
          channel = channels.get(0);
        }
        SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
      }
    }
  }

  @Subscribe
  public void onChannelChanged(EventBus.ChannelChangedEvent event) {
    setCurrentChannel(event.getChannel());
  }

  @Subscribe
  public void onVodVideoPlayback(EventBus.PlayVodEvent event) {
    setVodVideo(event.getVideoProgram());
  }

  public void setCurrentChannel(VideoBroadcast channel) {
    currentChannel = channel;
    currentVod = null;
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Date start = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 7);
    Date end = calendar.getTime();
    List<String> channelId = new ArrayList<>();
    channelId.add(channel.getChannelId());
    currentEpg = dlnaCache.searchEpg(
        settingsHelper.getEpgServer(),
        channelId,
        start,
        end
    );
    updateChannelInfo();
  }

  private void updateChannelInfo() {
    if (currentChannel != null) {
      channelEpgInfo.setVisibility(View.VISIBLE);
      vodVideoInfo.setVisibility(View.GONE);
      channelEpgInfo.bind(currentChannel, currentEpg);
      if (isVisible()) {
        channelEpgInfo.requestFocus();
      }
    }
  }

  private void setVodVideo(VideoProgram vodVideo) {
    currentVod = vodVideo;
    currentChannel = null;
    if (currentVod != null) {
      vodVideoInfo.setVisibility(View.VISIBLE);
      channelEpgInfo.setVisibility(View.GONE);
      vodVideoInfo.bind(vodVideo);
      if (isVisible()) {
        vodVideoInfo.requestFocus();
      }
    }
  }

  public void requestFocus() {
    channelEpgInfo.requestFocus();
  }

  public void nextChannel() {
    if (channels != null && channels.size() > 0) {
      int i = currentChannel != null ? channels.indexOf(currentChannel) + 1 : 0;
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
      int i = currentChannel != null ? channels.indexOf(currentChannel) - 1 : channels.size() - 1;
      if (i < 0) {
        i = channels.size() - 1;
      }
      VideoBroadcast channel = channels.get(i);
      // save new channel to settings
      SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
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
      return dlnaHelper.getChannels(udn, channelObserver, true);
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
