package com.sony.sel.tvapp.view;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
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
  @Bind(R.id.favorite)
  ImageView favorite;


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
  public void bind(final VideoBroadcast channel) {

    ButterKnife.bind(this);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();

    List<String> channels = new ArrayList<>();
    channels.add(channel.getChannelId());

    this.channel = channel;
    List<VideoProgram> epgList = DlnaHelper.getCache(getContext()).searchEpg(
        SettingsHelper.getHelper(getContext()).getEpgServer(),
        channels,
        now,
        now
    );
    if (epgList.size() > 0) {
      epg = epgList.get(0);
    } else {
      epg = null;
    }

    // icon
    if (channel.getIcon() != null) {
      // use channel icon
      icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
      int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
      icon.setPadding(padding, padding, padding, padding);
      Picasso.with(getContext()).load(Uri.parse(channel.getIcon())).into(icon);
    } else {
      // no icon available
      icon.setImageDrawable(null);
    }

    // call sign
    title.setText(channel.getCallSign());

    // hide epg fields until set
    seriesTitle.setVisibility(View.GONE);
    programTitle.setVisibility(View.GONE);
    programTime.setVisibility(View.GONE);

    if (SettingsHelper.getHelper(getContext()).getFavoriteChannels().contains(channel.getChannelId())) {
      favorite.setVisibility(View.VISIBLE);
    } else {
      favorite.setVisibility(View.GONE);
    }

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
        showChannelPopup(v, channel);
        return true;
      }
    });
  }

  public void setEpg(VideoProgram epg) {
    this.epg = epg;
    if (epg != null) {

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
        Picasso.with(getContext()).load(Uri.parse(epg.getIcon())).into(icon);
      }
    }
  }

  private void showChannelPopup(View v, final VideoBroadcast channel) {
    final SettingsHelper settingsHelper = SettingsHelper.getHelper(getContext());
    PopupMenu menu = new PopupMenu(getContext(), v);
    menu.inflate(R.menu.channel_popup_menu);
    if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
      menu.getMenu().removeItem(R.id.addToFavoriteChannels);
    } else {
      menu.getMenu().removeItem(R.id.removeFromFavoriteChannels);
    }
    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.addToFavoriteChannels:
            settingsHelper.addFavoriteChannel(channel.getChannelId());
            // re-bind to update display
            bind(channel);
            setEpg(epg);
            return true;
          case R.id.removeFromFavoriteChannels:
            settingsHelper.removeFavoriteChannel(channel.getChannelId());
            // re-bind to update display
            bind(channel);
            setEpg(epg);
            return true;
        }
        return false;
      }
    });
    menu.show();
    // keep ui alive longer if popup is selected
    EventBus.getInstance().post(new EventBus.ResetUiTimerLongEvent());
  }

  @Override
  public VideoBroadcast getData() {
    return channel;
  }

  @Override
  protected void onFocusChanged(boolean hasFocus, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(hasFocus, direction, previouslyFocusedRect);
    // keep the UI alive
    EventBus.getInstance().post(new EventBus.ResetUiTimerLongEvent());
  }
}
