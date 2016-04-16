package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;

/**
 * Cell for displaying channel info.
 */
public class ChannelCell extends BaseListCell<VideoBroadcast> {

  private VideoBroadcast channel;

  @Bind(R.id.channelIcon)
  ImageView icon;
  @Bind(R.id.channelName)
  TextView title;
  @Bind(R.id.channelNetwork)
  TextView details;

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

    this.channel = channel;

    // icon
    if (channel.getIcon() != null) {
      // use channel icon
      icon.setVisibility(View.VISIBLE);
      icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
      int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
      icon.setPadding(padding, padding, padding, padding);
      Picasso.with(getContext()).load(Uri.parse(channel.getIcon())).into(icon);
    } else {
      // no icon available
      icon.setVisibility(View.GONE);
    }

    // call sign
    title.setText(channel.getCallSign());

    // number
    details.setText(channel.getChannelNumber());

    setupFocus(null, 1.1f);

    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        SettingsHelper.getHelper(getContext()).setCurrentChannel(channel);
      }
    });
  }

  @Override
  public VideoBroadcast getData() {
    return channel;
  }
}
