package com.sony.sel.tvapp.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
  private Uri videoUri;
  private MediaPlayer mediaPlayer;
  private SurfaceHolder surfaceHolder;
  private PlayVideoTask playVideoTask;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate view
    View contentView = inflater.inflate(R.layout.video_fragment, null);
    ButterKnife.bind(this, contentView);

    currentChannel = SettingsHelper.getHelper(getActivity()).getCurrentChannel();

    changeChannel();

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stop();
  }

  /**
   * Set up the video playback components.
   * @param uri Video URI. If not null, play this video after setup.
   */
  private void setup(@Nullable final Uri uri) {
    if (surfaceHolder == null) {
      surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          holder.removeCallback(this);
          surfaceHolder = holder;
          play(uri);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
      });
    } else {
      play(uri);
    }
  }

  /**
   * Play a video.
   * @param uri URI of the video to play.
   */
  public void play(@NonNull Uri uri) {
    if (surfaceHolder == null) {
      setup(uri);
      return;
    }
    if (playVideoTask != null) {
      playVideoTask.cancel(true);
    }
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
    playVideoTask = new PlayVideoTask(uri);
    playVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

  }

  /**
   * Play an existing video that is paused.
   */
  public void play() {
    if (mediaPlayer == null) {
      play(videoUri);
    } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
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

  private class PlayVideoTask extends AsyncTask<Void,Void,MediaPlayer> {

    private final Uri uri;

    public PlayVideoTask(Uri uri) {
      this.uri = uri;
    }

    @Override
    protected MediaPlayer doInBackground(Void... params) {
      MediaPlayer mediaPlayer = null;
      try {
        mediaPlayer = MediaPlayer.create(getActivity(), uri);
        mediaPlayer.setDisplay(surfaceHolder);
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
          @Override
          public boolean onError(MediaPlayer mp, int what, int extra) {
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.error)
                .setMessage("Player error " + what + ".")
                .setNeutralButton(getString(android.R.string.ok), null)
                .setPositiveButton(getString(R.string.selectChannelVideos), new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(getActivity(), SelectChannelVideosActivity.class));
                  }
                })
                .create()
                .show();
            return false;
          }
        });
        mediaPlayer.start();
        videoUri = uri;
      } catch (Throwable e) {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.error)
            .setMessage("Error preparing video:" + uri + ": "+e.toString())
            .setNeutralButton(getString(android.R.string.ok), null)
            .setPositiveButton(getString(R.string.selectChannelVideos), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getActivity(), SelectChannelVideosActivity.class));
              }
            })
            .create()
            .show();
      }
      return mediaPlayer;
    }

    @Override
    protected void onCancelled(MediaPlayer mediaPlayer) {
      super.onCancelled(mediaPlayer);
      if (mediaPlayer != null) {
        mediaPlayer.release();
      }
    }

    @Override
    protected void onPostExecute(MediaPlayer mediaPlayer) {
      super.onPostExecute(mediaPlayer);
      VideoFragment.this.mediaPlayer = mediaPlayer;
      playVideoTask = null;
    }
  }
}
