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

    public ProgressInfo(Date startTime, Date progress, Date endTime) {
      this.startTime = startTime;
      this.progress = progress;
      this.endTime = endTime;
    }

    public ProgressInfo(long duration, long progress) {
      this.startTime = new Date(0);
      this.progress = new Date(progress);
      this.endTime = new Date(duration);
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

    startTime.setText(format.format(data.getProgress()));
    endTime.setText("-"+format.format(new Date(data.getEndTime().getTime()-data.getProgress().getTime())));

    int duration = (int) (data.getEndTime().getTime() - data.getStartTime().getTime());
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

  @Override
  public ProgressInfo getData() {
    return data;
  }

}
