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

import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.SettingsHelper;

import org.fourthline.cling.support.messagebox.model.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by breeze on 4/12/16.
 */
public class InputChannelsUtil {
  public static final String TAG = "InputChannelsUtil";
  private String mInputId;
  private Context mContext;
  public static final String inputIdFile = "input_id";
  private DlnaInterface dlnaHelper;
  private String udn;
  private SettingsHelper settingsHelper;
  boolean convertEpgtimeToToday = false;

  public InputChannelsUtil(String inputId, Context context) {
    mInputId = inputId;
    mContext = context;
    dlnaHelper = DlnaHelper.getHelper(context);
    settingsHelper = SettingsHelper.getHelper(context);
    udn = settingsHelper.getEpgServer();
    if (udn == null) {
      Log.e(TAG, "failed to get epg server udn");
    }

  }

  public InputChannelsUtil(Context context) {
    mContext = context;
    dlnaHelper = DlnaHelper.getHelper(context);
    getInputIdFromFile();
    settingsHelper = SettingsHelper.getHelper(context);
    udn = settingsHelper.getEpgServer();
    if (udn == null) {
      Log.e(TAG, "failed to get epg server udn");
    }
  }


  public void registerChannels(List<VideoBroadcast> channels) {
    Log.d(TAG, "registerChannels");
    Uri uri = TvContract.buildChannelsUriForInput(mInputId);
    Cursor cursor = null;
    try {
      cursor = mContext.getContentResolver().query(uri, null, null, null, null);
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
      values.put(TvContract.Channels.COLUMN_INPUT_ID, mInputId);
      Log.d(TAG, "registering channels.  Num channes: " + channels.size());
      for (VideoBroadcast x : channels) {
        values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, x.getChannelNumber());
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, x.getCallSign());
        values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, x.getResMimeType());
        //values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, x.getNetworkId());
        values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, x.getResource());
        values.put(TvContract.Channels.COLUMN_SERVICE_ID, x.getChannelId());
        values.put(TvContract.Channels._ID, x.getChannelId());
        Uri channelUri = mContext.getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);
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

  public void saveInputId(String inputId) {
    mInputId = inputId;
    saveObjectToFile(inputIdFile, inputId);
  }

  private void getInputIdFromFile() {
    mInputId = (String) getObjectFromFile(inputIdFile);
  }

  private void saveObjectToFile(String filename, Object o) {
    try {
      FileOutputStream fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
      ObjectOutputStream os = new ObjectOutputStream(fos);
      os.writeObject(o);
      os.close();
      Log.d(TAG, "object saved to file");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Object getObjectFromFile(String file) {
    Object object = null;
    try {
      FileInputStream fileInputStream = mContext.openFileInput(file);
      ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
      object = objectInputStream.readObject();
      objectInputStream.close();
      fileInputStream.close();
      Log.d(TAG, "loaded object from file");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return object;
  }


  public void addProgramData() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, -1);
    Date startTime = calendar.getTime();
    calendar.add(Calendar.HOUR_OF_DAY, 13);
    Date endTime = calendar.getTime();

    List<VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);

    for (VideoBroadcast x : channels) {
      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      List<DlnaObjects.VideoProgram> programData = dlnaHelper.getEpgPrograms(udn, x, startTime, endTime);
      for (DlnaObjects.VideoProgram y : programData) {
        ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI).withValues(videoProgramToContentValues(y)).build());
      }
      try {
        Log.d(TAG, "adding programming data to system");
        mContext.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }


  }

  private ContentValues videoProgramToContentValues(DlnaObjects.VideoProgram videoProgram) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(TvContract.Programs.COLUMN_CHANNEL_ID, videoProgram.getChannelId());
    contentValues.put(TvContract.Programs.COLUMN_TITLE, videoProgram.getTitle());
    contentValues.put(TvContract.Programs.COLUMN_LONG_DESCRIPTION, videoProgram.getLongDescription());
    contentValues.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, videoProgram.getDescription());

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
          mContext.getContentResolver().openAssetFileDescriptor(channelLogoUri, "rw");
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
