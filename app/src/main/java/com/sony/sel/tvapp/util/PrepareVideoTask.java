package com.sony.sel.tvapp.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * Async task for initializing a new MediaPlayer object and preparing to start video playback.
 * <p/>
 * If the video is successfully prepared for playback, {@link #onPostExecute(Object)} will receive
 * a MediaPlayer object that is ready to be attached to a SurfaceView and played.
 * <p/>
 * Subclasses need to override {@link #onPostExecute(Object)} to attach a SurfaceView and start playback.
 * If the object passed in is null, subclasses should call {@link #getError()}
 * to find out what happened.
 * <p/>
 * This task can be cancelled at any time while it is executing. If cancelled,
 * {@link #onPostExecute(Object)} will never be called, the MediaPlayer will be
 * cleaned up and released, and the AsyncTask will finish.
 */
public abstract class PrepareVideoTask extends AsyncTask<Void, Void, MediaPlayer> {

  public static final String TAG = PrepareVideoTask.class.getSimpleName();

  private final Context context;
  private final long timeout;
  private Uri uri;

  private Throwable error;
  private boolean prepared;

  /**
   * Initialize the async task.
   *
   * @param context Context for accessing resources.
   * @param uri     URI of the video to play.
   * @param timeout Time in milliseconds to wait before canceling media prepare with a timeout.
   */
  protected PrepareVideoTask(Context context, Uri uri, long timeout) {
    this.context = context;
    this.uri = uri;
    this.timeout = timeout;
    Log.d(TAG, "Prepare video task created for " + uri.toString() + ".");
  }

  public Uri getUri() {
    return uri;
  }

  public void setUri(Uri uri) {
    this.uri = uri;
  }

  public Throwable getError() {
    return error;
  }

  @Override
  protected MediaPlayer doInBackground(final Void... params) {
    Log.d(TAG, "Starting prepare video task for " + uri + ".");
    MediaPlayer mediaPlayer = null;
    if (isCancelled()) {
      // don't do anything if canceled
      return null;
    }

    try {
      mediaPlayer = prepareMedia(uri, timeout);
      if (isCancelled()) {
        return null;
      } else {
        return mediaPlayer;
      }
    } catch (IOException e) {
      Log.e(TAG, "Error preparing video: " + e);
      error = e;
      return null;
    } catch (InterruptedException e) {
      Log.e(TAG, "Error preparing video: " + e);
      error = e;
      return null;
    } catch (Throwable e) {
      Log.e(TAG, "Error preparing video: " + e);
      error = e;
      return null;
    }

  }

  private MediaPlayer prepareMedia(Uri videoUri, long timeout) throws IOException, InterruptedException {
    final Object prepareLock = new Object();
    final MediaPlayer mediaPlayer = new MediaPlayer();
    try {
      mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
          Log.e(TAG, "Player error: " + decodeMediaStatus(what) + ". Extra = " + extra + ".");
          return true;
        }
      });
      mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
          Log.d(TAG, "Video buffering: " + percent + "%.");
        }
      });
      mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
          Log.d(TAG, "Video info: what = " + decodeMediaStatus(what) + ", extra = " + extra + '.');
          return false;
        }
      });
      mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.d(TAG, "Video prepared.");
          prepared = true;
          synchronized (prepareLock) {
            prepareLock.notifyAll();
          }
        }
      });
      mediaPlayer.setDataSource(context, videoUri);
      Log.d(TAG, "Preparing video: " + videoUri + ".");
      mediaPlayer.prepareAsync();
      synchronized (prepareLock) {
        prepareLock.wait(timeout);
        if (!prepared) {
          mediaPlayer.release();
          throw new InterruptedException("Video prepare timed out after " + timeout + " ms.");
        }
      }
      return mediaPlayer;
    } catch (Throwable e) {
      // release player on errors and re-throw the error
      mediaPlayer.release();
      throw e;
    }
  }

  @Override
  protected void onCancelled(MediaPlayer mediaPlayer) {
    super.onCancelled(mediaPlayer);
    Log.w(TAG, "Prepare video task canceled for " + uri + ".");
    if (mediaPlayer != null) {
      mediaPlayer.release();
    }
  }

  private static String decodeMediaStatus(int code) {
    switch (code) {
      case 703:
        return "MEDIA_INFO_NETWORK_BANDWIDTH";
      case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
        return "MEDIA_INFO_BAD_INTERLEAVING";
      case MediaPlayer.MEDIA_INFO_BUFFERING_END:
        return "MEDIA_INFO_BUFFERING_END";
      case MediaPlayer.MEDIA_INFO_BUFFERING_START:
        return "MEDIA_INFO_BUFFERING_START";
      case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
        return "MEDIA_INFO_METADATA_UPDATE";
      case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
        return "MEDIA_INFO_NOT_SEEKABLE";
      case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
        return "MEDIA_INFO_SUBTITLE_TIMED_OUT";
      case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
        return "MEDIA_INFO_UNSUPPORTED_SUBTITLE";
      case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
        return "MEDIA_INFO_VIDEO_RENDERING_START";
      case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
        return "MEDIA_INFO_VIDEO_TRACK_LAGGING";
      case MediaPlayer.MEDIA_ERROR_IO:
        return "MEDIA_ERROR_IO";
      case MediaPlayer.MEDIA_ERROR_MALFORMED:
        return "MEDIA_ERROR_MALFORMED";
      case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
        return "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
      case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
        return "MEDIA_ERROR_SERVER_DIED";
      case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
        return "MEDIA_ERROR_TIMED_OUT";
      case MediaPlayer.MEDIA_ERROR_UNKNOWN:
      default:
        return String.valueOf(code);
    }
  }
}
