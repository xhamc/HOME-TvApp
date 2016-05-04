package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * View
 */
public class SearchResultCell extends BaseListCell<VideoProgram> {

  private VideoProgram program;

  @Bind(android.R.id.text1)
  TextView title;

  @Bind(android.R.id.text2)
  TextView details;

  @Bind(R.id.icon)
  ImageView icon;

  public SearchResultCell(Context context) {
    super(context);
  }

  public SearchResultCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SearchResultCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SearchResultCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  @Override
  public void bind(VideoProgram data) {
    program = data;
    title.setText(data.getTitle());
    String time = new SimpleDateFormat("M/d/yy h:mm a").format(data.getScheduledStartTime());
    details.setText(time);
    if (program.getIcon() != null && program.getIcon().length() > 0 ) {
      Picasso.with(getContext()).load(Uri.parse(program.getIcon())).into(icon);
    } else {
      icon.setImageDrawable(null);
    }
  }

  @Override
  public VideoProgram getData() {
    return program;
  }
}
