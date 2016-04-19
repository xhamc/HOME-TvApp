package com.sony.sel.tvapp.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.SelectChannelVideosActivity;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;

/**
 * Fragment for displaying video
 */
public class VideoFragment extends BaseFragment {

  public static final String TAG = VideoFragment.class.getSimpleName();

  @Bind(R.id.surfaceView)
  SurfaceView surfaceView;

  private VideoBroadcast currentChannel;
  private MediaPlayer mediaPlayer;
  private boolean preparing;
  private boolean surfaceCreated;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate view
    View contentView = inflater.inflate(R.layout.video_fragment, null);
    ButterKnife.bind(this, contentView);

    currentChannel = SettingsHelper.getHelper(getActivity()).getCurrentChannel();

    setup();

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stop();
  }

  /**
   * Set up the video playback components.
   */
  private void setup() {
    mediaPlayer = new MediaPlayer();
    if (!surfaceCreated) {
      surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          holder.removeCallback(this);
          mediaPlayer.setDisplay(holder);
          surfaceCreated = true;
          changeChannel();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
      });
    } else {
      mediaPlayer.setDisplay(surfaceView.getHolder());
      changeChannel();
    }
  }

  /**
   * Play a video.
   * @param uri URI of the video to play.
   */
  public void play(Uri uri) {
    if (mediaPlayer == null) {
      setup();
    }
    try {
      if (preparing) {
        mediaPlayer.reset();
      } else if (mediaPlayer.isPlaying()) {
        mediaPlayer.stop();
        mediaPlayer.reset();
      }
      mediaPlayer.setDataSource(getActivity(), uri);
      mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          preparing = false;
          mediaPlayer.start();
        }
      });
      preparing = true;
      mediaPlayer.prepareAsync();
    } catch (IOException e) {
      new AlertDialog.Builder(getActivity())
          .setTitle("Error")
          .setMessage("Error preparing video:" + uri + ".")
          .setPositiveButton(getString(android.R.string.ok), null)
          .create()
          .show();
    }
  }

  /**
   * Play an existing video that is paused.
   */
  public void play() {
    if (mediaPlayer == null) {
      setup();
    } else if (!mediaPlayer.isPlaying()) {
      mediaPlayer.start();
    }
  }

  /**
   * Pause a video that's playing.
   */
  public void pause() {
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    }
  }

  /**
   * Stop video playback & release player resources.
   */
  public void stop() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }

  /**
   * Change channel to a random video stream.
   */
  private void changeChannel() {
    if (mediaPlayer == null) {
      // need to reset
      setup();
      return;
    }
    List<VideoItem> videos = SettingsHelper.getHelper(getActivity()).getChannelVideos();
    if (videos.size() > 0) {
      VideoItem video = videos.get(Math.abs(new Random().nextInt()) % videos.size());
      final String res = video.getResource();
      final String protocolInfo = video.getProtocolInfo();
      if (res != null) {
        play(Uri.parse(res));
      }
    } else {
      new AlertDialog.Builder(getActivity())
          .setTitle(R.string.error)
          .setMessage(R.string.noVideosError)
          .setNeutralButton(getString(R.string.selectVideos), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              startActivity(new Intent(getActivity(), SelectChannelVideosActivity.class));
            }
          })
          .setNegativeButton(getString(android.R.string.cancel), null)
          .create()
          .show();
    }
  }


  @Subscribe
  public void onChannelChanged(EventBus.ChannelChangedEvent event) {
    currentChannel = event.getChannel();
    changeChannel();
  }

}
