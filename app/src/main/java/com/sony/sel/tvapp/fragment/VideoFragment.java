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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpHead;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Fragment for displaying video
 */
public class VideoFragment extends BaseFragment {

  public static final String TAG = VideoFragment.class.getSimpleName();

  @Bind(R.id.videoSurfaceView)
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
      Log.d(TAG, "Resuming video playback.");
      mediaPlayer.start();
    }
  }

  /**
   * Pause a video that's playing.
   */
  public void pause() {
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      Log.d(TAG, "Pausing video.");
      mediaPlayer.pause();
      updateMediaPlaybackState();
    }
  }

  /**
   * Stop video playback & release player resources.
   */
  public void stop() {
    if (mediaPlayer != null) {
      Log.d(TAG, "Stopping and releasing video.");
      mediaPlayer.release();
      mediaPlayer = null;
      updateMediaPlaybackState();
    }
  }

  /**
   * Change to a random video stream selected from the "channel videos" list in Settings.
   */
  private void changeChannel() {
    if (SettingsHelper.getHelper(getActivity()).useChannelVideosSetting()){
      final String res = currentChannel.getResource();
      if (res != null) {
        Log.d(TAG, "Changing video channel to " + res + ".");
        play(Uri.parse(res));
      }
      return;
    }

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

  /**
   * Async task for initializing a MediaPlayer and starting video playback.
   */
  private class PlayVideoTask extends AsyncTask<Void, Void, MediaPlayer> {

    // timeout value for preparing video
    private final static long PREPARE_TIMEOUT = 10000;

    private final Uri uri;
    private Throwable error;
    private boolean prepared;

    public PlayVideoTask(Uri uri) {
      this.uri = uri;
    }

    @Override
    protected MediaPlayer doInBackground(final Void... params) {
      Log.d(TAG, "Starting play video task for " + uri + ".");
      MediaPlayer mediaPlayer = null;
      if (isCancelled()) {
        // don't do anything if canceled
        return null;
      }

      if (uri.getScheme().equals("http")) {
        // transform to a DLNA URI
        ProtocolInfo mProtoInfo=new ProtocolInfo(uri.toString(), 0,null);
        Uri videoUri = Uri.parse(mProtoInfo.getUrl());
        Log.d(TAG, "URL after parsing: "+videoUri);
        try {
          mediaPlayer = prepareMedia(videoUri);
          if (isCancelled()) {
            mediaPlayer.release();
            return null;
          } else {
            return mediaPlayer;
          }
        } catch (IOException e) {
          Log.e(TAG, "Error playing DLNA URI: " + e);
        } catch (InterruptedException e) {
          Log.e(TAG, "Error playing DLNA URI: " + e);
        } catch (Throwable e) {
          Log.e(TAG, "Error playing DLNA URI: " + e);
        }
      }

      try {
        mediaPlayer = prepareMedia(uri);
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

    private MediaPlayer prepareMedia(Uri videoUri) throws IOException, InterruptedException {
      final Object prepareLock = new Object();
      MediaPlayer mediaPlayer = new MediaPlayer();
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
      mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
          // loop when done
          Log.d(TAG, "Video complete, restarting.");
          mp.seekTo(0);
          mp.start();
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
      mediaPlayer.setDataSource(getActivity(), videoUri);
      Log.d(TAG, "Preparing video: " + videoUri + ".");
      mediaPlayer.prepareAsync();
      synchronized (prepareLock) {
        prepareLock.wait(PREPARE_TIMEOUT);
        if (!prepared) {
          throw new InterruptedException("Video prepare timed out after " + PREPARE_TIMEOUT + " ms.");
        }
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
      hideSpinner();
      if (error != null) {
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
        mediaPlayer.setDisplay(surfaceHolder);
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.start();
        VideoFragment.this.mediaPlayer = mediaPlayer;
        videoUri = uri;
        playVideoTask = null;
        mediaSession.setActive(true);
        updateMediaPlaybackState();
        new FetchEpgTask().executeOnExecutor(THREAD_POOL_EXECUTOR);
        Log.d(TAG, "Play video task completed for " + uri + ".");
      }
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

  private void updateMediaMetadata() {
    if (currentChannel != null && mediaSession != null) {
      MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
      // To provide most control over how an item is displayed set the
      // display fields in the metadata
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentProgram != null ? currentProgram.getTitle() : currentChannel.getCallSign());
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mediaArtwork);
      metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mediaArtwork);
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
      metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, currentProgram != null ? currentProgram.getIcon() : currentChannel.getIcon());
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

  private class ProtocolInfo {


      private static final String TAG                             = "CVP-2";
      private static final String GET_CONTENT_FEATURES      = "getcontentFeatures.dlna.org";
      private static final String CONTENT_FEATURES        = "contentfeatures.dlna.org";
      private static final String CONTENT_LENGTH          = "content-length";
      private static final String GET_CONTENT_RANGE       = "Content-Range";
      private static final String GET_CONTENT_RANGE_DTCP       = "Content-Range.dtcp.com";
      private static final String PLAYSPEED_HEADER        ="PlaySpeed.dlna.org";

      private static final String GET_RANGE               = "Range";
      private static final String GET_TIMESEEK_RANGE      = "TimeSeekRange.dlna.org";
      private static final String DLNA_ORG_OP             = "dlna\\.org_op";
      private static final String DLNA_ORG_PN             = "dlna\\.org_pn";
      private static final String DLNA_ORG_PS			  = "dlna\\.org_ps";
      private static final String DTCP1HOST 			  = "DTCP1HOST";
      private static final String DTCP1PORT 			  = "DTCP1PORT";
      private static final int SONY_PB_PLAYER = 8;
      private static final int SONY_PB_PLAYER_DTCP = 9;
      private static final int MTK_STREAM_PLAYER = 6; /* This is our good guess */
      private static final int                       RANGE_OP_INDEX          = 1;
      private static final int                       TIMESEEK_OP_INDEX       = 0;
      private static final boolean 		PLAYSPEED_MODE = true;

      private final String PLAYER_SET_KEY_PROTOCOLINFO  = "protocol_info";
      private final String PLAYER_SET_KEY_PLAYMODE      = "playmode";

      private final String PLAYER_SET_KEY_DURATION      = "duration";
      private final String PLAYER_SET_KEY_CONTENTLENGTH = "contentlength";
      private final String PLAYSPEEDS					  ="PLAYSPEED";

      protected static final String COOKIE_HEADER = "Cookie";
      protected static final String USER_AGENT_HEADER = "User-Agent";

      private final boolean USE_URI_METHOD=true;

      protected boolean mGetContentFeaturesResult;
      protected boolean mGetOpFlagsResult;
      protected boolean mGetContentRangeResult;
      protected boolean mGetTimeSeekRangeResult;
      protected boolean mGetPSParamResult;
      protected String mAKEhost;
      protected String mAKEport;
      protected String mProtocolInfo;
      private String mUrl;
      private String mCookies;
      private String mUserAgent;
      private int mRequestedPosition;
      protected long mContentLength;
      protected double[] mTimeSeekRange={-1, -1, -1};
      protected char mOP[]={'0','0'};
      protected List<String> mPsString;
      protected List<Double> mPsValue;
      protected String mPsStringTotal;
      protected long[] mContentRange={-1,-1,-1};

      private int mPlayerType;

      protected String mURLextensions="";

      private ProtocolInfo(String url, int position, Map<String, String> hdrs){
        mGetContentFeaturesResult=false;
        mGetOpFlagsResult=false;
        mGetContentRangeResult=false;
        mGetTimeSeekRangeResult=false;
        mGetPSParamResult=false;
        mProtocolInfo="";
        mContentLength=-1;
        mAKEhost="";
        mUrl=url;
        if (hdrs!=null && hdrs.containsKey(COOKIE_HEADER))
          mCookies=hdrs.get(COOKIE_HEADER);
        else
          mCookies="";
        if (hdrs!=null && hdrs.containsKey(USER_AGENT_HEADER))
          mUserAgent=hdrs.get(USER_AGENT_HEADER);
        else
          mUserAgent="";

        getProtocolInfo();

        try {

          if (getPlayerType()==SONY_PB_PLAYER){
            mUrl=mUrl.replaceFirst("(?i)http", "dlna://URI=http");
            if (USE_URI_METHOD) {
              mUrl = mUrl.concat(mURLextensions);
            }
            Log.d("CVP-2","Using Sony PB Player by replacing http with dlna : "+mUrl);
          }else if(getPlayerType()==SONY_PB_PLAYER_DTCP){

            String insertIntoUrl="";
            if (!"".equals(mAKEhost) && !"".equals(mAKEport) && USE_URI_METHOD)
              insertIntoUrl=insertIntoUrl.concat("dtcpip://AKEHost="+mAKEhost+",AKEPort="+mAKEport+",URI=http");
            else
              insertIntoUrl=insertIntoUrl.concat("dtcpip://URI=http");
              mUrl=mUrl.replaceFirst("(?i)http", insertIntoUrl);
            if (USE_URI_METHOD) {
              mUrl = mUrl.concat(mURLextensions);
            }
            Log.v(TAG,"URL with DTCP-IP extensions: "+mUrl);
            Log.d("CVP-2","Using Sony PB Player by replacing http with dtcpip : "+mUrl);
          }else {
            Log.d(TAG, "Using default player selected by android factory: URL:" + mUrl);
          }


        } catch (Exception e) {
          Log.e(TAG, e.toString());
          if (e.getMessage() != null) {
            Log.e(TAG, e.getMessage());
          }
        }

      }

      public String getUrl(){
        return mUrl;
      }



      public int getPlayerType() {

        int playerType = MTK_STREAM_PLAYER;
        Pattern pattern = Pattern.compile(".*" + DLNA_ORG_PN + "=([^;]+);.*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(mProtocolInfo);
        if(matcher.find()) {
          Log.d(TAG, "DLNA profile name: " + matcher.group(1));
          if(!matcher.group(1).contains("DASH")){

            if (!matcher.group(1).contains("DTCP"))
              playerType = SONY_PB_PLAYER;
            else
              playerType = SONY_PB_PLAYER_DTCP;
          }

        } else {
          Log.d(TAG, "Didn't find DLNA profile name");
        }
        Log.d(TAG, "getPlayerType() returns " + playerType + " (Sony: " + SONY_PB_PLAYER + ", MTK: " + MTK_STREAM_PLAYER + ")");
        return playerType;
      }

      public double getDuration(){

        return mTimeSeekRange[2];
      }

      public long getContentLength(){
        if (mContentRange[2]!=-1){
          return mContentRange[2];
        }else {
          return mContentLength;
        }
      }

      public String getProtocolInfoString(){
        return mProtocolInfo;
      }

      public boolean playSpeedSupported(){
        return mGetPSParamResult;
      }

      public boolean testPlaySpeedSupport(String sp){
        return mPsString.contains(sp);
      }
      public boolean testPlaySpeedSupport(Double sp){
        return mPsValue.contains(sp);
      }
      private Map<String, String> headerResult = new HashMap<>();
      public Map<String, String> getHeaders() {
        return headerResult;
      }


      public void getProtocolInfo(){
        Log.d(TAG, "getProtocolInfo, url=" + mUrl);
        Pattern pattern = Pattern.compile(".mpd", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(mUrl);
        if(matcher.find()) {
          mProtocolInfo="DASH";
          Log.d(TAG,"found DASH Content through .mpd extension");
          return;
        }

        Header[] mHeaders;
        //HttpClient httpclient = new DefaultHttpClient();
        HttpClient httpclient = HttpClientBuilder.create().build();

        HttpHead httphead = new HttpHead(mUrl);
        //headerResult.put(PLAYER_SET_KEY_USEFLAG, "on");
        httphead.setHeader(COOKIE_HEADER, mCookies);
        httphead.setHeader(USER_AGENT_HEADER, mUserAgent);
        headerResult.put(COOKIE_HEADER, mCookies);
        headerResult.put(USER_AGENT_HEADER, mUserAgent);

        try{
          mAKEhost=getAKE(DTCP1HOST, mUrl);
          mAKEport=getAKE(DTCP1PORT, mUrl);
          if (mGetContentFeaturesResult=mGetContentFeatures(httphead,httpclient)){

            if (mGetOpFlagsResult=getOPFlags(mProtocolInfo)){
              if (mOP[RANGE_OP_INDEX]=='1'){
                httphead.setHeader(GET_RANGE,"bytes=0-");
              }
              if (mOP[TIMESEEK_OP_INDEX]=='1'){
                httphead.setHeader(GET_TIMESEEK_RANGE,"npt=0.0-");
              }
              Log.d(TAG, "before httpclient.execute(httphead).getAllHeaders();");
              mHeaders =  httpclient.execute(httphead).getAllHeaders();
              Log.d(TAG, "after httpclient.execute(httphead).getAllHeaders();");
              Log.d(TAG, "mHeaders: " + mHeaders);
              mURLextensions=mURLextensions.concat(",SEEK_TYPE="+mOP[0]+mOP[1]);
              if (mOP[TIMESEEK_OP_INDEX]=='1'){
                //mURLextensions=mURLextensions.concat(",SeekType=Time");
                if (mRequestedPosition>0)
                  mURLextensions=mURLextensions.concat(",CurrentPosition="+ Integer.toString(mRequestedPosition));
                else
                  mURLextensions=mURLextensions.concat(",CurrentPosition=0");
                httphead.removeHeaders(GET_TIMESEEK_RANGE);
                mGetTimeSeekRangeResult=getTimeSeekRange(mHeaders, GET_TIMESEEK_RANGE);
                mGetContentRangeResult=getContentRange(mHeaders, GET_CONTENT_RANGE);
                if (!mGetContentRangeResult)
                  mGetContentRangeResult=getContentRange(mHeaders, GET_CONTENT_RANGE_DTCP);
                headerResult.put(PLAYER_SET_KEY_PROTOCOLINFO, mProtocolInfo);
                if (mGetTimeSeekRangeResult)
                  mURLextensions=mURLextensions.concat(",Duration=" + Double.toString(mTimeSeekRange[2] * 1000));
                headerResult.put(PLAYER_SET_KEY_DURATION, Double.toString(mTimeSeekRange[2] * 1000));
                if (mGetContentRangeResult)
                  headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentRange[2]));

              }
              if (mOP[RANGE_OP_INDEX] == '1') {
                //mURLextensions=mURLextensions.concat(",SeekType=Byte");
                httphead.removeHeaders(GET_RANGE);
                if (!mGetContentRangeResult) {
                  mGetContentRangeResult = getContentRange(mHeaders, GET_CONTENT_RANGE);
                  if (!mGetContentRangeResult)
                    mGetContentRangeResult=getContentRange(mHeaders, GET_CONTENT_RANGE_DTCP);
                }
                if (mGetContentRangeResult)
                  headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentRange[2]));

              }
            }
            if(!mGetContentRangeResult){
              Log.d(TAG, "We don't have ContentRange so lets try ContentLength");
              headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentLength));
              //mURLextensions=mURLextensions.concat(",&size=" + Long.toString(mContentLength));
              mURLextensions=mURLextensions.concat(",SIZE=" + Long.toString(mContentLength));

            }else{
              //mURLextensions=mURLextensions.concat(",&size=" + Long.toString(mContentRange[2]));
              mURLextensions=mURLextensions.concat(",SIZE=" + Long.toString(mContentLength));

            }
            mGetPSParamResult=getPSParam(mProtocolInfo);
            if (mGetPSParamResult) {
              mURLextensions = mURLextensions.concat(",PLAYSPEED="+mPsStringTotal);
              headerResult.put(PLAYSPEEDS, mPsStringTotal);
            }
						/*	mURLextensions=mURLextensions.concat(",PLAYSPEED=");
							Iterator<Double> i = mPsValue.iterator();
							while (i.hasNext())
								mURLextensions=mURLextensions.concat(Double.toString(i.next()));
								if (i.hasNext()){
									mURLextensions=mURLextensions.concat(",");
								}
						}*/

          }

        } catch (Exception e) {
          Log.e(TAG,"Error trying to initialize backend"+e);
        }
        headerResult.put(PLAYER_SET_KEY_PLAYMODE, "video");
      }


      private boolean mGetContentFeatures(HttpHead head, HttpClient client) {
        head.setHeader(GET_CONTENT_FEATURES, "1");
        mProtocolInfo="";
        mContentLength=-1;
        Header[] mHeaders;
        boolean result=false;
        try{

          Log.d(TAG, "before client.execute(head).getAllHeaders(); in mGetContentFeatures()");
          mHeaders = client.execute(head).getAllHeaders();
          Log.d(TAG, "after client.execute(head).getAllHeaders(); in mGetContentFeatures()");
          Log.d(TAG, "mHeaders: " + mHeaders);
          head.removeHeaders(GET_CONTENT_FEATURES);
          for(Header h:mHeaders){
            Log.d(TAG, "name: " + h.getName() + ", value: " + h.getValue());
            if (h.getName().toLowerCase().equals(CONTENT_FEATURES)){
              mProtocolInfo= h.getValue();
              Log.d(TAG,"Protocol Info: "+ mProtocolInfo);
              result=true;
            }
            if (h.getName().toLowerCase().equals(CONTENT_LENGTH)){
              try{
                mContentLength= Long.parseLong(h.getValue());
                Log.d(TAG,"PContent length: "+mContentLength);
              }catch(Exception e){
                Log.e(TAG,"Not a long integer in content length");
              }
            }
          }
        }catch (Exception e){
          Log.e(TAG,"Exception caught in getContentFeatures"+e);
        }
        return result;
      }

      private boolean mGetContentFeatures(String cdsData) {

        mProtocolInfo="";
        mContentLength=-1;
        Header[] mHeaders;
        boolean result=false;

        try{
          String[] cdsDataSplit=cdsData.split("&");
          if (cdsDataSplit.length==2) {
            mProtocolInfo = cdsDataSplit[0];
            mTimeSeekRange[2] = Double.valueOf(cdsDataSplit[1]);
            result = true;
          }
        }catch (Exception e){

          Log.e(TAG, "Error parsing cdsProtocolInfo: cdsData="+cdsData + "  error: "+e);
        }


        return result;
      }

      private boolean getOPFlags(String protocolInfo) {
        try{
          Pattern pattern = Pattern.compile(".*" + DLNA_ORG_OP + "=([01])([01]).*", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(protocolInfo);
          if(matcher.find()) {
            mOP[0] = matcher.group(1).charAt(0);
            mOP[1] = matcher.group(2).charAt(0);
            Log.d(TAG,"getOP parsing step4: "+mOP[0]+" , "+mOP[1]);
            return true;
          }
        }catch(Exception e){
          Log.e(TAG,"getOP string parsing exception: "+e);
        }
        return false;
      }

      private String getAKE(String tag, String url) {
        try{
          Pattern pattern = Pattern.compile(tag + "=([0-9.]+)", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(url);
          if(matcher.find()) {
            Log.d(TAG,"getAKE pattern found in "+url+" that is "+matcher.group(1));
            return matcher.group(1);
          }
          else{
            Log.d(TAG,"getAKE no pattern found in "+url);
            return "";
          }
        }catch(Exception e){
          Log.e(TAG,"getAKE string parsing exception: "+e);
        }
        return "";
      }

      private boolean getPSParam(String protocolInfo) {
        try{
          mPsStringTotal="";
          mPsString=new ArrayList<>();

          mPsValue=new ArrayList<>();
          double spval;
          Pattern pattern = Pattern.compile(".*" + DLNA_ORG_PS + "=(.*);", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(protocolInfo);
          if(matcher.find()) {
            String s="([^,]+),?";
            Pattern speedPattern = Pattern.compile(s);
            mPsStringTotal = matcher.group(1);
            Log.v(TAG, "getPSParam(): match string: " + mPsStringTotal);
            Matcher speedMatcher = speedPattern.matcher(mPsStringTotal);
            int n=0;
            while (speedMatcher.find()) {
              n++;
              String sp=speedMatcher.group(1);
              Log.v(TAG, "getPSParam(): Found speeds: " + sp);
              mPsString.add(sp);

              String[] fractions=sp.split("/");
              if (fractions.length==1) {
                spval= Double.parseDouble(sp);
                Log.v(TAG, "getPSParam(): Found speed double: " + spval);
                mPsValue.add(spval);

              }
              else if (fractions.length==2 && Double.parseDouble(fractions[1])>0){
                spval= Double.parseDouble(fractions[0])/ Double.parseDouble(fractions[1]);
                Log.v(TAG, "getPSParam(): Found speed double: " + spval);
                mPsValue.add(spval);
              }
              else{
                return false;
              }
            }
            if (mPsString.size()>0){
              Log.v(TAG,"Number of speeds found: "+mPsString.size());
              return true;
            }
          }
        }catch(Exception e){
          Log.e(TAG,"getPSParam() string parsing exception: "+e);
        }
        Log.v(TAG,"getPSParam(): Did not find pattern");
        return false;

      }

      private boolean getTimeSeekRange(Header[] headers, String headName){
        boolean result = false;
        try{
          Log.d(TAG, "getTimeSeekRange() starts");
          for(Header h:headers){
            Log.d(TAG, "looking for [" + headName + "]");
            Log.d(TAG, "getTimeSeekRange: name=" + h.getName() + ", value=" + h.getValue());
            if (h.getName().toLowerCase().equals(headName.toLowerCase())){
              Log.d(TAG, "value=" + h.getValue());
              Pattern pattern = Pattern.compile("(([0-9]+):)?(([0-9]+):)?([0-9]+(\\.[0-9]+)?)");
              Matcher matcher = pattern.matcher(h.getValue());
              for(int j = 0; j < 3 && matcher.find(); j++) {
                // for(int k = 1; k <= 6; k++) {
                //   Log.d(TAG, "group(" + k + ")=" + matcher.group(k));
                // }
                mTimeSeekRange[j] = 0;
                if(matcher.group(2) != null)
                  mTimeSeekRange[j] += Double.parseDouble(matcher.group(2)) * 3600f;
                if(matcher.group(4) != null)
                  mTimeSeekRange[j] += Double.parseDouble(matcher.group(4)) * 60f;
                mTimeSeekRange[j] += Double.parseDouble(matcher.group(5));
              }
              if (mTimeSeekRange[2]==-1.0)
                mTimeSeekRange[2]=mTimeSeekRange[1]; //Allow for x:y/* format, set end time to max seek
              Log.d(TAG, "mTimeSeekRange[3]: " + mTimeSeekRange[0] + ", " + mTimeSeekRange[1] + ", " + mTimeSeekRange[2]);
              // return true;
              result = true;
            }
          }
        }catch (Exception e){
          Log.e(TAG,"Exception caught in getTimeSeekRange"+e);
        }
        return result;
      }

      private boolean getContentRange(Header[] headers, String headName){
        try{
          for(Header h:headers){
            Log.d(TAG, "getContentRange: name=" + h.getName() + ", value=" + h.getValue());
            if (h.getName().toLowerCase().equals(headName.toLowerCase()) ){
              Pattern pattern = Pattern.compile(".*bytes[ =]*([0-9]+)-([0-9]+)/([0-9*]+).*");
              Matcher matcher = pattern.matcher(h.getValue());
              if(matcher.find()) {
                for(int i = 0; i < 3; i++) {
                  Log.d(TAG, "ContentRange matcher output" + matcher.group(i + 1));
                  if (!matcher.group(i + 1).startsWith("*"))
                    mContentRange[i] = Long.parseLong(matcher.group(i + 1));
                  else if (i==2)
                    Log.d(TAG, "ContentRange substituting * for max content seekable range");
                  mContentRange[2]= mContentRange[1]+1; //Allow for x:y/* format, set length to max seek range +1
                }

                Log.d(TAG, "Content Range start, end, range: "
                        + mContentRange[0] + " ,"  + mContentRange[1] + " ," + mContentRange[2]);
                return true;
              }
            }
          }

        }catch (Exception e){
          Log.e(TAG,"Exception caught in getContentFeatures"+e);
        }
        return false;
      }
    }




}
