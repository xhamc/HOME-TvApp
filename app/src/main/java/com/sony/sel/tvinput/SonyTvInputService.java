package com.sony.sel.tvinput;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class SonyTvInputService extends TvInputService {

  public static final String TAG = SonyTvInputService.class.getSimpleName();

  @Override
  public Session onCreateSession(String inputId) {
    Log.d(TAG, "Creating new TV playback session for input ID: " + inputId + ".");
    return new TvPlaybackSession(this);
  }

  private class TvPlaybackSession extends Session {

    private MediaPlayer mediaPlayer;
    private Surface surface;
    private PlayDlnaVideoTask playDlnaVideoTask;

    public TvPlaybackSession(Context context) {
      super(context);
      Log.d(TAG, "Session created.");
    }

    @Override
    public void onRelease() {
      Log.d(TAG, "Session onRelease()");
      stop();
    }

    /**
     * Stop media preparation and/or playback if active.
     */
    private void stop() {
      if (mediaPlayer != null) {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
      }
      if (playDlnaVideoTask != null) {
        playDlnaVideoTask.cancel(true);
        playDlnaVideoTask = null;
      }
    }

    @Override
    public boolean onSetSurface(Surface surface) {
      Log.d(TAG, "Session onSetSurface()");
      this.surface = surface;
      if (mediaPlayer != null) {
        mediaPlayer.setSurface(surface);
      }
      return true;
    }

    @Override
    public void onSetStreamVolume(float volume) {
      Log.d(TAG, "Session onSetStreamVolume(" + volume + ").");
      if (mediaPlayer != null) {
        mediaPlayer.setVolume(volume, volume);
      }
    }

    @Override
    public boolean onTune(Uri channelUri) {
      Log.d(TAG, "Session onTune(" + channelUri + ").");

      // mask video, notify we are busy "tuning"
      notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

      // stop any playback or preparation
      stop();

      Log.d(TAG, "Preparing for playback.");
      playDlnaVideoTask = new PlayDlnaVideoTask(getBaseContext(), channelUri, 30000, surface) {
        @Override
        protected void onPostExecute(MediaPlayer mediaPlayer) {
          if (mediaPlayer != null) {
            Log.d(TAG, "Media prepared, starting playback.");
            TvPlaybackSession.this.mediaPlayer = mediaPlayer;
            mediaPlayer.start();
            // unmask video, notify we are playing
            notifyVideoAvailable();
            if (Build.VERSION.SDK_INT >= 23) {
              notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
            }
          } else {
            Log.e(TAG, "Error preparing media playback.");
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
          }
          playDlnaVideoTask = null;
        }
      };
      playDlnaVideoTask.execute();
      return true;
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftPause() {
      Log.d(TAG, "Session onTimeShiftPause().");
      if (mediaPlayer != null) {
        mediaPlayer.pause();
      }
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftResume() {
      Log.d(TAG, "Session onTimeShiftResume().");
      if (mediaPlayer != null) {
        mediaPlayer.start();
      }
    }

    @TargetApi(23)
    @Override
    public void onTimeShiftSeekTo(long value) {
      Log.d(TAG, "Session onTimeShiftSeekTo(" + value + ").");
      mediaPlayer.seekTo((int) value);
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