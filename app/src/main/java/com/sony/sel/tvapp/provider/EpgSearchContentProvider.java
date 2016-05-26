package com.sony.sel.tvapp.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.sony.sel.tvapp.util.DlnaCache;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android content provider for searching EPG data.
 * This provider can be bound using "content://com.sony.sel.tvapp" URI.
 */
public class EpgSearchContentProvider extends ContentProvider {

  public static final String TAG = EpgSearchContentProvider.class.getSimpleName();
  private DlnaCache cache;
  private DlnaInterface dlnaHelper;
  private SettingsHelper settingsHelper;

  @Override
  public boolean onCreate() {
    Log.d(TAG, "Content provider created.");
    cache = DlnaHelper.getCache(getContext());
    settingsHelper = SettingsHelper.getHelper(getContext());
    dlnaHelper = DlnaHelper.getHelper(getContext());
    return true;
  }

  @Nullable
  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.d(TAG, "Query uri =  " + uri + ", projection = " + new Gson().toJson(projection) + ", selection = " + selection + ", selectionArgs = " + new Gson().toJson(selectionArgs) + ", sortOrder = " + sortOrder + ".");
    if (selection.startsWith("VOD")) {
      return searchVod(selectionArgs[0]);
    } else if (selection.startsWith("EPG")) {
      return searchEpg(selectionArgs[0]);
    } else {
      return null;
    }
  }

  Cursor searchEpg(String query) {
    Log.d(TAG, "Search EPG for \"" + query + "\".");
    List<VideoProgram> epgSearch = cache.search(settingsHelper.getEpgServer(), "0/EPG", query);
    List<VideoProgram> results = new ArrayList<>();
    Date now = new Date();
    for (VideoProgram program : epgSearch) {
      if (program.getScheduledEndTime().after(now)) {
        results.add(program);
      }
    }
    // sort EPG by date & time
    Collections.sort(results, new Comparator<VideoProgram>() {
      @Override
      public int compare(VideoProgram lhs, VideoProgram rhs) {
        return (lhs.getScheduledStartTime().compareTo(rhs.getScheduledStartTime()));
      }
    });
    Log.d(TAG, String.format("%d EPG results found.", results.size()));
    return new EpgSearchSuggestionsCursor(results, dlnaHelper.getChannels(settingsHelper.getEpgServer(), null, true));
  }

  Cursor searchVod(String query) {
    Log.d(TAG, "Search VOD for \"" + query + "\".");
    List<VideoProgram> vodSearch = cache.search(settingsHelper.getEpgServer(), "0/VOD", query);
    // sort VOD by title
    Collections.sort(vodSearch, new Comparator<DlnaObject>() {
      @Override
      public int compare(DlnaObject lhs, DlnaObject rhs) {
        return (lhs.getTitle().compareTo(rhs.getTitle()));
      }
    });
    Log.d(TAG, String.format("%d VOD results found.", vodSearch.size()));
    return new VodSearchSuggestionsCursor(vodSearch);
  }

  @Nullable
  @Override
  public String getType(Uri uri) {
    Log.d(TAG, "Get type for URI " + uri + ".");
    return null;
  }

  @Nullable
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    // not implemented
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    // not implemented
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    // not implemented
    return 0;
  }


  private static final String[] cols = new String[]{
      BaseColumns._ID,
      SearchManager.SUGGEST_COLUMN_TEXT_1,
      SearchManager.SUGGEST_COLUMN_TEXT_2,
      SearchManager.SUGGEST_COLUMN_ICON_1,
      SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
  };

  private class EpgSearchSuggestionsCursor extends AbstractCursor {

    private final List<VideoProgram> dlnaObjects;
    private final Map<String, VideoBroadcast> channelMap = new HashMap<>();

    public EpgSearchSuggestionsCursor(List<VideoProgram> dlnaObjects, List<VideoBroadcast> channels) {
      this.dlnaObjects = dlnaObjects;
      for (VideoBroadcast channel : channels) {
        channelMap.put(channel.getChannelId(), channel);
      }
    }

    @Override
    public int getCount() {
      return dlnaObjects.size();
    }

    @Override
    public String[] getColumnNames() {
      return cols;
    }

    @Override
    public String getString(int column) {
      VideoProgram data = dlnaObjects.get(getPosition());
      switch (column) {
        case 1:
          if (data.getProgramTitle() != null && data.getProgramTitle().length() > 0) {
            return data.getTitle() + ": " + data.getProgramTitle();
          } else {
            return data.getTitle();
          }
        case 2:
          DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
          DateFormat format = new SimpleDateFormat("h:mm");
          return channelMap.get(data.getChannelId()).getCallSign() + " " + dateFormat.format(data.getScheduledStartTime()) + ", " + format.format(data.getScheduledStartTime()) + "-" + format.format(data.getScheduledEndTime());
        case 3:
          if (data.getIcon() != null) {
            return data.getIcon();
          } else {
            return channelMap.get(data.getChannelId()).getIcon();
          }
        case 4:
          return data.getId();
        default:
          return null;
      }
    }

    @Override
    public short getShort(int column) {
      return 0;
    }

    @Override
    public int getInt(int column) {
      VideoProgram data = dlnaObjects.get(getPosition());
      switch (column) {
        case 0:
          return Integer.valueOf(data.getId());
        default:
          return 0;
      }
    }

    @Override
    public long getLong(int column) {
      return 0;
    }

    @Override
    public float getFloat(int column) {
      return 0;
    }

    @Override
    public double getDouble(int column) {
      return 0;
    }

    @Override
    public boolean isNull(int column) {
      return false;
    }
  }

  private class VodSearchSuggestionsCursor extends AbstractCursor {

    private final List<VideoProgram> dlnaObjects;

    public VodSearchSuggestionsCursor(List<VideoProgram> dlnaObjects) {
      this.dlnaObjects = dlnaObjects;
    }

    @Override
    public int getCount() {
      return dlnaObjects.size();
    }

    @Override
    public String[] getColumnNames() {
      return cols;
    }

    @Override
    public String getString(int column) {
      VideoProgram data = dlnaObjects.get(getPosition());
      switch (column) {
        case 1:
          if (data.getProgramTitle() != null && data.getProgramTitle().length() > 0) {
            return data.getTitle() + ": " + data.getProgramTitle();
          } else {
            return data.getTitle();
          }
        case 2:
          StringBuilder builder = new StringBuilder();
          if (data.getGenre() != null) {
            builder.append(data.getGenre());
          }
          if (data.getRating() != null) {
            if (builder.length() > 0) {
              builder.append(", ");
            }
            builder.append(data.getRating());
          }
          return builder.toString();
        case 3:
          return data.getIcon();
        case 4:
          return data.getId();
        default:
          return null;
      }
    }

    @Override
    public short getShort(int column) {
      return 0;
    }

    @Override
    public int getInt(int column) {
      DlnaObject data = dlnaObjects.get(getPosition());
      switch (column) {
        case 0:
          return data.hashCode();
        default:
          return 0;
      }
    }

    @Override
    public long getLong(int column) {
      return 0;
    }

    @Override
    public float getFloat(int column) {
      return 0;
    }

    @Override
    public double getDouble(int column) {
      return 0;
    }

    @Override
    public boolean isNull(int column) {
      return false;
    }
  }
}
