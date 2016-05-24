package com.sony.sel.tvapp.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;

import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.PrepareVideoTask;
import com.sony.sel.tvapp.util.ProtocolInfo;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.List;
import java.util.Random;

/**
 * Helper to allow simple DLNA/DTCP video playback to a provided Surface.
 */
public class VideoHelper {

  public static final String TAG = VideoHelper.class.getSimpleName();

  private final Context context;
  private final SettingsHelper settingsHelper;

  public VideoHelper(Context context) {
    this.context = context;
    this.settingsHelper = SettingsHelper.getHelper(context);
  }

  /**
   * Prepare a video and play it on the provided surface.
   *
   * @param videoUri Video URI. Must have the "http://" scheme.
   * @param surface  Surface to play the video on.
   * @return An AsyncTask that has already been configured to prepare and play the video and is queued to execute. Can be cancelled by the caller.
   */
  public AsyncTask<Void, Void, MediaPlayer> playVideo(String videoUri, Surface surface) {

    // use the provided video URI if the settings are configured that way, otherwise a placeholder
    Uri uri = settingsHelper.useChannelVideosSetting() ? Uri.parse(videoUri) : getPlaceholderVideo();

    if (uri.getScheme().equals("http")) {
      // transform the URI to a DLNA version before prepare
      ProtocolInfo protocolInfo = new ProtocolInfo(uri.toString(), 0, null);
      String dlnaUri = protocolInfo.getUrl();
      if (!dlnaUri.equals(uri.toString())) {
        // dlna URI was created successfully
        Log.d(TAG, "URI after parsing is " + dlnaUri);
        PlayVideoTask task = new PlayVideoTask(context, Uri.parse(dlnaUri), 30000, surface);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
      } else {
        Log.e(TAG, "DLNA URI was not created.");
      }
    } else {
      // can't play if it't not http
      Log.e(TAG, "Wrong URI scheme for playback.");
    }
    return null;
  }

  private static class PlayVideoTask extends PrepareVideoTask {

    private final Surface videoSurface;

    /**
     * Initialize the async task.
     *
     * @param context      Context for accessing resources.
     * @param uri          URI of the video to play.
     * @param timeout      Time in milliseconds to wait before canceling media prepare with a timeout.
     * @param videoSurface Surface to play the video on when prepare is successful.
     */
    protected PlayVideoTask(Context context, Uri uri, long timeout, Surface videoSurface) {
      super(context, uri, timeout);
      this.videoSurface = videoSurface;
    }

    @Override
    protected void onPostExecute(MediaPlayer mediaPlayer) {
      super.onPostExecute(mediaPlayer);
      if (mediaPlayer != null) {
        mediaPlayer.setSurface(videoSurface);
        mediaPlayer.start();
      }
    }
  }

  /**
   * Get a random placeholder video if they have been configured.
   *
   * @return A placeholder video, or null if no placeholders have been configured.
   */
  private Uri getPlaceholderVideo() {
    List<DlnaObjects.VideoItem> videos = settingsHelper.getChannelVideos();
    if (videos.size() > 0) {
      // select a random video to play
      DlnaObjects.VideoItem video = videos.get(Math.abs(new Random().nextInt()) % videos.size());
      final String res = video.getResource();
      if (res != null) {
        return (Uri.parse(res));
      }
    }
    // no videos found, or no resource for selected video
    return null;
  }
}
