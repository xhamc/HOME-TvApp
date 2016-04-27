package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.SettingsHelper;
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
  @Bind(R.id.programTitle)
  TextView title;
  @Nullable
  @Bind(R.id.programChannel)
  TextView channelNumber;
  @Bind(R.id.programTime)
  TextView time;
  @Nullable
  @Bind(R.id.programDescription)
  TextView description;
  @Nullable
  @Bind(R.id.favorite)
  ImageView favorite;
  @Nullable
  @Bind(R.id.recordProgram)
  ImageView recordProgram;
  @Nullable
  @Bind(R.id.recordSeries)
  ImageView recordSeries;
  @Nullable
  @Bind(R.id.popupAlignView)
  View popupAlignView;

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

    }

    if (channel != null && favorite != null) {
      if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
        favorite.setVisibility(View.VISIBLE);
      } else {
        favorite.setVisibility(View.GONE);
      }
    }

    if (program != null && recordProgram != null && recordSeries != null) {
      if (settingsHelper.getSeriesToRecord().contains(program.getTitle())) {
        recordSeries.setVisibility(View.VISIBLE);
        recordProgram.setVisibility(View.GONE);
      } else if (settingsHelper.getProgramsToRecord().contains(program.getId())) {
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
