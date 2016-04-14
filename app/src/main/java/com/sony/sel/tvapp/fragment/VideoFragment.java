package com.sony.sel.tvapp.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.ProgramInfoView;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Fragment for displaying video
 */
public class VideoFragment extends BaseFragment {

  public static final String TAG = VideoFragment.class.getSimpleName();

  @Bind(R.id.programInfo)
  ProgramInfoView programInfo;

  @Bind(R.id.videoView)
  VideoView videoView;

  private List<VideoBroadcast> channels = new ArrayList<>();
  private VideoBroadcast currentChannel;
  private Map<String,VideoProgram> currentPrograms = new HashMap<>();

  private GetCurrentProgramsTask epgTask;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.video_fragment, null);

    ButterKnife.bind(this, contentView);

    // keep currentProgram info hidden until loaded
    programInfo.setAlpha(0.0f);

    String videoFile = "file:///sdcard/Movies/tvapp.mp4";
    if (new File(URI.create(videoFile)).exists()) {

      videoView.setVideoURI(Uri.parse(videoFile));
      videoView.start();
    } else {
      new AlertDialog.Builder(getActivity())
          .setTitle("Error")
          .setMessage("Background video file not found at "+videoFile+".")
          .setPositiveButton(getString(android.R.string.ok),null)
          .create()
          .show();
    }

    contentView.requestFocus();
    contentView.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
          switch (keyCode) {
            case KeyEvent.KEYCODE_CHANNEL_UP:
              previousChannel();
              return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
              nextChannel();
              return true;
            case KeyEvent.KEYCODE_BACK:
              if (programInfo.isVisible()) {
                programInfo.hide();
                return true;
              }
          }
        }
        return false;
      }
    });

    // start fetching channels
    getChannels();

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (epgTask != null) {
      // epgTask.cancel(true);
    }
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    getChannels();
  }

  private void getChannels() {
    String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
    new GetChannelsTask(getActivity(), udn) {
      @Override
      protected void onPostExecute(List<VideoBroadcast> channels) {
        super.onPostExecute(channels);
        Log.d(TAG, String.format("%d channels found.", channels.size()));
        setChannels(channels);
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

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
      setCurrentChannel(channel);

      // fetch current programs for all channels
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
    if (currentChannel != null && currentPrograms != null) {
      VideoProgram program = currentPrograms.get(currentChannel.getChannelId());
      programInfo.bind(program, currentChannel);
    }
  }

  public void nextChannel() {
    if (currentChannel != null && channels != null && channels.size() > 0) {
      int i = channels.indexOf(currentChannel) + 1;
      if (i>=channels.size()) {
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
      if (i<0) {
        i = channels.size()-1;
      }
      VideoBroadcast channel = channels.get(i);
      // save new channel to settings
      SettingsHelper.getHelper(getActivity()).setCurrentChannel(channel);
    }
  }

  private class GetCurrentProgramsTask extends AsyncTask<Void,VideoProgram,Void> {

    @Override
    protected Void doInBackground(Void... params) {
      String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
      DlnaHelper dlnaHelper = DlnaHelper.getHelper(getActivity());
      for (VideoBroadcast channel : channels) {
        Log.d(TAG, "Channel: "+channel.toString());
        VideoProgram currentProgram = dlnaHelper.getCurrentEpgProgram(udn,channel);
        if (currentProgram != null) {
          Log.d(TAG, "Current program: "+currentProgram.toString());
          publishProgress(currentProgram);
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
      for (VideoProgram program : values) {
        currentPrograms.put(program.getChannelId(), program);
        if (currentChannel != null && currentChannel.getChannelId().equals(program.getChannelId())) {
          updateChannelInfo();
        }
      }
    }
  }

  private class GetChannelsTask extends AsyncTask<Void,Void,List<VideoBroadcast>> {

    private final Context context;
    private final String udn;

    public GetChannelsTask(Context context, String udn) {
      this.context = context;
      this.udn = udn;
    }

    @Override
    protected List<VideoBroadcast> doInBackground(Void... params) {
      return DlnaHelper.getHelper(context).getChannels(udn);
    }
  }

}
