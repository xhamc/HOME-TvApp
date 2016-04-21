package com.sony.sel.tvapp.fragment;

import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.MainActivity;
import com.sony.sel.tvapp.activity.SelectChannelVideosActivity;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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

  @Bind(R.id.surfaceView)
  SurfaceView surfaceView;
  @Bind(R.id.spinner)
  ProgressBar spinner;

  private VideoBroadcast currentChannel;
  private VideoProgram currentProgram;

  private Uri videoUri;
  private MediaPlayer mediaPlayer;
  private SurfaceHolder surfaceHolder;
  private PlayVideoTask playVideoTask;
  private MediaSession mediaSession;
  private Bitmap mediaArtwork;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // inflate view
    View contentView = inflater.inflate(R.layout.video_fragment, null);
    ButterKnife.bind(this, contentView);

    // current EPG channel from settings
    setCurrentChannel(SettingsHelper.getHelper(getActivity()).getCurrentChannel());

    // create media session
    createMediaSession();

    return contentView;
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
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

  @Override
  public void onResume() {
    super.onResume();
    if (mediaPlayer != null) {
      // resume play
      play();
    } else if (currentChannel != null) {
      // pick a channel video to play
      changeChannel();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stop();
    releaseMediaSession();
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
    if (playVideoTask != null) {
      // cancel a playback task in progress
      playVideoTask.cancel(true);
    }
    stop();
    if (uri != null) {
      showSpinner();
      // create & execute async task for video playback
      playVideoTask = new PlayVideoTask(uri);
      playVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void showSpinner() {
    spinner.setVisibility(View.VISIBLE);
  }

  private void hideSpinner() {
    spinner.setVisibility(View.GONE);
  }

  /**
   * Restart playback of an existing video that is paused or stopped.
   */
  public void play() {
    if (mediaPlayer == null && videoUri != null) {
      // restart playback from scratch
      play(videoUri);
    } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
      // resume play
      mediaPlayer.start();
    }
  }

  /**
   * Pause a video that's playing.
   */
  public void pause() {
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
      updateMediaPlaybackState();
    }
  }

  /**
   * Stop video playback & release player resources.
   */
  public void stop() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
      updateMediaPlaybackState();
    }
  }

  /**
   * Change to a random video stream selected from the "channel videos" list in Settings.
   */
  private void changeChannel() {
    List<VideoItem> videos = SettingsHelper.getHelper(getActivity()).getChannelVideos();
    if (videos.size() > 0) {
      VideoItem video = videos.get(Math.abs(new Random().nextInt()) % videos.size());
      final String res = video.getResource();
      final String protocolInfo = video.getProtocolInfo();
      if (res != null) {
        Log.d(TAG, "Changing video channel to " + res + ".");
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

  /**
   * Async task for initializing a MediaPlayer and starting video playback.
   */
  private class PlayVideoTask extends AsyncTask<Void, Void, MediaPlayer> {

    private final Uri uri;

    public PlayVideoTask(Uri uri) {
      this.uri = uri;
    }

    @Override
    protected MediaPlayer doInBackground(Void... params) {
      Log.d(TAG, "Starting play video task for " + uri + ".");
      if (isCancelled()) {
        // don't do anything if canceled
        return null;
      }
      MediaPlayer mediaPlayer = null;
      try {
        // create, set data source and prepare media player with one call
        mediaPlayer = MediaPlayer.create(getActivity(), uri);
        if (isCancelled()) {
          // don't continue if canceled while preparing
          return mediaPlayer;
        }
        mediaPlayer.setDisplay(surfaceHolder);
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
          @Override
          public boolean onError(MediaPlayer mp, int what, int extra) {
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.error)
                .setMessage("Player error: code = " + what + ", extra = " + extra + ".")
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
      } catch (Throwable e) {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.error)
            .setMessage("Error playing video:" + uri + ": " + e.toString())
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
      Log.w(TAG, "Play video task canceled for " + uri + ".");
      if (mediaPlayer != null) {
        mediaPlayer.release();
      }
    }

    @Override
    protected void onPostExecute(MediaPlayer mediaPlayer) {
      super.onPostExecute(mediaPlayer);
      Log.d(TAG, "Play video task completed for " + uri + ".");
      hideSpinner();
      VideoFragment.this.mediaPlayer = mediaPlayer;
      videoUri = uri;
      playVideoTask = null;
      mediaSession.setActive(true);
      updateMediaPlaybackState();
      new FetchEpgTask().executeOnExecutor(THREAD_POOL_EXECUTOR);
    }
  }

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
      // TODO real channel change
      changeChannel();
    }

    @Override
    public void onSkipToPrevious() {
      // TODO real channe change
      changeChannel();
    }
  }

  private void setCurrentChannel(VideoBroadcast channel) {
    currentChannel = channel;
    mediaArtwork = null;
    currentProgram = null;
    if (currentChannel != null) {
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
      changeChannel();
      updateMediaMetadata();
    }
  }

  private void setCurrentProgram(VideoProgram program) {
    if (program != null) {
      currentProgram = program;
      if (program.getIcon() != null) {
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

  private void updateMediaPlaybackState() {
    if (mediaSession != null && mediaPlayer != null) {
      long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
      if (mediaPlayer != null && mediaPlayer.isPlaying()) {
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

  private void updateMediaMetadata() {
    if (currentChannel != null && mediaSession != null) {
      MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
      // To provide most control over how an item is displayed set the
      // display fields in the metadata
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mediaArtwork);
      // TODO more metadata?
      mediaSession.setMetadata(metadataBuilder.build());
    }
  }

  private void releaseMediaSession() {
    if (mediaSession != null) {
      mediaSession.release();
      mediaSession = null;
    }
  }

  private class FetchEpgTask extends AsyncTask<Void, Void, VideoProgram> {

    @Override
    protected VideoProgram doInBackground(Void... params) {
      return DlnaHelper.getHelper(getActivity()).getCurrentEpgProgram(
          SettingsHelper.getHelper(getActivity()).getEpgServer(),
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
