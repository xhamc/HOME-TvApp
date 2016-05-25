package com.sony.sel.tvapp.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.sony.sel.tvapp.util.PrepareVideoTask;
import com.sony.sel.tvapp.util.ProtocolInfo;

/**
 * AsyncTask for initiating playback of a video
 * using Sony PBPlayer or DTCPPlayer to a specified Surface.
 */
public class PlayDlnaVideoTask extends PrepareVideoTask {

  private final Surface videoSurface;

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
  }


  @Override
  protected MediaPlayer doInBackground(Void... params) {
    if (getUri().getScheme().equals("http")) {
      // transform the URI to a DLNA version before prepare
      Log.d(TAG, "Creating DLNA URI.");
      ProtocolInfo protocolInfo = new ProtocolInfo(getUri().toString(), 0, null);
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
}

