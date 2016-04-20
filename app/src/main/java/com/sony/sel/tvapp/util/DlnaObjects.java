package com.sony.sel.tvapp.util;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sony.huey.dlna.IconList;

import org.fourthline.cling.support.model.DIDLObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class for managing DLNA data.
 */
public class DlnaObjects {

  /**
   * Annotation for tagging data fields to extract from a cursor
   * <p>
   * Usage: @ColumnName(NAME) String field
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ColumnName {
    /**
     * Value is the name of the param
     */
    String value();
  }

  /// Date format for UTC dates/times
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");

  /**
   * Enumeration of all DLNA data classes.
   * Primary purpose is to support the {@link #newInstance(String)} factory method
   * to instantiate objects based upon the DLNA class name.
   */
  public enum DlnaClass {

    VIDEO_BROADCAST(VideoBroadcast.class, "object.item.videoItem.videoBroadcast"),
    VIDEO_ITEM(VideoItem.class, "object.item.videoItem"),


    VIDEO_PROGRAM(VideoProgram.class, "object.item.epgItem.videoProgram"),
    EPG_ITEM(EpgItem.class, "object.item.epgItem"),

    ITEM(Item.class, "object.item"),

    CONTAINER(Container.class, "object.container"),
    OBJECT(DlnaObject.class, "object");

    private final Class<DlnaObject> objectClass;
    private final String className;

    DlnaClass(Class objectClass, String className) {
      this.objectClass = objectClass;
      this.className = className;
    }

    /**
     * Return a new instance of a DlnaObject based upon the DLNA class name.
     *
     * @param className UPnP class name.
     * @return
     * @throws IllegalAccessException if constructor is not available.
     * @throws InstantiationException if another instantiation error occurs.
     */
    public static DlnaObject newInstance(@NonNull String className) throws IllegalAccessException, InstantiationException {
      for (DlnaClass dlnaClass : values()) {
        if (className.startsWith(dlnaClass.className)) {
          return dlnaClass.objectClass.newInstance();
        }
      }
      throw new InstantiationException("Class not resolved.");
    }

  }

  /**
   * Base class for objects that can be extracted from Cursors.
   * The {@link com.sony.sel.tvapp.util.DlnaObjects.ColumnName} annotation is used
   * to specify the column name in the cursor that should be extracted for the member contents.
   * <p>
   * The constructor takes care of parsing all data fields from the cursor.
   */
  public static class CursorObject {

    @ColumnName("_id")
    private String uid;
    @ColumnName("@id")
    private String id;

    /**
     * Load object contents from a cursor.
     *
     * @param cursor Cursor containing DLNA object data.
     */
    public final void loadFromCursor(Cursor cursor) {
      for (Class clazz = this.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
        for (Field field : clazz.getDeclaredFields()) {
          // allow access to private fields
          field.setAccessible(true);
          // check for column name annotations
          ColumnName annotation = field.getAnnotation(ColumnName.class);
          if (annotation != null) {
            String columnName = annotation.value();
            if (columnName != null) {
              int columnIndex = cursor.getColumnIndex(columnName);
              if (columnIndex >= 0) {
                try {
                  if (field.getType() == String.class) {
                    field.set(this, cursor.getString(columnIndex));
                  } else if (field.getType() == Integer.class) {
                    field.set(this, cursor.getInt(columnIndex));
                  } else if (field.getType() == byte[].class) {
                    field.set(this, cursor.getBlob(columnIndex));
                  }
                } catch (IllegalAccessException e) {
                  // TODO log access errors
                }
              }
            }
          }
        }
      }
    }

    /**
     * Return the list of Cursor column names expected from a given DLNA object class.
     *
     * @param clazz Class.
     * @param <T>   Class.
     * @return List of columns for use in Content Queries.
     */
    public static <T extends DlnaObject> String[] getColumnNames(Class<T> clazz) {
      List<String> columnNames = new ArrayList<>();
      for (Class c = clazz; c != Object.class; c = c.getSuperclass()) {
        for (Field field : c.getDeclaredFields()) {
          // allow access to private fields
          field.setAccessible(true);
          // check for column name annotations
          ColumnName annotation = field.getAnnotation(ColumnName.class);
          if (annotation != null) {
            String columnName = annotation.value();
            if (columnName != null) {
              columnNames.add(columnName);
            }
          }
        }
      }
      String[] columnArray = new String[columnNames.size()];
      columnNames.toArray(columnArray);
      return columnArray;
    }

    @Override
    public String toString() {
      return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof CursorObject ? id.equals(((CursorObject) o).id) : false;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    public String getUid() {
      return uid;
    }

    public String getId() {
      return id;
    }

    public void setUid(String uid) {
      this.uid = uid;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  /**
   * Class for UPnP devices when browsing the network
   */
  public static class UpnpDevice extends CursorObject {

    @ColumnName("LOCATION")
    private String location;
    @ColumnName("MODEL_NAME")
    private String modelName;
    @ColumnName("MODEL_NUMBER")
    private String modelNumber;
    @ColumnName("UDN")
    private String udn;
    @ColumnName("FRIENDLY_NAME")
    private String friendlyName;
    @ColumnName("DEVICE_TYPE")
    private String deviceType;
    @ColumnName("MANUFACTURER")
    private String manufacturer;
    @ColumnName("SERVICE_TYPE")
    private String serviceType;
    @ColumnName("ICONLIST")
    private byte[] iconList;

//    @ColumnName("HOST")
//    private String host;
//    @ColumnName("MAC_ADDRESS")
//    private String macAddress;

//    @ColumnName("PRESENTATION_URL")
//    private String presentationUrl;
//    @ColumnName("MANUFACTURER_URL")
//    private String manufacturerUrl;
//    @ColumnName("MODEL_DESCRIPTION")
//    private String modelDescription;
//    @ColumnName("MODEL_URL")
//    private String modelUrl;
//    @ColumnName("SERIAL_NUMBER")
//    private String serialNumber;
//    @ColumnName("UPC")
//    private String upc;
//    @ColumnName("X_DLNADOC")
//    private String dlnaDoc;
//    @ColumnName("X_DLNACAP")
//    private String dlnaCap;
//    @ColumnName("X_AV_PHYSIAL_UNIT_INFO")
//    private String avPhysicalUnitInfo;
//    @ColumnName("X_AV_SERVER_INFO")
//    private String avPhysicalServerInfo;
//    @ColumnName("STANDARD_CDS")
//    private String standardCds;
//    @ColumnName("VIDEO_ROOT")
//    private String videoRoot;
//    @ColumnName("MUSIC_ROOT")
//    private String musicRoot;
//    @ColumnName("PHOTO_ROOT")
//    private String photoRoot;
//    @ColumnName("VIDEO_AUTO_SYNC_CONTAINER")
//    private String videoAudioSyncContainer;
//    @ColumnName("VIDEO_LIVE_TUNER_CONTAINER")
//    private String videoLiveTunerContainer;
//    @ColumnName("X_WAKEUP_ON_LAN")
//    private String wakeupOnLan;
//    @ColumnName("MAX_BGMCOUNT")
//    private String maxBgmCount;
//    @ColumnName("STANDARD_DMR")
//    private String standardDmr;
//    @ColumnName("DEVICE_STATE")
//    private String deviceState;
//    @ColumnName("DEVICE_ERROR_CODE")
//    private String deviceErrorCode;
//    @ColumnName("IsXSRSSupported")
//    private String isXrssSupported;
//    @ColumnName("IsPMHServer")
//    private String isPmhServer;
//    @ColumnName("CHILD_COUNT")
//    private String childCount;

    public String getLocation() {
      return location;
    }

    public String getModelName() {
      return modelName;
    }

    public String getModelNumber() {
      return modelNumber;
    }

    public String getUdn() {
      return udn;
    }

    public String getFriendlyName() {
      return friendlyName;
    }

    public String getDeviceType() {
      return deviceType;
    }

    public String getManufacturer() {
      return manufacturer;
    }

    public String getServiceType() {
      return serviceType;
    }

    public IconList getIconList() {
      return IconList.blob2IconList(iconList);
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public void setModelNumber(String modelNumber) {
      this.modelNumber = modelNumber;
    }

    public void setUdn(String udn) {
      this.udn = udn;
    }

    public void setFriendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
    }

    public void setDeviceType(String deviceType) {
      this.deviceType = deviceType;
    }

    public void setManufacturer(String manufacturer) {
      this.manufacturer = manufacturer;
    }

    public void setServiceType(String serviceType) {
      this.serviceType = serviceType;
    }

    public void setIconList(byte[] iconList) {
      this.iconList = iconList;
    }
  }

  public static class DlnaObject extends CursorObject {

    //    @ColumnName("_num_")
//    private String responseOrder;
//    @ColumnName("@parentID")
//    private String parentId;
//    @ColumnName("@restricted")
//    private String restricted;
    @ColumnName("dc:title")
    private String title;
    @ColumnName("upnp:class")
    private String upnpClass;
    @ColumnName("res")
    private String res;
    @ColumnName("res@protocolInfo")
    private String protocolInfo;
    @ColumnName("upnp:icon")
    private String icon;

    public String getTitle() {
      return title;
    }

    public String getUpnpClass() {
      return upnpClass;
    }

    public String getResource() {
      return res;
    }

    public String getProtocolInfo() {
      return protocolInfo;
    }

    public String getIcon() {
      return icon;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public void setUpnpClass(String upnpClass) {
      this.upnpClass = upnpClass;
    }

    public void setRes(String res) {
      this.res = res;
    }

    public void setProtocolInfo(String protocolInfo) {
      this.protocolInfo = protocolInfo;
    }

    public void setIcon(String icon) {
      this.icon = icon;
    }
  }

  /**
   * Base class for all DLNA items.
   */
  public static class Item extends DlnaObject {

//    @ColumnName("@refID")
//    private String refId;
//    @ColumnName("upnp:bookmarkID")
//    private String bookmarkId;

  }

  /**
   * Base class for EPG items, a.k.a. programs or VOD items.
   */
  public static class EpgItem extends Item {

    //    @ColumnName("upnp:channelGroupName")
//    private String channelGroupName;
//    @ColumnName("upnp:channelGroupName@id")
//    private String channelGroupNameId;
//    @ColumnName("upnp:serviceProvider")
//    private String serviceProvider;
    @ColumnName("upnp:channelName")
    private String channelName;
    @ColumnName("upnp:channelNr")
    private String channelNumber;
    @ColumnName("upnp:programTitle")
    private String programTitle;
    @ColumnName("upnp:seriesTitle")
    private String seriesTitle;
    //    @ColumnName("upnp:programID")
//    private String programId;
//    @ColumnName("pnp:programID@type")
//    private String programIdType;
//    @ColumnName("upnp:seriesID")
//    private String seriesId;
//    @ColumnName("upnp:seriesID@type")
//    private String seriesIdType;
    @ColumnName("upnp:channelID")
    private String channelId;
    //    @ColumnName("upnp:channelID@type")
//    private String channelIdType;
//    @ColumnName("upnp:episodeCount")
//    private String eposodeCount;
//    @ColumnName("upnp:episodeNumber")
//    private String episodeNumber;
//    @ColumnName("upnp:programCode")
//    private String programCode;
//    @ColumnName("upnp:programCode@type")
//    private String programCodeType;
    @ColumnName("upnp:rating")
    private String rating;
    //    @ColumnName("upnp:rating@type")
//    private String ratingType;
//    @ColumnName("upnp:episodeType")
//    private String episodeType;
    @ColumnName("upnp:genre")
    private String genre;
    //    @ColumnName("upnp:genre@id")
//    private String genreId;
//    @ColumnName("upnp:genre@extended")
//    private String genreExtended;
//    @ColumnName("upnp:artist")
//    private String artist;
//    @ColumnName("upnp:artist@role")
//    private String artistRole;
//    @ColumnName("upnp:actor")
//    private String actor;
//    @ColumnName("upnp:actor@role")
//    private String actorRole;
//    @ColumnName("upnp:author")
//    private String author;
//    @ColumnName("upnp:author@role")
//    private String authorRole;
//    @ColumnName("upnp:producer")
//    private String producer;
//    @ColumnName("upnp:director")
//    private String director;
//    @ColumnName("dc:publisher")
//    private String publisher;
//    @ColumnName("dc:contributor")
//    private String contributor;
    @ColumnName("upnp:callSign")
    private String callSign;
    @ColumnName("upnp:networkAffiliation")
    private String networkAffiliation;
    //    @ColumnName("upnp:serviceProvider")
//    private String servideProvider;
//    @ColumnName("upnp:price")
//    private String price;
//    @ColumnName("upnp:price@currency")
//    private String priceCurrency;
//    @ColumnName("upnp:payPerView")
//    private String payPerView;
//    @ColumnName("upnp:epgProviderName")
//    private String epgProviderName;
    @ColumnName("dc:description")
    private String description;
    @ColumnName("upnp:longDescription")
    private String longDescription;
    //    @ColumnName("upnp:region")
//    private String region;
//    @ColumnName("dc:language")
//    private String language;
//    @ColumnName("dc:relation")
//    private String relation;
    @ColumnName("upnp:scheduledStartTime")
    private String scheduledStartTime;
    @ColumnName("upnp:scheduledEndTime")
    private String scheduledEndTime;
//    @ColumnName("upnp:recordable")
//    private String recordable;

    public String getChannelName() {
      return channelName;
    }

    public String getChannelNumber() {
      return channelNumber;
    }

    public String getProgramTitle() {
      return programTitle;
    }

    public String getSeriesTitle() {
      return seriesTitle;
    }

    public String getChannelId() {
      return channelId;
    }

    public String getCallSign() {
      return callSign;
    }

    public String getNetworkAffiliation() {
      return networkAffiliation;
    }

    public String getDescription() {
      return description;
    }

    public String getLongDescription() {
      return longDescription;
    }

    public Date getScheduledStartTime() {
      if (scheduledStartTime != null) {
        try {
          return DATE_FORMAT.parse(scheduledStartTime);
        } catch (ParseException e) {
          return null;
        }
      } else {
        return null;
      }
    }

    public Date getScheduledEndTime() {
      if (scheduledEndTime != null) {
        try {
          return DATE_FORMAT.parse(scheduledEndTime);
        } catch (ParseException e) {
          return null;
        }
      } else {
        return null;
      }
    }

    public String getGenre() {
      return genre;
    }

    public String getRating() {
      return rating;
    }

    public void setChannelName(String channelName) {
      this.channelName = channelName;
    }

    public void setChannelNumber(String channelNumber) {
      this.channelNumber = channelNumber;
    }

    public void setProgramTitle(String programTitle) {
      this.programTitle = programTitle;
    }

    public void setSeriesTitle(String seriesTitle) {
      this.seriesTitle = seriesTitle;
    }

    public void setChannelId(String channelId) {
      this.channelId = channelId;
    }

    public void setRating(String rating) {
      this.rating = rating;
    }

    public void setGenre(String genre) {
      this.genre = genre;
    }

    public void setCallSign(String callSign) {
      this.callSign = callSign;
    }

    public void setNetworkAffiliation(String networkAffiliation) {
      this.networkAffiliation = networkAffiliation;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void setLongDescription(String longDescription) {
      this.longDescription = longDescription;
    }

    public void setScheduledStartTime(String scheduledStartTime) {
      this.scheduledStartTime = scheduledStartTime;
    }

    public void setScheduledEndTime(String scheduledEndTime) {
      this.scheduledEndTime = scheduledEndTime;
    }
  }

  /**
   * Class for video programs, a.k.a. TV shows.
   */
  public static class VideoProgram extends EpgItem {

    /**
     * Gson serializer that adds keys & values expected by JavaScript code.
     */
    public static class WebSerializer implements JsonSerializer<VideoProgram> {
      @Override
      public JsonElement serialize(VideoProgram src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new Gson().toJsonTree(src).getAsJsonObject();
        obj.addProperty("start", String.valueOf(src.getScheduledStartTime().getTime()));
        obj.addProperty("length", String.valueOf(src.getScheduledEndTime().getTime() - src.getScheduledStartTime().getTime()));
        obj.addProperty("description", src.getLongDescription());
        obj.addProperty("programIcon", src.getIcon());
        return obj;
      }
    }

  }

  /**
   * Base class for all video items.
   */
  public static class VideoItem extends Item {

    //    @ColumnName("upnp:genre")
//    private String genre;
//    @ColumnName("upnp:genre@id")
//    private String genreId;
//    @ColumnName("upnp:genre@type")
//    private String genreType;
    @ColumnName("upnp:longDescription")
    private String longDescription;
    //    @ColumnName("upnp:producer")
//    private String producer;
//    @ColumnName("upnp:rating")
//    private String rating;
//    @ColumnName("upnp:actor")
//    private String actor;
//    @ColumnName("upnp:director")
//    private String director;
    @ColumnName("dc:description")
    private String description;
//    @ColumnName("dc:publisher")
//    private String publisher;
//    @ColumnName("dc:language")
//    private String language;
//    @ColumnName("dc:relation")
//    private String relation;
//    @ColumnName("upnp:playbackCount")
//    private String playbackCount;
//    @ColumnName("upnp:lastPlaybackTime")
//    private String lastPlaybackTime;
//    @ColumnName("upnp:lastPlaybackPosition")
//    private String lastPlaybackPosition;
//    @ColumnName("upnp:recordedDayOfWeek")
//    private String recordedDayOfWeek;
//    @ColumnName("upnp:srsRecordScheduleID")
//    private String srsRecordScheduleId;

    public String getLongDescription() {
      return longDescription;
    }

    public String getDescription() {
      return description;
    }

    public void setLongDescription(String longDescription) {
      this.longDescription = longDescription;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /**
   * Class for video broadcasts, a.k.a. TV channels.
   */
  public static class VideoBroadcast extends VideoItem {

    //    @ColumnName("upnp:region")
//    private String region;
    @ColumnName("upnp:channelNr")
    private String channelNumber;
    //    @ColumnName("upnp:signalStrength")
//    private String signalStrength;
//    @ColumnName("upnp:signalLocked")
//    private String signalLocked;
//    @ColumnName("upnp:tuned")
//    private String tuned;
//    @ColumnName("upnp:recordable")
//    private String recordable;
    @ColumnName("upnp:callSign")
    private String callSign;
//    @ColumnName("upnp:price")
//    private String price;
//    @ColumnName("upnp:payPerView")
//    private String payPerView;

    public String getChannelNumber() {
      return channelNumber;
    }

    public String getCallSign() {
      if (callSign != null && callSign.startsWith("x")) {
        return callSign.substring(1);
      }
      return callSign;
    }

    public String getChannelId() {
      if (getId() != null) {
        return getId().split("/")[2];
      }
      return null;
    }

    public void setChannelNumber(String channelNumber) {
      this.channelNumber = channelNumber;
    }

    public void setCallSign(String callSign) {
      this.callSign = callSign;
    }

    /**
     * Gson serializer that adds keys & values expected by JavaScript code.
     */
    public static class WebSerializer implements JsonSerializer<VideoBroadcast> {
      @Override
      public JsonElement serialize(VideoBroadcast src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new Gson().toJsonTree(src).getAsJsonObject();
        obj.addProperty("callSign", src.getCallSign());
        obj.addProperty("channelID", src.getChannelId());
        obj.addProperty("channelIcon", src.getIcon());
        return obj;
      }
    }
  }

  /**
   * Base class for all DLNA containers that have child objects.
   */
  public static class Container extends DlnaObject {

//    @ColumnName("@childCount")
//    private String childCount;
//    @ColumnName("upnp:createClass")
//    private String createClass;
//    @ColumnName("upnp:searchClass")
//    private String searchClass;
//    @ColumnName("@searchable")
//    private String searchable;
//    @ColumnName("@neverPlayable")
//    private String neverPlayable;

  }

  /**
   * Class for EPG container items, a.k.a EPG "channels" and "days"
   */
  public static class EpgContainer extends Container {

    public static final String CLASS = "object.container.epgContainer";

    //    @ColumnName("upnp:channelGroupName")
//    private String channelGroupName;
//    @ColumnName("upnp:channelGroupName@id")
//    private String channelGroupNameId;
//    @ColumnName("upnp:serviceProvider")
//    private String seviceProvider;
//    @ColumnName("upnp:channelName")
//    private String channelName;
//    @ColumnName("upnp:channelNr")
//    private String channelNr;
//    @ColumnName("upnp:channelID")
//    private String channelId;
//    @ColumnName("upnp:channelID@type")
//    private String channelIdType;
//    @ColumnName("upnp:radioCallSign")
//    private String radioCallSign;
//    @ColumnName("upnp:radioStationID")
//    private String radioStationId;
//    @ColumnName("upnp:radioBand")
//    private String radioBand;
//    @ColumnName("upnp:callSign")
//    private String callSign;
//    @ColumnName("upnp:networkAffiliation")
//    private String networkAffiliation;
//    @ColumnName("upnp:serviceProvider")
//    private String serviceProvider;
//    @ColumnName("upnp:price")
//    private String price;
//    @ColumnName("upnp:price@currency")
//    private String priceCurrency;
//    @ColumnName("upnp:payPerView")
//    private String payPerView;
//    @ColumnName("upnp:epgProviderName")
//    private String epgProviderName;
    //    @ColumnName("upnp:region")
//    private String region;
//    @ColumnName("dc:language")
//    private String language;
//    @ColumnName("dc:relation")
//    private String relation;
    @ColumnName("upnp:dateTimeRange")
    private String dateTimeRange;

    public Date getDateTimeRangeStart() {
      if (dateTimeRange != null) {
        String[] dates = dateTimeRange.split("/");
        try {
          return DATE_FORMAT.parse(dates[0]);
        } catch (ParseException e) {
          return null;
        }
      }
      return null;
    }

    public Date getDateTimeRangeEnd() {
      if (dateTimeRange != null) {
        String[] dates = dateTimeRange.split("/");
        try {
          return DATE_FORMAT.parse(dates[1]);
        } catch (ParseException e) {
          return null;
        }
      }
      return null;
    }
  }
}

