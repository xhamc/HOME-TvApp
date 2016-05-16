package com.sony.sel.tvapp.fragment;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.MainActivity;
import com.sony.sel.tvapp.activity.SelectChannelVideosActivity;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.EventBus.ChannelChangedEvent;
import com.sony.sel.tvapp.util.EventBus.PlayVodEvent;
import com.sony.sel.tvapp.util.PrepareVideoTask;
import com.sony.sel.tvapp.util.ProtocolInfo;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.MediaProgressView;
import com.sony.sel.tvapp.view.MediaProgressView.ProgressInfo;
import com.sony.sel.tvapp.view.SpinnerView;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Fragment for displaying video
 */
public class VideoFragment extends BaseFragment {

  public static final String TAG = VideoFragment.class.getSimpleName();


  private Method mediaPlayerInvoke = null;
  private Method setSpeedMethod = null;
  private ProtocolInfo protocolInfo;

  @Bind(R.id.videoSurfaceView)
  SurfaceView surfaceView;
  @Bind(R.id.spinner)
  SpinnerView spinner;
  @Bind(R.id.mediaProgress)
  MediaProgressView mediaProgress;

  private VideoBroadcast currentChannel;
  private VideoProgram currentProgram;
  private VideoItem currentVod;
  private Double currentPlaySpeed = 1.0;
  private boolean wasPlaying;

  private Uri videoUri;
  private MediaPlayer mediaPlayer;
  private SurfaceHolder surfaceHolder;
  private PlayVideoTask playVideoTask;
  private MediaSession mediaSession;
  private Bitmap mediaArtwork;

  private final long PREPARE_DLNA_VIDEO_TIMEOUT = 30000;
  private final long PREPARE_VIDEO_TIMEOUT = 60000;
  private final long CHANNEL_START_DELAY = 500;
  private final long PROGRESS_UI_HIDE_DELAY = 10000;

  private Handler handler = new Handler();
  private Runnable channelChangeRunnable;
  private Runnable mediaProgressRunnable = new Runnable() {
    @Override
    public void run() {
      updateProgressBar();
    }
  };
  private Runnable hideProgressRunnable = new Runnable() {
    @Override
    public void run() {
      hideProgressBar();
    }
  };
  private Runnable showSpinnerRunnable = new Runnable() {
    @Override
    public void run() {
      spinner.show();
    }
  };

  private long seekPosition = -1;

  // values for setting playback speed
  private final Double[] fixedSpeeds = {-16.0, -4.0, -2.0, -1.0, -0.5, 0.5, 1.0, 2.0, 4.0, 16.0};
  private final static int INVOKE_ID_SET_SPEED_FLOAT = 100;
  private final static String IMEDIA_PLAYER = "android.media.IMediaPlayer";

  private SettingsHelper settingsHelper;
  private DlnaInterface dlnaHelper;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate view
    View contentView = inflater.inflate(R.layout.video_fragment, null);
    ButterKnife.bind(this, contentView);

    settingsHelper = SettingsHelper.getHelper(getActivity());
    dlnaHelper = DlnaHelper.getHelper(getActivity());

    // create media session
    createMediaSession();

    // retrieve invoke method for setting playback speed
    getInvokeMethod();

