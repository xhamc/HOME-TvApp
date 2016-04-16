package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects;
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
public class ProgramInfoView extends FrameLayout  {

  public static final String TAG = ProgramInfoView.class.getSimpleName();

  public static final long HIDE_TIMEOUT = 5000;

  @Bind(R.id.programIcon)
  ImageView icon;
  @Bind(R.id.programTitle)
  TextView title;
  @Bind(R.id.programChannel)
  TextView channelNumber;
  @Bind(R.id.programTime)
  TextView time;
  @Bind(R.id.programDescription)
  TextView description;

  private VideoProgram program;
  private VideoBroadcast channel;

  private Handler handler = new Handler();
  private Runnable timeoutRunnable = new Runnable() {
    @Override
    public void run() {
      hide();
    }
  };

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
    ButterKnife.bind(this);
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

      // number
      channelNumber.setText(channel.getCallSign());

      // start/end time
      DateFormat format = new SimpleDateFormat("h:mm");
      String programTime = format.format(program.getScheduledStartTime()) + "-" + format.format(program.getScheduledEndTime());
      time.setText(programTime);
      time.setVisibility(View.VISIBLE);

      // long description
      description.setText(program.getLongDescription());

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

      // call sign
      channelNumber.setText(channel.getChannelNumber());

      // hide time
      time.setVisibility(View.GONE);

      // use description if available
      description.setText(channel.getDescription());
    }
    // show or keep showing
    show();
  }

  /**
   * Set up the icon as a program/show thumbnail.
   * @param uri Icon uri.
   */
  private void setProgramIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    icon.setPadding(0,0,0,0);
    Picasso.with(getContext()).load(Uri.parse(uri)).into(icon);
  }

  /**
   * Set up the icon as a channel ID thumbnail.
   * @param uri Icon uri.
   */
  private void setChannelIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
    icon.setPadding(padding,padding,padding,padding);
    Picasso.with(getContext()).load(Uri.parse(uri)).into(icon);
  }

  public void show() {
    this.animate().alpha(1.0f).start();
    handler.removeCallbacks(timeoutRunnable);
    handler.postDelayed(timeoutRunnable, HIDE_TIMEOUT);
  }

  public boolean isVisible() {
    return getAlpha() == 1.0f;
  }

  public void hide() {
    this.animate().alpha(0.0f).start();
  }

}
