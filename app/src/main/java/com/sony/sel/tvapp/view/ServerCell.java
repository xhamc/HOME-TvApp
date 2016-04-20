package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.huey.dlna.IconList;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

public class ServerCell extends BaseListCell<UpnpDevice> {

  private UpnpDevice data;

  @Bind(R.id.icon)
  ImageView icon;
  @Bind(R.id.title)
  TextView friendlyName;
  @Bind(R.id.deviceInfo)
  TextView deviceInfo;
  @Bind(R.id.udn)
  TextView udn;
  @Bind(R.id.deviceType)
  TextView deviceType;
  @Bind(R.id.serviceTypes)
  TextView serviceTypes;
  @Bind(R.id.check)
  CheckBox check;

  public ServerCell(Context context) {
    super(context);
  }

  public ServerCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ServerCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ServerCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this);

    EventBus.getInstance().register(this);
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    check.setChecked(event.getServerUdn().equals(data.getUdn()));
  }

  @Override
  public void bind(final UpnpDevice data) {

    String serverUdn = SettingsHelper.getHelper(getContext()).getEpgServer();

    this.data = data;
    friendlyName.setText(data.getFriendlyName());
    udn.setText(data.getUdn());
    deviceType.setText(data.getDeviceType());

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

    String iconUri = data.getIcon();
    if (iconUri != null) {
      // use explicitly set icon
      icon.setImageDrawable(null);
      Picasso.with(getContext()).load(iconUri).into(icon);
    } else {
      // else parse icon list
      IconList iconList = data.getIconList();
      if (iconList != null && iconList.getCount() > 0) {
        drawIcon(iconList);
      } else {
        icon.setImageDrawable(null);
      }
    }

    check.setChecked(serverUdn != null ? serverUdn.equals(data.getUdn()) : false);

    setupFocus(null, 1.02f);
  }

  @Override
  public UpnpDevice getData() {
    return data;
  }

  public static final String LOG_TAG = "DlnaTest";

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
