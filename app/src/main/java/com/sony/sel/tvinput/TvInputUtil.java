package com.sony.sel.tvinput;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Utilities for managing TV Input, Channels, EPG Data, etc.
 */
public class TvInputUtil {

  public static final String TAG = TvInputUtil.class.getSimpleName();

  private Context context;
  private DlnaInterface dlnaHelper;
  private DlnaCache dlnaCache;
  private SettingsHelper settingsHelper;

  public TvInputUtil(String inputId, Context context) {
    this.context = context;
    dlnaHelper = DlnaHelper.getHelper(context);
    dlnaCache = DlnaHelper.getCache(context);
    settingsHelper = SettingsHelper.getHelper(context);
    settingsHelper.setInputId(inputId);
  }

  public TvInputUtil(Context context) {
    this.context = context;
    dlnaHelper = DlnaHelper.getHelper(context);
    dlnaCache = DlnaHelper.getCache(context);
    settingsHelper = SettingsHelper.getHelper(context);

  }

  public void registerChannels(List<VideoBroadcast> channels) {
    Log.d(TAG, "registerChannels");
    Uri uri = TvContract.buildChannelsUriForInput(settingsHelper.getInputId());
    Cursor cursor = null;
    try {
      cursor = context.getContentResolver().query(uri, null, null, null, null);
      if (cursor != null && cursor.getCount() > 0) {
        return;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    try {
      ContentValues values = new ContentValues();
      values.put(TvContract.Channels.COLUMN_INPUT_ID, settingsHelper.getInputId());
      Log.d(TAG, "registering channels.  Num channes: " + channels.size());
      for (VideoBroadcast x : channels) {
        values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, x.getChannelNumber());
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, x.getCallSign());
        values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, x.getResMimeType());
        //values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, x.getNetworkId());
        values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, x.getResource());
        values.put(TvContract.Channels.COLUMN_SERVICE_ID, x.getChannelId());
        values.put(TvContract.Channels._ID, Integer.valueOf(x.getChannelId()));
        Uri channelUri = context.getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);
        //x.setChannelUri(channelUri);
        if (channelUri != null) {
          //x.setChannelId(ContentUris.parseId(channelUri));
          writeChannelLogo(ContentUris.parseId(channelUri), x.getIcon());
        }
      }


    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void addProgramData() {

    Log.d(TAG, "Adding program data to Android TV database.");

    final String udn = settingsHelper.getEpgServer();

    Calendar calendar = Calendar.getInstance();
    Date startTime = calendar.getTime();
    // TODO manage  EPG data further ahead for "real" release.
    // However, too much data in one request crashes the binder.
    calendar.add(Calendar.HOUR_OF_DAY, 6);
    Date endTime = calendar.getTime();

    // create channel list
    List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);
    Log.d(TAG, "Found " + channels.size() + " channels.");
    List<String> channelIds = new ArrayList<>();
    for (VideoBroadcast channel : channels) {
      channelIds.add(channel.getChannelId());
    }

    // query cache for EPG data
    List<VideoProgram> programData = dlnaCache.searchEpg(udn, channelIds, startTime, endTime);
    Log.d(TAG, "Found " + programData.size() + " EPG programs.");

    // iterate epg data
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    for (VideoProgram y : programData) {
      ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(videoProgramToContentValues(y)).build());
    }

    // apply batch process to content provider
    try {
      Log.d(TAG, "Adding " + ops.size() + " EPG programs to Android TV database.");
      context.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private ContentValues videoProgramToContentValues(VideoProgram videoProgram) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(TvContract.Programs.COLUMN_CHANNEL_ID, Integer.valueOf(videoProgram.getChannelId()));
    // primary EPG title, top line of EPG cells
    contentValues.put(TvContract.Programs.COLUMN_TITLE, videoProgram.getTitle());
    contentValues.put(TvContract.Programs.COLUMN_EPISODE_TITLE, videoProgram.getTitle());
    if (videoProgram.getEpisodeNumber() != null) {
      contentValues.put(TvContract.Programs.COLUMN_EPISODE_NUMBER, Integer.valueOf(videoProgram.getEpisodeNumber()));
    }
    if (videoProgram.getEpisodeSeason() != null) {
      contentValues.put(TvContract.Programs.COLUMN_SEASON_NUMBER, Integer.valueOf(videoProgram.getEpisodeSeason()));
    }
    // long description for details box
    contentValues.put(TvContract.Programs.COLUMN_LONG_DESCRIPTION, videoProgram.getLongDescription());
    // short description for second line of EPG cells and second line description when channel flipping
    contentValues.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, (videoProgram.getProgramTitle() != null && videoProgram.getProgramTitle().length() > 0) ? videoProgram.getProgramTitle() : videoProgram.getLongDescription());
    // start & end time, already in UTC milliseconds from cache
    contentValues.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, videoProgram.getScheduledStartTime().getTime());
    contentValues.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, videoProgram.getScheduledEndTime().getTime());
    if (videoProgram.getIcon() != null) {
      Uri icon_uri = Uri.parse(videoProgram.getIcon());
      contentValues.put(TvContract.Programs.COLUMN_POSTER_ART_URI, icon_uri.toString());
      contentValues.put(TvContract.Programs.COLUMN_THUMBNAIL_URI, icon_uri.toString());
    }
    return contentValues;
  }

  public void writeChannelLogo(long channelId, String iconUrl) {
    if (channelId >= 0)
      Log.d(TAG, "channelId: " + channelId);
    if (iconUrl != null)
      Log.d(TAG, "iconUrl: " + iconUrl);
    if (channelId <= 0 || iconUrl == null) {
      Log.d(TAG, "no channel id or url, not fetching channel logo");
      return;
    }
    ArrayList<String> parameters = new ArrayList<String>();
    parameters.add(Long.toString(channelId));
    parameters.add(iconUrl);
    new createStationImage().execute(parameters);
  }

  public void createChannelLogo(long channelId, String iconUrl) {
    byte[] logo = convertImageUrlToBitmap(iconUrl);
    if (logo == null) {
      return;
    }
    Uri channelLogoUri = TvContract.buildChannelLogoUri(channelId);
    try {
      AssetFileDescriptor fd =
          context.getContentResolver().openAssetFileDescriptor(channelLogoUri, "rw");
      OutputStream os = fd.createOutputStream();
      os.write(logo);
      os.close();
      fd.close();
      Log.d(TAG, "created channel logo for channelId: " + channelId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] convertImageUrlToBitmap(String imageUrl) {
    Bitmap bitmap = null;
    try {
      URL url = new URL(imageUrl);
      bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
      return convertBitmapToBytes(bitmap);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private byte[] convertBitmapToBytes(Bitmap bitmap) {
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
      byte[] byteArray = stream.toByteArray();
      return byteArray;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private class createStationImage extends AsyncTask<ArrayList<String>, Void, Void> {

    @Override
    protected Void doInBackground(ArrayList<String>... params) {
      Log.d(TAG, "parameters length:" + params.length);
            /*for(ArrayList<String> x: params)
            {
                //Log.d(TAG,);
            }*/
      ArrayList<String> passed = params[0];
      Log.d(TAG, "passed size: " + passed.size());
      if (passed.size() > 1) {
        String idString = passed.get(0);
        Log.d(TAG, "id received is: " + idString);
        Long id = Long.parseLong(idString);
        String iconUrl = passed.get(1);
        Log.d(TAG, "icon url received:" + iconUrl);
//                Long id = Long.getLong(passed.get(0));//(passed.get(0));
        createChannelLogo(id, iconUrl);
      }
      return null;
    }
  }

}
