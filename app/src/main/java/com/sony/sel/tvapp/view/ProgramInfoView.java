package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.EventBus.FavoriteChannelsChangedEvent;
import com.sony.sel.tvapp.util.EventBus.FavoriteProgramsChangedEvent;
import com.sony.sel.tvapp.util.EventBus.RecordingsChangedEvent;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * View for displaying the channel data overlay.
 */
public class ProgramInfoView extends FrameLayout {

  public static final String TAG = ProgramInfoView.class.getSimpleName();

  public static final long HIDE_TIMEOUT = 5000;

  @Bind(R.id.programIcon)
  ImageView icon;
  @Bind(R.id.title)
  TextView title;
  @Nullable
  @Bind(R.id.programTitle)
  TextView programTitle;
  @Nullable
  @Bind(R.id.programChannel)
  TextView channelNumber;
  @Bind(R.id.programTime)
  TextView time;
  @Nullable
  @Bind(R.id.programDescription)
  TextView description;
  @Nullable
  @Bind(R.id.favoriteChannel)
  ImageView favoriteChannel;
  @Nullable
  @Bind(R.id.recordProgram)
  ImageView recordProgram;
  @Nullable
  @Bind(R.id.recordSeries)
  ImageView recordSeries;
  @Nullable
  @Bind(R.id.popupAlignView)
  View popupAlignView;
  @Nullable
  @Bind(R.id.favoriteProgram)
  View favoriteProgram;

  private VideoProgram program;
  private VideoBroadcast channel;
  private SettingsHelper settingsHelper;

  public ProgramInfoView(Context context) {
    super(context);
  }

  public ProgramInfoView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ProgramInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ProgramInfoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    if (isInEditMode()) {
      return;
    }
    settingsHelper = SettingsHelper.getHelper(getContext());
    ButterKnife.bind(this);
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

  @Subscribe
  public void onRecordingsChanged(RecordingsChangedEvent event) {
    // rebind to refresh display
    bind(program, channel);
  }

  @Subscribe
  public void onFavoriteChannelsChanged(FavoriteChannelsChangedEvent event) {
    // rebind to refresh display
    bind(program, channel);
  }

  @Subscribe
  public void onFavoriteProgramsChanged(FavoriteProgramsChangedEvent event) {
    // rebind to refresh display
    bind(program, channel);
  }

  public VideoProgram getProgram() {
    return program;
  }

  public VideoBroadcast getChannel() {
    return channel;
  }

  public View getPopupAlignView() {
    return popupAlignView != null ? popupAlignView : this;
  }

  public void bind(VideoProgram program, DlnaObjects.VideoBroadcast channel) {
    this.program = program;
    this.channel = channel;

    if (program != null) {

      // icon
      if (program.getIcon() != null) {
        // we have a program icon, use it
        setProgramIcon(program.getIcon());
      } else if (channel.getIcon() != null) {
        // fall back to channel icon
        setChannelIcon(channel.getIcon());
      } else {
        // no icon available
        icon.setVisibility(View.GONE);
      }

      // title
      title.setText(program.getTitle());

      // hide program title
      if (programTitle != null) {
        if (program.getProgramTitle() != null && program.getProgramTitle().length() > 0) {
          programTitle.setText(program.getProgramTitle());
          programTitle.setVisibility(View.VISIBLE);
        } else {
          programTitle.setVisibility(View.GONE);
        }
      }

      if (channelNumber != null) {
        // number
        channelNumber.setText(channel.getChannelNumber() + " " + channel.getCallSign());
      }

      // start/end time
      DateFormat format = new SimpleDateFormat("h:mm");
      String programTime = format.format(program.getScheduledStartTime()) + "-" + format.format(program.getScheduledEndTime());
      time.setText(programTime);
      time.setVisibility(View.VISIBLE);

      if (description != null) {
        // long description
        description.setText(program.getLongDescription());
      }

      if (favoriteProgram != null) {
        favoriteProgram.setVisibility(settingsHelper.isFavoriteProgram(program) ? VISIBLE : GONE);
      }

    } else if (channel != null) {

      // icon
      if (channel.getIcon() != null) {
        // use channel icon
        setChannelIcon(channel.getIcon());
      } else {
        // no icon available
        icon.setVisibility(View.GONE);
      }

      // channel name
      title.setText(channel.getCallSign());

      // hide program title
      if (programTitle != null) {
        programTitle.setVisibility(View.GONE);
      }

      if (channelNumber != null) {
        // call sign
        channelNumber.setText(channel.getChannelNumber());
      }

      // hide time
      time.setVisibility(View.GONE);
      if (description != null) {
        // use description if available
        description.setText(channel.getDescription());
      }

      if (favoriteProgram != null) {
        favoriteProgram.setVisibility(GONE);
      }

    }

    if (channel != null && favoriteChannel != null) {
      if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
        favoriteChannel.setVisibility(View.VISIBLE);
      } else {
        favoriteChannel.setVisibility(View.GONE);
      }
    }

    if (program != null && recordProgram != null && recordSeries != null) {
      if (settingsHelper.isSeriesRecorded(program)) {
        recordSeries.setVisibility(View.VISIBLE);
        recordProgram.setVisibility(View.GONE);
      } else if (settingsHelper.isProgramRecorded(program)) {
        recordSeries.setVisibility(View.GONE);
        recordProgram.setVisibility(View.VISIBLE);
      } else {
        recordSeries.setVisibility(View.GONE);
        recordProgram.setVisibility(View.GONE);
      }
    } else {
      recordSeries.setVisibility(View.GONE);
      recordProgram.setVisibility(View.GONE);
    }
  }

  /**
   * Set up the icon as a program/show thumbnail.
   *
   * @param uri Icon uri.
   */
  private void setProgramIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    icon.setPadding(0, 0, 0, 0);
    Picasso.with(getContext()).load(Uri.parse(uri)).into(icon);
  }

  /**
   * Set up the icon as a channel ID thumbnail.
   *
   * @param uri Icon uri.
   */
  private void setChannelIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
    icon.setPadding(padding, padding, padding, padding);
    Picasso.with(getContext()).load(Uri.parse(uri)).into(icon);
  }

}
