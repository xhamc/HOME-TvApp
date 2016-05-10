package com.sony.sel.tvapp.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.Bindable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * View for showing a media progress bar
 */
public class MediaProgressView extends FrameLayout implements Bindable<MediaProgressView.ProgressInfo> {

  /**
   * Class for sending media progress.
   */
  public static class ProgressInfo {
    private Date startTime;
    private Date progress;
    private Date endTime;
    private Double playSpeed;

    public ProgressInfo(Date startTime, Date progress, Date endTime, Double playSpeed) {
      this.startTime = startTime;
      this.progress = progress;
      this.endTime = endTime;
      this.playSpeed = playSpeed;
    }

    public Date getStartTime() {
      return startTime;
    }

    public void setStartTime(Date startTime) {
      this.startTime = startTime;
    }

    public Date getProgress() {
      return progress;
    }

    public void setProgress(Date progress) {
      this.progress = progress;
    }

    public void setEndTime(Date endTime) {
      this.endTime = endTime;
    }

    public Date getEndTime() {
      return endTime;
    }

    public Double getPlaySpeed() {
      return playSpeed;
    }

    public void setPlaySpeed(Double playSpeed) {
      this.playSpeed = playSpeed;
    }
  }

  @Bind(R.id.startTime)
  TextView startTime;
  @Bind(R.id.progress)
  ProgressBar progressBar;
  @Bind(R.id.endTime)
  TextView endTime;

  private ProgressInfo data;

  public MediaProgressView(Context context) {
    super(context);
  }

  public MediaProgressView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public MediaProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public MediaProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  @Override
  public void bind(ProgressInfo data) {
    this.data = data;

    DateFormat format = new SimpleDateFormat("H:mm:ss");
    if (data.getStartTime().getTime() == 0) {
      // adjust for UTC if time is absolute
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    String startText = format.format(data.getProgress());
    if (data.getPlaySpeed() != 1) {
      startText += " ("+data.getPlaySpeed()+"x)";
    }
    startTime.setText(startText);
    endTime.setText(format.format(new Date(data.getEndTime().getTime())));

    int duration = (int) (data.getEndTime().getTime());
    int progress = (int) (data.getProgress().getTime() - data.getStartTime().getTime());

    progressBar.setMax(duration);
    progressBar.setProgress(progress);
  }

  public void setProgress(Date progress) {
    // keep progress value within limits
    progress = new Date(Math.max(data.getStartTime().getTime(),Math.min(data.getEndTime().getTime(), progress.getTime())));
    data.setProgress(progress);
    // rebind to update
    bind(data);
  }

  public void setPlaySpeed(Double speed) {
    data.setPlaySpeed(speed);
    // rebind to update
    bind(data);
  }

  @Override
  public ProgressInfo getData() {
    return data;
  }

}
