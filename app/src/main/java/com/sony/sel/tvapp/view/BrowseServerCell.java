package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.huey.dlna.IconList;
import com.sony.sel.tvapp.R;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

public class BrowseServerCell extends BaseListCell<UpnpDevice> {

  private UpnpDevice data;

  @Bind(R.id.icon)
  ImageView icon;
  @Bind(R.id.title)
  TextView friendlyName;
  @Bind(R.id.deviceInfo)
  TextView deviceInfo;

  public BrowseServerCell(Context context) {
    super(context);
  }

  public BrowseServerCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BrowseServerCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public BrowseServerCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  @Override
  public void bind(final UpnpDevice data) {

    this.data = data;
    friendlyName.setText(data.getFriendlyName());

    StringBuilder info = new StringBuilder();
    if (data.getManufacturer() != null) {
      info.append(data.getManufacturer());
    }
    if (data.getModelName() != null) {
      if (info.length() > 0) {
        info.append(" ");
      }
      info.append(data.getModelName());
    }
    if (info.length() > 0) {
      deviceInfo.setVisibility(View.VISIBLE);
      deviceInfo.setText(info.toString());
    } else {
      deviceInfo.setVisibility(View.GONE);
    }

    IconList iconList = data.getIconList();

    if (iconList != null && iconList.getCount() > 0) {
      drawIcon(iconList);
    } else {
      icon.setImageDrawable(null);
    }

    setupFocus(null, 1.02f);
  }

  @Override
  public UpnpDevice getData() {
    return data;
  }

  void drawIcon(IconList iconList) {
    List<String> formats = Arrays.asList(
        "image/bmp",
        "image/gif",
        "image/jpeg",
        "image/png"
    );
    int bestIcon = 0;
    for (int i = 1; i < iconList.getCount(); i++) {
      if (formats.indexOf(iconList.getMimetype(i)) > formats.indexOf(iconList.getMimetype(bestIcon))) {
        bestIcon = i;
      } else if (iconList.getWidth(i) > iconList.getWidth(bestIcon)) {
        bestIcon = i;
      }
    }
    Uri uri = Uri.parse(iconList.getUrl(bestIcon));
    Picasso.with(getContext()).load(uri).into(icon);
  }


}