    return contentView;
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mediaPlayer != null) {
      // Argument equals true to notify the system that the activity
      // wishes to be visible behind other translucent activities
      if (!getActivity().requestVisibleBehind(true)) {
        // App-specific method to stop playback and release resources
        // because call to requestVisibleBehind(true) failed
        stop();
      }
    } else {
      // Argument equals false because the activity is not playing
      getActivity().requestVisibleBehind(false);
    }
  }

  public void onVisibleBehindCanceled() {
    stop();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (wasPlaying && videoUri != null) {
      // resume play
      play(videoUri);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stop();
    releaseMediaSession();
  }


  /**
   * Retrieve the media player's invoke and setSpeed methods if they are available
   * so we can set playback speed.
   */
  private void getInvokeMethod() {
    try {
      mediaPlayerInvoke = MediaPlayer.class.getMethod("invoke", new Class[]{Parcel.class, Parcel.class});
    } catch (Exception e) {
      Log.e(TAG, "Class method not supported :" + e.getMessage());
    }
    try {
      setSpeedMethod = MediaPlayer.class.getMethod("setSpeed", new Class[]{float.class});
    } catch (Exception e) {
      Log.e(TAG, "Class method not supported :" + e.getMessage());
    }
  }

  /**
   * Set the playback speed.
   *
   * @param speed The speed multiple.
   * @return Response from the player's speed setting method.
   */
  private int setPlaySpeed(Double speed) {
    Parcel request = Parcel.obtain();
    Parcel reply = Parcel.obtain();
    try {
      request.writeInterfaceToken(IMEDIA_PLAYER);
      request.writeInt(INVOKE_ID_SET_SPEED_FLOAT);
      request.writeFloat(speed.floatValue());
      mediaPlayerInvoke.invoke(mediaPlayer, request, reply);
      Log.d(TAG, "Set speed to " + speed + ", response: " + reply.readInt());
      return reply.readInt();
    } catch (Exception e) {
      Log.e(TAG, "Error setting speed to " + speed + ": " + e);
    } finally {
      request.recycle();
      reply.recycle();
    }
    return 0;
  }

  /**
   * Set up the video playback surface.
   *
   * @param uri Video URI. If not null, play this video after setup.
   */
  private void setup(@Nullable final Uri uri) {
    if (surfaceHolder == null) {
      if (surfaceView.getHolder().getSurface() != null) {
        // surface is ready, just need to get the holder
        surfaceHolder = surfaceView.getHolder();
        play(uri);
      } else {
        // surface is not ready, listen for surface creation
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
          @Override
          public void surfaceCreated(SurfaceHolder holder) {
            // surface is ready, now we can play
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
      }
    } else {
      // just initiate playback
      play(uri);
    }
  }

  /**
   * Play a video.
   *
   * @param uri URI of the video to play.
   */
  public void play(@NonNull Uri uri) {
    if (surfaceHolder == null) {
      // need to set up the surface
      setup(uri);
      return;
    }
    stop();
    if (uri != null) {
      showSpinner();
      // create & execute async task for video playback
      playVideoTask = new PlayDlnaVideoTask(getActivity(), uri);
      playVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  /**
   * Restart playback of an existing video that is paused or stopped.
   */
  public void play() {
    showProgressBar(PROGRESS_UI_HIDE_DELAY);
    seekPosition = -1;
    if (mediaPlayer == null && videoUri != null) {
      // restart playback from scratch
      play(videoUri);
    } else if (mediaPlayer != null) {
      // resume play
      Log.d(TAG, "Resuming video playback.");
      mediaPlayer.start();
      resetPlaySpeed();
      updateMediaPlaybackState();
    }
  }

  /**
   * Pause a video that's playing.
   */
  public void pause() {
    if (canPause()) {
      Log.d(TAG, "Pausing video.");
      mediaPlayer.pause();
      updateMediaPlaybackState();
      showProgressBar(PROGRESS_UI_HIDE_DELAY);
    }
  }

  /**
   * Stop video playback & release player resources.
   */
  public void stop() {
    if (mediaPlayer != null) {
      Log.d(TAG, "Stopping and releasing video.");
      wasPlaying = mediaPlayer.isPlaying();
      settingsHelper.saveVideoPosition(videoUri.toString(), mediaPlayer.getCurrentPosition());
      mediaPlayer.release();
      mediaPlayer = null;
      updateMediaPlaybackState();
      hideProgressBar();
      stopProgressUpdates();
      resetPlaySpeed();
      seekPosition = -1;
    }
    if (playVideoTask != null) {
      // cancel a playback task in progress
      playVideoTask.cancel(true);
      playVideoTask = null;
      hideSpinner();
    }
  }

  /**
   * Seek to a specific playback position.
   *
   * @param position Position to seek to.
   */
  private void seekTo(int position, final boolean playAfterSeek) {
    // show spinner if seek takes a while
    showSpinner();
    // set player to seek
    mediaPlayer.seekTo(position);
    // clear any saved seek position
    seekPosition = -1;
    // completion listener
    mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
      @Override
      public void onSeekComplete(MediaPlayer mp) {
        hideSpinner();
        showProgressBar(PROGRESS_UI_HIDE_DELAY);
        startProgressUpdates();
        if (playAfterSeek) {
          mediaPlayer.start();
        }
      }
    });
  }


  /**
   * Process keyboard events for media seek. When seek buttons are held down, the
   * seek location moves in the UI. After buttons are released, the actual seek is initiated.
   *
   * @param keyEvent Key events to process.
   */
  public void seek(KeyEvent keyEvent) {
    if (canSeek()) {
      if (seekPosition < 0) {
        seekPosition = mediaPlayer.getCurrentPosition();
      }
      switch (keyEvent.getAction()) {
        case KeyEvent.ACTION_DOWN:
        case KeyEvent.ACTION_MULTIPLE:
          stopProgressUpdates();
          showProgressBar(PROGRESS_UI_HIDE_DELAY);
          switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
              // move the seek position forward without actually seeking
              seekPosition += mediaPlayer.getDuration() / 100;
              seekPosition = Math.max(0, Math.min(seekPosition, mediaPlayer.getDuration()));
              mediaProgress.setProgress(new Date(mediaProgress.getData().getStartTime().getTime() + seekPosition));
              break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
              // move the seek position back without actually seeking
              seekPosition -= mediaPlayer.getDuration() / 100;
              seekPosition = Math.max(0, Math.min(seekPosition, mediaPlayer.getDuration()));
              mediaProgress.setProgress(new Date(mediaProgress.getData().getStartTime().getTime() + seekPosition));
              break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
              // bump up and/or loop the forward play speed
              increasePlaySpeed();
              break;
            }
            case KeyEvent.KEYCODE_MEDIA_REWIND: {
              // bump up and/or loop the reverse play speed
              increasePlaySpeedReverse();
              break;
            }
          }
          break;
        case KeyEvent.ACTION_UP:
          switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
              // initiate a seek to the current position on key release
              seekTo((int) (seekPosition - mediaProgress.getData().getStartTime().getTime()), false);
              break;
          }
      }
    }
  }

  /**
   * Increase the playback speed to the next increment in the forward direction, and update the UI.
   */

  void increasePlaySpeed() {
    List<Double> ps = new ArrayList<>();
    if (protocolInfo != null && protocolInfo.playSpeedSupported()) {
      ps = protocolInfo.getPlaySpeedDoubles();

    } else {
      ps.addAll(Arrays.asList(fixedSpeeds));
    }
    Double speed = 1.0;
    if (mediaPlayer.isPlaying()) {

      Double minSpeedChange = Double.POSITIVE_INFINITY;
      for (Double s : ps) {
        if (s > currentPlaySpeed) {
          if (minSpeedChange > s && s > 1.0) {
            speed = s;
            minSpeedChange = s;
          }
        }
      }

    } else {

      Double minSpeedChange = 1.0;
      for (Double s : ps) {
        if (s > 0.0 && s < 1.0) {
          if (minSpeedChange > s) {
            speed = s;
            minSpeedChange = s;
          }
        }
      }

    }

    invokeSetSpeed(speed);
    updateProgressBar();
  }

  /**
   * Increase the playback speed to the next increment in the reverse direction, and update the UI.
   */
  void increasePlaySpeedReverse() {
    List<Double> ps = new ArrayList<>();
    if (protocolInfo != null && protocolInfo.playSpeedSupported()) {
      ps = protocolInfo.getPlaySpeedDoubles();

    } else {
      ps.addAll(Arrays.asList(fixedSpeeds));
    }
    Double speed = 1.0;
    if (mediaPlayer.isPlaying()) {
      Double minSpeedChange = Double.NEGATIVE_INFINITY;
      for (Double s : ps) {
        if (s < currentPlaySpeed) {
          if (minSpeedChange < s) {
            speed = s;
            minSpeedChange = s;
          }
        }
      }
    } else {
      Double minSpeedChange = -1.0;
      for (Double s : ps) {
        if (s < 0.0 && s > -1.0) {
          if (minSpeedChange < s) {
            speed = s;
            minSpeedChange = s;
          }
        }
      }
    }
    invokeSetSpeed(speed);
    updateProgressBar();
  }


  /**
   * Reset the playback speed to 1x forward.
   */
  void resetPlaySpeed() {
    invokeSetSpeed(1.0);
    updateProgressBar();
  }

  /**
   * call the MediaPlayer invoke method and update currentPlaySpeed based on result.
   */
  void invokeSetSpeed(Double speed) {

    try {
      if (speed != currentPlaySpeed) {
        //setSpeedMethod.invoke(mediaPlayer, speed.floatValue());
        if (speed > 0.0 && speed < 1.0) speed = 0.5;
        if (speed < 0.0 && speed > -1.0) speed = -0.5;
        if (speed > 1.0) speed = 10.0;
        if (speed <= -1.0) speed = -10.0;
        int result = setPlaySpeed(speed);
        Log.d(TAG, "Setting speed to " + speed + " gives return value of: " + result);
      }
    } catch (Exception e) {
      Log.e(TAG, "Error setting speed: " + e.getMessage());
      return;
    }
    currentPlaySpeed = speed;

  }

  private boolean canSeek() {
    // TODO better capabilities analysis from ProtocolInfo
    return mediaPlayer != null && mediaPlayer.getDuration() > 0;
  }

  private boolean canPause() {
    // TODO better capabilities analysis from ProtocolInfo
    return mediaPlayer != null && mediaPlayer.getDuration() > 0;
  }


  /**
   * Set the current broadcast channel.
   * <p/>
   * Sets the video stream to the current channel video, or a random video stream selected
   * from the "channel videos" list, depending on the {@link SettingsHelper#useChannelVideosSetting()}
   * setting.
   */
  private void setCurrentChannel(VideoBroadcast channel) {

    currentChannel = channel;
    currentProgram = null;
    currentVod = null;
    mediaArtwork = null;
    currentProgram = null;

    if (currentChannel == null) {
      return;
    }

    // start fetching icon
    if (currentChannel.getIcon() != null) {
      // need to fetch the icon ourselves, image urls not working for metadata
      Picasso.with(getActivity()).load(currentChannel.getIcon()).into(new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          mediaArtwork = bitmap;
          updateMediaMetadata();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
      });
    }

    // start fetching EPG data
    new FetchEpgTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    // update metadata for media session
    updateMediaMetadata();

    if (settingsHelper.useChannelVideosSetting()) {
      // play actual channel video
      final String res = currentChannel.getResource();
      if (res != null) {
        Log.d(TAG, "Changing video channel to " + res + ".");
        playChannelVideo(Uri.parse(res), CHANNEL_START_DELAY);
      }
    } else {
      // select a random video to play
      playPlaceholderVideo();
    }
  }

  /**
   * Set the EPG data for the current program being viewed.
   * <p/>
   * Updates media session metadata and loads icon.
   *
   * @param program Current EPG program data.
   */
  private void setCurrentProgram(VideoProgram program) {
    currentProgram = program;
    if (currentProgram != null) {
      if (currentProgram.getIcon() != null) {
        // need to fetch the icon ourselves, image urls not working for metadata
        Picasso.with(getActivity()).load(currentProgram.getIcon()).into(new Target() {
          @Override
          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mediaArtwork = bitmap;
            updateMediaMetadata();
          }

          @Override
          public void onBitmapFailed(Drawable errorDrawable) {

          }

          @Override
          public void onPrepareLoad(Drawable placeHolderDrawable) {

          }
        });
      }
      updateMediaMetadata();
    }
  }

  /**
   * Set the current VOD VideoItem to play.
   *
   * @param video Video to play.
   */
  private void setCurrentVodItem(VideoItem video) {
    currentVod = video;
    currentChannel = null;
    currentProgram = null;

    // update metadata for media session
    updateMediaMetadata();

    // start fetching icon
    if (currentVod.getIcon() != null) {
      // need to fetch the icon ourselves, image urls not working for metadata
      Picasso.with(getActivity()).load(currentVod.getIcon()).into(new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          mediaArtwork = bitmap;
          updateMediaMetadata();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
      });
    }

    if (settingsHelper.useChannelVideosSetting()) {
      // play actual VOD item
      play(Uri.parse(currentVod.getResource()));
    } else {
      // change to simulated channel
      playPlaceholderVideo();
    }
  }

  /**
   * Play a random placeholder video from the "channel videos list" in app settings.
   */
  private void playPlaceholderVideo() {
    List<VideoItem> videos = settingsHelper.getChannelVideos();
    if (videos.size() > 0) {
      // select a random video to play
      VideoItem video = videos.get(Math.abs(new Random().nextInt()) % videos.size());
      final String res = video.getResource();
      if (res != null) {
        Log.d(TAG, "Changing video channel to " + res + ".");
        playChannelVideo(Uri.parse(res), CHANNEL_START_DELAY);
      }
    } else if (settingsHelper.useChannelVideosSetting() == false) {
      // show a dialog so the user can pick some videos
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

  /**
   * Start playing a channel video after a delay.
   *
   * @param uri     Uri to play for channel video.
   * @param delayMs Delay before playback task is started
   */
  private void playChannelVideo(final Uri uri, long delayMs) {
    if (channelChangeRunnable != null) {
      // clear the queued runnable
      handler.removeCallbacks(channelChangeRunnable);
    }
    // create new runnable
    channelChangeRunnable = new Runnable() {
      @Override
      public void run() {
        // clear this runnable
        channelChangeRunnable = null;
        // and start playback
        play(uri);
      }
    };
    // start timer
    handler.postDelayed(channelChangeRunnable, delayMs);
  }

  /**
   * Receive a channel change event.
   *
   * @param event Channel change event with data.
   */
  @Subscribe
  public void onChannelChanged(ChannelChangedEvent event) {
    setCurrentChannel(event.getChannel());
  }

  /**
   * Receive a VOD playback event.
   *
   * @param event Playback event with data.
   */
  @Subscribe
  public void onPlayVod(PlayVodEvent event) {
    setCurrentVodItem(event.getVideoItem());
  }

  /**
   * Hide the media progress bar with animation.
   */
  private void hideProgressBar() {
    mediaProgress.animate().alpha(0.0f).start();
  }

  /**
   * Show the media progress bar. Time out and hide after the specified duration in ms.
   * Calling this repeatedly will keep the bar visible.
   * <p/>
   * If the current media has no duration, nothing happens.
   *
   * @param duration Duration to time out and hide.
   */
  private void showProgressBar(long duration) {
    if (!isProgressVisible() && mediaPlayer != null && mediaPlayer.getDuration() > 0) {
      mediaProgress.setAlpha(0.0f);
      mediaProgress.setVisibility(View.VISIBLE);
      mediaProgress.animate().alpha(1.0f).start();
    }
    handler.removeCallbacks(hideProgressRunnable);
    handler.postDelayed(hideProgressRunnable, duration);
  }

  /**
   * Is the progress bar visible or in the process of being shown?
   */
  private boolean isProgressVisible() {
    return mediaProgress.getAlpha() > 0 && mediaProgress.getVisibility() == View.VISIBLE;
  }

  /**
   * Update the progress bar with current media player info.
   * <p/>
   * If the current media has no duration, then nothing happens.
   */

  private void updateProgressBar() {

    if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
      ProgressInfo info = new ProgressInfo(
          new Date(0),
          new Date(mediaPlayer.getCurrentPosition()),
          new Date(mediaPlayer.getDuration()),
          currentPlaySpeed
      );
      mediaProgress.bind(info);
      startProgressUpdates();
    }
  }

  /**
   * Start a timed task to update the progress bar once per second.
   */
  private void startProgressUpdates() {
    if (mediaProgressRunnable != null) {
      handler.removeCallbacks(mediaProgressRunnable);
    }
    if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
      mediaProgressRunnable = new Runnable() {
        @Override
        public void run() {
          updateProgressBar();
          handler.postDelayed(mediaProgressRunnable, 1000);
        }
      };
      handler.postDelayed(mediaProgressRunnable, 1000);
    }
  }

  /**
   * Stop the timed task updating the progress bar.
   */
  private void stopProgressUpdates() {
    if (mediaProgressRunnable != null) {
      handler.removeCallbacks(mediaProgressRunnable);
      mediaProgressRunnable = null;
    }
  }

  /**
   * Show the spinner if {@link #hideSpinner()} is not called after 2 seconds.
   */
  private void showSpinner() {
    handler.postDelayed(showSpinnerRunnable, 2000);
  }

  /**
   * Hide the spinner if visible.
   */
  private void hideSpinner() {
    handler.removeCallbacks(showSpinnerRunnable);
    spinner.hide();
  }

  /**
   * Async task for preparing and starting video playback.
   */
  private class PlayVideoTask extends PrepareVideoTask {

    public PlayVideoTask(Context context, Uri uri, long timeout) {
      super(context, uri, timeout);
    }

    @Override
    protected void onPostExecute(MediaPlayer mediaPlayer) {
      super.onPostExecute(mediaPlayer);
      Throwable error = getError();
      Uri uri = getUri();
      if (isInBackground()) {
        // can't play in background, video surface will not be valid
        Log.e(TAG, "Activity went to background while preparing.");
        hideSpinner();
        return;
      } else if (error != null) {
        hideSpinner();
        Log.e(TAG, "Error starting video playback: " + error);
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.error)
            .setMessage("Error starting video: " + uri + ": " + error.toString())
            .setNeutralButton(getString(android.R.string.ok), null)
            .setPositiveButton(getString(R.string.selectChannelVideos), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getActivity(), SelectChannelVideosActivity.class));
              }
            })
            .create()
            .show();
      } else if (mediaPlayer == null) {
        hideSpinner();
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.error)
            .setMessage("Error starting video: " + uri + "." + (error != null ? "\n\n" + error : ""))
            .setNeutralButton(getString(android.R.string.ok), null)
            .setPositiveButton(getString(R.string.selectChannelVideos), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getActivity(), SelectChannelVideosActivity.class));
              }
            })
            .create()
            .show();
      } else {
        Log.d(TAG, "Starting playback.");
        VideoFragment.this.mediaPlayer = mediaPlayer;
        mediaPlayer.setDisplay(surfaceHolder);
        mediaPlayer.setScreenOnWhilePlaying(true);
        videoUri = uri;
        playVideoTask = null;
        mediaSession.setActive(true);
        int position = settingsHelper.getVideoPosition(uri.toString());
        if (position > 0) {
          Log.d(TAG, "Resuming play at position " + position + ".");
          seekTo(position, true);
        } else {
          hideSpinner();
          mediaPlayer.start();
        }
        updateMediaPlaybackState();
        updateProgressBar();
        showProgressBar(PROGRESS_UI_HIDE_DELAY);
        Log.d(TAG, "Play video task completed for " + uri + ".");
      }
    }
  }

  /**
   * Async task for attempting video playback using DLNA/DTCP player. Falls back to normal
   * player if creating DLNA/DTCP URI was unsuccessful.
   */
  private class PlayDlnaVideoTask extends PlayVideoTask {

    // cached original uri if a new uri is used
    private Uri originalUri;

    public PlayDlnaVideoTask(Context context, Uri uri) {
      super(context, uri, PREPARE_DLNA_VIDEO_TIMEOUT);
    }

    @Override
    protected MediaPlayer doInBackground(Void... params) {
      // transform the URI for DLNA playback
      createDlnaUri();
      // base class default behavior
      return super.doInBackground(params);
    }


    private void createDlnaUri() {
      if (getUri().getScheme().equals("http")) {
        // transform the URI to a DLNA version before prepare
        protocolInfo = new ProtocolInfo(getUri().toString(), 0, null);
        String dlnaUri = protocolInfo.getUrl();
        if (!dlnaUri.equals(getUri().toString())) {
          // dlna URI was created successfully
          Log.d(TAG, "URI after parsing is " + dlnaUri);
          originalUri = getUri();
          setUri(Uri.parse(dlnaUri));
        } else {
          Log.e(TAG, "DLNA URI was not created.");
        }
      }
    }

    @Override
    protected void onPostExecute(MediaPlayer mediaPlayer) {
      if (mediaPlayer == null && originalUri != null) {
        // not successful preparing DLNA playback
        Log.e(TAG, "Error preparing DLNA URI " + getUri() + ". Retrying with normal MediaPlayer.");
        playVideoTask = new PlayVideoTask(getActivity(), originalUri, PREPARE_VIDEO_TIMEOUT);
        playVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      } else {
        // fall back to normal base class handling
        super.onPostExecute(mediaPlayer);
      }
    }
  }


  /**
   * Class for receiving media session callback events, playback controls.
   */
  private class MediaSessionCallback extends MediaSession.Callback {
    @Override
    public void onPause() {
      pause();
    }

    @Override
    public void onPlay() {
      play();
    }

    @Override
    public void onStop() {
      stop();
      mediaSession.setActive(false);
    }

    @Override
    public void onSkipToNext() {
      // TODO channel change
    }

    @Override
    public void onSkipToPrevious() {
      // TODO channel change
    }
  }

  /**
   * Create a media session to tell the system that we are in media playback mode.
   * <p/>
   * This triggers behaviors like the ability to play in the background and display
   * a "now playing" tile on the Home Screen.
   */
  private void createMediaSession() {
    // new session
    mediaSession = new MediaSession(getActivity(), "TVApp");
    // set up callback
    mediaSession.setCallback(new MediaSessionCallback(), new Handler(Looper.getMainLooper()));
    // set flags, the transport control flags enable the now playing tile on home screen
    mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    // intent for returning to playback in progress: jump straight to MainActivity
    mediaSession.setSessionActivity(PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), MainActivity.class), 0));
    // intent for returning to playback in progress: jump straight to MainActivity
    mediaSession.setMediaButtonReceiver(PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), MainActivity.class), 0));
    // update state of player
    updateMediaPlaybackState();
    // update state of channel or program
    updateMediaMetadata();


  }

  /**
   * Update the playback state based on the current media player status.
   */
  private void updateMediaPlaybackState() {
    if (mediaSession != null && mediaPlayer != null) {
      long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
      if (mediaPlayer.isPlaying()) {
        position = mediaPlayer.getCurrentPosition();
      }
      PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
          .setActions(
              PlaybackState.ACTION_PLAY_PAUSE
                  | (mediaPlayer.isPlaying() ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY)
                  | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                  | PlaybackState.ACTION_PLAY_FROM_SEARCH
                  | PlaybackState.ACTION_SKIP_TO_NEXT
                  | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                  | PlaybackState.ACTION_STOP
          )
          .setState(mediaPlayer != null ? mediaPlayer.isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED : PlaybackState.STATE_STOPPED, position, 1.0f);
      mediaSession.setPlaybackState(stateBuilder.build());
    }
  }

  /**
   * Update media session metadata based on current channel, current EPG program,
   * and media artwork (icon).
   */
  private void updateMediaMetadata() {
    if (currentChannel != null && mediaSession != null) {
      MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mediaArtwork);
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
      mediaSession.setMetadata(metadataBuilder.build());
    } else if (currentVod != null) {
      MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, currentVod.getTitle());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentVod.getTitle());
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mediaArtwork);
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, currentVod.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentVod.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, currentVod.getIcon());
      mediaSession.setMetadata(metadataBuilder.build());
    }
  }

  /**
   * Release the media session, e.g. if we're destroying the fragment.
   */
  private void releaseMediaSession() {
    if (mediaSession != null) {
      mediaSession.release();
      mediaSession = null;
    }
  }

  /**
   * Async task that fetches EPG data for the current channel video.
   * This in turn updates the media session metadata, icon, etc.
   */
  private class FetchEpgTask extends AsyncTask<Void, Void, VideoProgram> {

    @Override
    protected VideoProgram doInBackground(Void... params) {
      return dlnaHelper.getCurrentEpgProgram(
          settingsHelper.getEpgServer(),
          currentChannel
      );
    }

    @Override
    protected void onPostExecute(VideoProgram videoProgram) {
      super.onPostExecute(videoProgram);
      setCurrentProgram(videoProgram);
    }
  }


}
