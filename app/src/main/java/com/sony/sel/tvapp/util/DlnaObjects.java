package com.sony.sel.tvapp.util;

import android.database.Cursor;

import com.google.gson.Gson;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class to hold DLNA object declarations.
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

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");

  public static class DlnaObject implements Serializable {

    @ColumnName("_id")
    private String uid;
    @ColumnName("@id")
    private String id;
    @ColumnName("_num_")
    private String responseOrder;
    @ColumnName("@parentID")
    private String parentId;
    @ColumnName("@restricted")
    private String restricted;
    @ColumnName("dc:title")
    private String title;
    @ColumnName("upnp:class")
    private String upnpClass;

    public DlnaObject(Cursor cursor) {
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

    public String getUid() {
      return uid;
    }

    public String getId() {
      return id;
    }

    public String getResponseOrder() {
      return responseOrder;
    }

    public String getParentId() {
      return parentId;
    }

    public String getRestricted() {
      return restricted;
    }

    public String getTitle() {
      return title;
    }

    public String getUpnpClass() {
      return upnpClass;
    }

    @Override
    public String toString() {
      return new Gson().toJson(this);
    }
  }

  /**
   * Base class for all DLNA items.
   */
  public static class Item extends DlnaObject {

    @ColumnName("@refID")
    private String refId;
    @ColumnName("upnp:bookmarkID")
    private String bookmarkId;

    public Item(Cursor cursor) {
      super(cursor);
    }

    public String getRefId() {
      return refId;
    }

    public String getBookmarkId() {
      return bookmarkId;
    }
  }

  /**
   * Base class for EPG items, a.k.a. programs or VOD items.
   */
  public static class EpgItem extends Item {

    @ColumnName("upnp:channelGroupName")
    private String channelGroupName;
    @ColumnName("upnp:channelGroupName@id")
    private String channelGroupNameId;
    @ColumnName("upnp:serviceProvider")
    private String serviceProvider;
    @ColumnName("upnp:channelName")
    private String channelName;
    @ColumnName("upnp:channelNr")
    private String channelNr;
    @ColumnName("upnp:programTitle")
    private String programTitle;
    @ColumnName("upnp:seriesTitle")
    private String seriesTitle;
    @ColumnName("upnp:programID")
    private String programId;
    @ColumnName("pnp:programID@type")
    private String programIdType;
    @ColumnName("upnp:seriesID")
    private String seriesId;
    @ColumnName("upnp:seriesID@type")
    private String seriesIdType;
    @ColumnName("upnp:channelID")
    private String channelId;
    @ColumnName("upnp:channelID@type")
    private String channelIdType;
    @ColumnName("upnp:episodeCount")
    private String eposodeCount;
    @ColumnName("upnp:episodeNumber")
    private String episodeNumber;
    @ColumnName("upnp:programCode")
    private String programCode;
    @ColumnName("upnp:programCode@type")
    private String programCodeType;
    @ColumnName("upnp:rating")
    private String rating;
    @ColumnName("upnp:rating@type")
    private String ratingType;
    @ColumnName("upnp:episodeType")
    private String episodeType;
    @ColumnName("upnp:genre")
    private String genre;
    @ColumnName("upnp:genre@id")
    private String genreId;
    @ColumnName("upnp:genre@extended")
    private String genreExtended;
    @ColumnName("upnp:artist")
    private String artist;
    @ColumnName("upnp:artist@role")
    private String artistRole;
    @ColumnName("upnp:actor")
    private String actor;
    @ColumnName("upnp:actor@role")
    private String actorRole;
    @ColumnName("upnp:author")
    private String author;
    @ColumnName("upnp:author@role")
    private String authorRole;
    @ColumnName("upnp:producer")
    private String producer;
    @ColumnName("upnp:director")
    private String director;
    @ColumnName("dc:publisher")
    private String publisher;
    @ColumnName("dc:contributor")
    private String contributor;
    @ColumnName("upnp:callSign")
    private String callSign;
    @ColumnName("upnp:networkAffiliation")
    private String networkAffiliation;
    @ColumnName("upnp:serviceProvider")
    private String servideProvider;
    @ColumnName("upnp:price")
    private String price;
    @ColumnName("upnp:price@currency")
    private String priceCurrency;
    @ColumnName("upnp:payPerView")
    private String payPerView;
    @ColumnName("upnp:epgProviderName")
    private String epgProviderName;
    @ColumnName("dc:description")
    private String description;
    @ColumnName("upnp:longDescription")
    private String longDescription;
    @ColumnName("upnp:icon")
    private String icon;
    @ColumnName("upnp:region")
    private String region;
    @ColumnName("dc:language")
    private String language;
    @ColumnName("dc:relation")
    private String relation;
    @ColumnName("upnp:scheduledStartTime")
    private String scheduledStartTime;
    @ColumnName("upnp:scheduledEndTime")
    private String scheduledEndTime;
    @ColumnName("upnp:recordable")
    private String recordable;

    public EpgItem(Cursor cursor) {
      super(cursor);
    }

    public String getChannelGroupName() {
      return channelGroupName;
    }

    public String getChannelGroupNameId() {
      return channelGroupNameId;
    }

    public String getServiceProvider() {
      return serviceProvider;
    }

    public String getChannelName() {
      return channelName;
    }

    public String getChannelNr() {
      return channelNr;
    }

    public String getProgramTitle() {
      return programTitle;
    }

    public String getSeriesTitle() {
      return seriesTitle;
    }

    public String getProgramId() {
      return programId;
    }

    public String getProgramIdType() {
      return programIdType;
    }

    public String getSeriesId() {
      return seriesId;
    }

    public String getSeriesIdType() {
      return seriesIdType;
    }

    public String getChannelId() {
      return channelId;
    }

    public String getChannelIdType() {
      return channelIdType;
    }

    public String getEposodeCount() {
      return eposodeCount;
    }

    public String getEpisodeNumber() {
      return episodeNumber;
    }

    public String getProgramCode() {
      return programCode;
    }

    public String getProgramCodeType() {
      return programCodeType;
    }

    public String getRating() {
      return rating;
    }

    public String getRatingType() {
      return ratingType;
    }

    public String getEpisodeType() {
      return episodeType;
    }

    public String getGenre() {
      return genre;
    }

    public String getGenreId() {
      return genreId;
    }

    public String getGenreExtended() {
      return genreExtended;
    }

    public String getArtist() {
      return artist;
    }

    public String getArtistRole() {
      return artistRole;
    }

    public String getActor() {
      return actor;
    }

    public String getActorRole() {
      return actorRole;
    }

    public String getAuthor() {
      return author;
    }

    public String getAuthorRole() {
      return authorRole;
    }

    public String getProducer() {
      return producer;
    }

    public String getDirector() {
      return director;
    }

    public String getPublisher() {
      return publisher;
    }

    public String getContributor() {
      return contributor;
    }

    public String getCallSign() {
      return callSign;
    }

    public String getNetworkAffiliation() {
      return networkAffiliation;
    }

    public String getServideProvider() {
      return servideProvider;
    }

    public String getPrice() {
      return price;
    }

    public String getPriceCurrency() {
      return priceCurrency;
    }

    public String getPayPerView() {
      return payPerView;
    }

    public String getEpgProviderName() {
      return epgProviderName;
    }

    public String getDescription() {
      return description;
    }

    public String getLongDescription() {
      return longDescription;
    }

    public String getIcon() {
      return icon;
    }

    public String getRegion() {
      return region;
    }

    public String getLanguage() {
      return language;
    }

    public String getRelation() {
      return relation;
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

    public String getRecordable() {
      return recordable;
    }
  }

  /**
   * Class for video programs, a.k.a. TV shows.
   */
  public static class VideoProgram extends EpgItem {

    public static final String CLASS = "object.item.epgItem.videoProgram";

    public VideoProgram(Cursor cursor) {
      super(cursor);
    }
  }

  /**
   * Base class for all video items.
   */
  public static class VideoItem extends Item {

    @ColumnName("upnp:genre")
    private String genre;
    @ColumnName("upnp:genre@id")
    private String genreId;
    @ColumnName("upnp:genre@type")
    private String genreType;
    @ColumnName("upnp:longDescription")
    private String longDescription;
    @ColumnName("upnp:producer")
    private String producer;
    @ColumnName("upnp:rating")
    private String rating;
    @ColumnName("upnp:actor")
    private String actor;
    @ColumnName("upnp:director")
    private String director;
    @ColumnName("dc:description")
    private String description;
    @ColumnName("dc:publisher")
    private String publisher;
    @ColumnName("dc:language")
    private String language;
    @ColumnName("dc:relation")
    private String relation;
    @ColumnName("upnp:playbackCount")
    private String playbackCount;
    @ColumnName("upnp:lastPlaybackTime")
    private String lastPlaybackTime;
    @ColumnName("upnp:lastPlaybackPosition")
    private String lastPlaybackPosition;
    @ColumnName("upnp:recordedDayOfWeek")
    private String recordedDayOfWeek;
    @ColumnName("upnp:srsRecordScheduleID")
    private String srsRecordScheduleId;

    public VideoItem(Cursor cursor) {
      super(cursor);
    }

    public String getGenre() {
      return genre;
    }

    public String getGenreId() {
      return genreId;
    }

    public String getGenreType() {
      return genreType;
    }

    public String getLongDescription() {
      return longDescription;
    }

    public String getProducer() {
      return producer;
    }

    public String getRating() {
      return rating;
    }

    public String getActor() {
      return actor;
    }

    public String getDirector() {
      return director;
    }

    public String getDescription() {
      return description;
    }

    public String getPublisher() {
      return publisher;
    }

    public String getLanguage() {
      return language;
    }

    public String getRelation() {
      return relation;
    }

    public String getPlaybackCount() {
      return playbackCount;
    }

    public String getLastPlaybackTime() {
      return lastPlaybackTime;
    }

    public String getLastPlaybackPosition() {
      return lastPlaybackPosition;
    }

    public String getRecordedDayOfWeek() {
      return recordedDayOfWeek;
    }

    public String getSrsRecordScheduleId() {
      return srsRecordScheduleId;
    }
  }

  /**
   * Class for video broadcasts, a.k.a. TV channels.
   */
  public static class VideoBroadcast extends VideoItem {

    public static final String CLASS = "object.item.videoItem.videoBroadcast";

    @ColumnName("upnp:icon")
    private String icon;
    @ColumnName("upnp:region")
    private String region;
    @ColumnName("upnp:channelNr")
    private String channelNr;
    @ColumnName("upnp:signalStrength")
    private String signalStrength;
    @ColumnName("upnp:signalLocked")
    private String signalLocked;
    @ColumnName("upnp:tuned")
    private String tuned;
    @ColumnName("upnp:recordable")
    private String recordable;
    @ColumnName("upnp:callSign")
    private String callSign;
    @ColumnName("upnp:price")
    private String price;
    @ColumnName("upnp:payPerView")
    private String payPerView;

    public VideoBroadcast(Cursor cursor) {
      super(cursor);
    }

    public static String getCLASS() {
      return CLASS;
    }

    public String getIcon() {
      return icon;
    }

    public String getRegion() {
      return region;
    }

    public String getChannelNr() {
      return channelNr;
    }

    public String getSignalStrength() {
      return signalStrength;
    }

    public String getSignalLocked() {
      return signalLocked;
    }

    public String getTuned() {
      return tuned;
    }

    public String getRecordable() {
      return recordable;
    }

    public String getCallSign() {
      return callSign;
    }

    public String getPrice() {
      return price;
    }

    public String getPayPerView() {
      return payPerView;
    }

    public String getChannelId() {
      if (getId() != null) {
        return getId().split("/")[2];
      }
      return null;
    }
  }

  /**
   * Base class for all DLNA containers that have child objects.
   */
  public static class Container extends DlnaObject {

    public static final String CLASS = "object.container";

    @ColumnName("@childCount")
    private String childCount;
    @ColumnName("upnp:createClass")
    private String createClass;
    @ColumnName("upnp:searchClass")
    private String searchClass;
    @ColumnName("@searchable")
    private String searchable;
    @ColumnName("@neverPlayable")
    private String neverPlayable;

    public Container(Cursor cursor) {
      super(cursor);
    }
  }

  /**
   * Class for EPG container items, a.k.a EPG channel data containers.
   */
  public static class EpgContainer extends Container {

    public static final String CLASS = "object.container.epgContainer";

    @ColumnName("upnp:channelGroupName")
    private String channelGroupName;
    @ColumnName("upnp:channelGroupName@id")
    private String channelGroupNameId;
    @ColumnName("upnp:serviceProvider")
    private String seviceProvider;
    @ColumnName("upnp:channelName")
    private String channelName;
    @ColumnName("upnp:channelNr")
    private String channelNr;
    @ColumnName("upnp:channelID")
    private String channelId;
    @ColumnName("upnp:channelID@type")
    private String channelIdType;
    @ColumnName("upnp:radioCallSign")
    private String radioCallSign;
    @ColumnName("upnp:radioStationID")
    private String radioStationId;
    @ColumnName("upnp:radioBand")
    private String radioBand;
    @ColumnName("upnp:callSign")
    private String callSign;
    @ColumnName("upnp:networkAffiliation")
    private String networkAffiliation;
    @ColumnName("upnp:serviceProvider")
    private String serviceProvider;
    @ColumnName("upnp:price")
    private String price;
    @ColumnName("upnp:price@currency")
    private String priceCurrency;
    @ColumnName("upnp:payPerView")
    private String payPerView;
    @ColumnName("upnp:epgProviderName")
    private String epgProviderName;
    @ColumnName("upnp:icon")
    private String icon;
    @ColumnName("upnp:region")
    private String region;
    @ColumnName("dc:language")
    private String language;
    @ColumnName("dc:relation")
    private String relation;
    @ColumnName("upnp:dateTimeRange")
    private String dateTimeRange;

    public EpgContainer(Cursor cursor) {
      super(cursor);
    }

    public String getChannelGroupName() {
      return channelGroupName;
    }

    public String getChannelGroupNameId() {
      return channelGroupNameId;
    }

    public String getSeviceProvider() {
      return seviceProvider;
    }

    public String getChannelName() {
      return channelName;
    }

    public String getChannelNr() {
      return channelNr;
    }

    public String getChannelId() {
      return channelId;
    }

    public String getChannelIdType() {
      return channelIdType;
    }

    public String getRadioCallSign() {
      return radioCallSign;
    }

    public String getRadioStationId() {
      return radioStationId;
    }

    public String getRadioBand() {
      return radioBand;
    }

    public String getCallSign() {
      return callSign;
    }

    public String getNetworkAffiliation() {
      return networkAffiliation;
    }

    public String getServiceProvider() {
      return serviceProvider;
    }

    public String getPrice() {
      return price;
    }

    public String getPriceCurrency() {
      return priceCurrency;
    }

    public String getPayPerView() {
      return payPerView;
    }

    public String getEpgProviderName() {
      return epgProviderName;
    }

    public String getIcon() {
      return icon;
    }

    public String getRegion() {
      return region;
    }

    public String getLanguage() {
      return language;
    }

    public String getRelation() {
      return relation;
    }

    public String getDateTimeRange() {
      return dateTimeRange;
    }

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

