package com.sony.sel.tvinput;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class SonyTvInputService extends TvInputService {

  public static final String TAG = SonyTvInputService.class.getSimpleName();

  private MediaPlayer mediaPlayer;
  private Surface surface;
  private PlayDlnaVideoTask playDlnaVideoTask;

  @Override
  public Session onCreateSession(String inputId) {
    Log.d(TAG, "Creating new tvinput session for: " + inputId);
    surface = null;
    return new SimpleSessionImpl(this);
  }

  private class SimpleSessionImpl extends Session {
    public SimpleSessionImpl(Context context) {
      super(context);
    }

    @Override
    public void onRelease() {
      Log.d(TAG, "onRelease");
      if (mediaPlayer != null) {
        mediaPlayer.release();
        mediaPlayer = null;
      }
    }

    @Override
    public boolean onSetSurface(Surface surface) {
      Log.d(TAG, "onSetSurface");
      SonyTvInputService.this.surface = surface;
      return true;
    }

    @Override
    public void onSetStreamVolume(float volume) {
      Log.d(TAG, "onSetStreamVolume: " + Float.toString(volume));
      // TODO Auto-generated method stub
    }

    @Override
    public boolean onTune(Uri channelUri) {
      Log.d(TAG, "onTune: " + channelUri.toString());

      if (mediaPlayer != null) {
        mediaPlayer.release();
        mediaPlayer = null;
      }
      if (playDlnaVideoTask != null) {
        playDlnaVideoTask.cancel(true);
        playDlnaVideoTask = null;
      }

      notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
      Log.d(TAG, "attempting to start playback");
      playDlnaVideoTask = new PlayDlnaVideoTask(getBaseContext(), channelUri, 30000, surface) {
        @Override
        protected void onPostExecute(MediaPlayer mediaPlayer) {
          Log.d(TAG, "onPostExecute video playback task");
          super.onPostExecute(mediaPlayer);
          if (mediaPlayer != null) {
            SonyTvInputService.this.mediaPlayer = mediaPlayer;
            notifyVideoAvailable();
            if (Build.VERSION.SDK_INT >= 23) {
              notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
            }
          }
          playDlnaVideoTask = null;
        }
      };
      playDlnaVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      return true;
    }


    @TargetApi(23)
    @Override
    public void onTimeShiftPause() {
      Log.d(TAG, "pause requested");
      if (mediaPlayer != null) {
        mediaPlayer.pause();
      }
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftResume() {
      Log.d(TAG, "resume requested");
      if (mediaPlayer != null) {
        mediaPlayer.start();
      }
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftSeekTo(long value) {
      Log.d(TAG, "seek to: " + value);
    }

    @TargetApi(23)
    @Override
    public long onTimeShiftGetStartPosition() {
      Log.d(TAG, "onTimeShiftGetStartPosition");
      // TODO:  calculate seek positoin
      return TvInputManager.TIME_SHIFT_INVALID_TIME;
    }

    @TargetApi(23)
    @Override
    public long onTimeShiftGetCurrentPosition() {
      Log.d(TAG, "onTimeShiftGetCurrentPosition");
      if (mediaPlayer != null) {
        return mediaPlayer.getCurrentPosition();
      } else {
        return 0;
      }
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
      Log.d(TAG, "playback params:" + params.toString());
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
      Log.d(TAG, "onSetCaptionEnabled");
      // TODO Auto-generated method stub

    }
  }
}