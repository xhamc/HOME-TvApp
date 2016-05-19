package com.sony.sel.tvapp.view;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.menu.PopupHelper;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.EventBus.FavoriteProgramsChangedEvent;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.EventBus.FavoriteChannelsChangedEvent;
import static com.sony.sel.tvapp.util.EventBus.RecordingsChangedEvent;
import static com.sony.sel.tvapp.util.EventBus.ResetUiTimerLongEvent;
import static com.sony.sel.tvapp.util.EventBus.getInstance;

/**
 * Cell for displaying channel info.
 */
public class ChannelCell extends BaseListCell<VideoBroadcast> {

  private VideoBroadcast channel;
  private VideoProgram epg;

  @Bind(R.id.channelIcon)
  ImageView icon;
  @Bind(R.id.channelName)
  TextView title;
  @Bind(R.id.seriesTitle)
  TextView seriesTitle;
  @Bind(R.id.programTitle)
  TextView programTitle;
  @Bind(R.id.programTime)
  TextView programTime;
  @Bind(R.id.favoriteChannel)
  ImageView favoriteChannel;
  @Bind(R.id.favoriteProgram)
  View favoriteProgram;
  @Bind(R.id.recordProgram)
  ImageView recordProgram;
  @Bind(R.id.recordSeries)
  ImageView recordSeries;
  @Bind(R.id.smallChannelIcon)
  ImageView smallChannelIcon;

  public ChannelCell(Context context) {
    super(context);
  }

  public ChannelCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChannelCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ChannelCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    EventBus.getInstance().register(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    EventBus.getInstance().unregister(this);
  }

  @Override
  public void bind(final VideoBroadcast channel) {

    SettingsHelper settingsHelper = SettingsHelper.getHelper(getContext());

    ButterKnife.bind(this);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();

    List<String> channels = new ArrayList<>();
    channels.add(channel.getChannelId());

    this.channel = channel;

    // icon
    icon.setImageDrawable(null);
    if (channel.getIcon() != null) {
      // use channel icon
      icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
      int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
      icon.setPadding(padding, padding, padding, padding);
      icon.setBackgroundColor(getResources().getColor(android.R.color.white));
      Picasso.with(getContext()).load(Uri.parse(channel.getIcon())).into(icon);
    } else {
      // no icon available
      icon.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    // call sign
    title.setText(channel.getCallSign());

    // hide epg fields until set
    seriesTitle.setVisibility(View.GONE);
    programTitle.setVisibility(View.GONE);
    programTime.setVisibility(View.GONE);

    if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
      favoriteChannel.setVisibility(View.VISIBLE);
    } else {
      favoriteChannel.setVisibility(View.GONE);
    }

    favoriteProgram.setVisibility(View.GONE);
    recordProgram.setVisibility(View.GONE);
    recordSeries.setVisibility(View.GONE);
    smallChannelIcon.setVisibility(View.GONE);

    setupFocus(null, 1.1f);

    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        SettingsHelper.getHelper(getContext()).setCurrentChannel(channel);
      }
    });
    setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        showChannelPopup(v);
        return true;
      }
    });
  }

  public void setEpg(VideoProgram epg) {
    this.epg = epg;
    if (epg != null) {

      SettingsHelper settingsHelper = SettingsHelper.getHelper(getContext());

      // title
      if (epg.getTitle().length() > 0) {
        seriesTitle.setVisibility(View.VISIBLE);
        seriesTitle.setText(epg.getTitle());
      }

      // program title
      if (epg.getProgramTitle().length() > 0) {
        programTitle.setVisibility(View.VISIBLE);
        programTitle.setText(epg.getProgramTitle());
      }

      // start/end time
      DateFormat format = new SimpleDateFormat("h:mm");
      String time = format.format(epg.getScheduledStartTime()) + "-" + format.format(epg.getScheduledEndTime());
      programTime.setText(time);
      programTime.setVisibility(View.VISIBLE);

      // icon
      if (epg.getIcon() != null) {
        // use epg icon
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setPadding(0, 0, 0, 0);
        icon.setBackgroundColor(getResources().getColor(android.R.color.black));
        Picasso.with(getContext()).load(Uri.parse(epg.getIcon())).into(icon);

        // move channel icon to small display in corner
        if (channel.getIcon() != null) {
          smallChannelIcon.setVisibility(View.VISIBLE);
          Picasso.with(getContext()).load(Uri.parse(channel.getIcon())).into(smallChannelIcon);
        }
      }

      favoriteProgram.setVisibility(settingsHelper.isFavoriteProgram(epg) ? VISIBLE : GONE);
      recordProgram.setVisibility(settingsHelper.isProgramRecorded(epg) ? VISIBLE : GONE);
      recordSeries.setVisibility(settingsHelper.isSeriesRecorded(epg) ? VISIBLE : GONE);
    }
  }

  private void showChannelPopup(View v) {
    if (epg != null) {
      PopupHelper.getHelper(getContext()).showPopup(epg, v);
    } else {
      PopupHelper.getHelper(getContext()).showPopup(channel, v);
    }
    // keep ui alive longer if popup is selected
    getInstance().post(new ResetUiTimerLongEvent());
  }

  @Override
  public VideoBroadcast getData() {
    return channel;
  }

  @Override
  protected void onFocusChanged(boolean hasFocus, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(hasFocus, direction, previouslyFocusedRect);
    // keep the UI alive
    getInstance().post(new ResetUiTimerLongEvent());
  }

  @Subscribe
  public void onFavoriteProgramsChanged(FavoriteProgramsChangedEvent event) {
    // rebind to refresh display
    bind(channel);
    setEpg(epg);
  }

  @Subscribe
  public void onFavoriteChannelsChanged(FavoriteChannelsChangedEvent event) {
    // rebind to refresh display
    bind(channel);
    setEpg(epg);
  }

  @Subscribe
  public void onRecordingsChangedEvent(RecordingsChangedEvent event) {
    // rebind to refresh display
    bind(channel);
    setEpg(epg);
  }

}
