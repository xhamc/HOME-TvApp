package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.Bindable;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * View for displaying information about a VideoItem (i.e. a VOD video).
 */
public class VideoItemInfoView extends FrameLayout implements Bindable<VideoItem> {

  @Bind(R.id.icon)
  ImageView icon;
  @Bind(R.id.title)
  TextView title;
  @Bind(R.id.line1)
  TextView line1;
  @Bind(R.id.line2)
  TextView line2;
  @Bind(R.id.line3)
  TextView line3;
  @Bind(R.id.description)
  TextView description;

  private VideoItem data;

  public VideoItemInfoView(Context context) {
    super(context);
  }

  public VideoItemInfoView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public VideoItemInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public VideoItemInfoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

  @Override
  public void bind(VideoItem data) {
    this.data = data;

    if (data != null) {

      // icon
      if (data.getIcon() != null) {
        // we have a video icon, use it
        setIcon(data.getIcon());
      } else {
        // no icon available
        icon.setVisibility(View.GONE);
      }

      // title
      title.setText(data.getTitle());

      if (data.getProgramTitle() != null && data.getProgramTitle().length() > 0) {
        line1.setVisibility(View.VISIBLE);
        line1.setText(data.getProgramTitle());
      } else {
        line1.setVisibility(View.GONE);
      }

      StringBuilder line2text = new StringBuilder();
      if (data.getGenre() != null) {
        line2text.append(data.getGenre());
      }
      if (data.getLanguage() != null) {
        if (line2text.length() > 0) {
          line2text.append(", ");
        }
        line2text.append(data.getLanguage());
      }
      if (line2text.length() > 0) {
        line2.setVisibility(View.VISIBLE);
        line2.setText(line2text.toString());
      } else {
        line2.setVisibility(View.GONE);
      }

      if (data.getRating() != null) {
        line3.setVisibility(View.VISIBLE);
        line3.setText(data.getRating());
      } else {
        line3.setVisibility(View.GONE);
      }

      // long description
      description.setText(data.getLongDescription());
    }
  }

  /**
   * Set up the icon as a program/show thumbnail.
   *
   * @param uri Icon uri.
   */
  private void setIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    icon.setPadding(0, 0, 0, 0);
    Picasso.with(getContext()).load(Uri.parse(uri)).into(icon);
  }

  @Override
  public VideoItem getData() {
    return data;
  }
}
