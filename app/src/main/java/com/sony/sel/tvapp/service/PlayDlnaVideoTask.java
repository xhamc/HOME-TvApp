package com.sony.sel.tvapp.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.PrepareVideoTask;
import com.sony.sel.tvapp.util.ProtocolInfo;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.List;
import java.util.Random;

/**
 * AsyncTask for initiating playback of a video
 * using Sony PBPlayer or DTCPPlayer to a specified Surface.
 */
public class PlayDlnaVideoTask extends PrepareVideoTask {

  private final Surface videoSurface;
    private final Context context;
    private final SettingsHelper settingsHelper;
    String videoUri;

  /**
   * Initialize the async task.
   *
   * @param context      Context for accessing resources.
   * @param uri          http URI of the video to play.
   * @param timeout      Time in milliseconds to wait before canceling media prepare with a timeout.
   * @param videoSurface Surface to play the video on when prepare is successful.
   */
  protected PlayDlnaVideoTask(Context context, Uri uri, long timeout, Surface videoSurface) {
    super(context, uri, timeout);
    this.videoSurface = videoSurface;
      this.context = context;
      this.settingsHelper = SettingsHelper.getHelper(context);
      this.videoUri = uri.toString();
  }


  @Override
  protected MediaPlayer doInBackground(Void... params) {
      // use the provided video URI if the settings are configured that way, otherwise a placeholder
      Uri uri = settingsHelper.useChannelVideosSetting() ? Uri.parse(videoUri) : getPlaceholderVideo();
    if (uri.getScheme().equals("http")) {
      // transform the URI to a DLNA version before prepare
      Log.d(TAG, "Creating DLNA URI.");
      ProtocolInfo protocolInfo = new ProtocolInfo(uri.toString(), 0, null);
      String dlnaUri = protocolInfo.getUrl();
      if (!dlnaUri.equals(getUri().toString())) {
        // dlna URI was created successfully
        Log.d(TAG, "URI after parsing is " + dlnaUri);
        setUri(Uri.parse(dlnaUri));
      } else {
        // couldn't create transformed uri
        Log.e(TAG, "DLNA URI was not created.");
      }
    } else {
      // can't transform if it's not http
      Log.e(TAG, "Wrong URI scheme for DLNA playback.");
    }
    // start preparation of the media player
    return super.doInBackground(params);
  }

  @Override
  protected void onPostExecute(MediaPlayer mediaPlayer) {
    super.onPostExecute(mediaPlayer);
    if (mediaPlayer != null) {
      mediaPlayer.setSurface(videoSurface);
      mediaPlayer.start();
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

