package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.util.SsdpServiceHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.util.SsdpServiceHelper.SsdpDeviceInfo;

public class ServerCell extends BaseListCell<SsdpDeviceInfo> {

  private SsdpDeviceInfo data;

  @Bind(R.id.icon)
  ImageView icon;
  @Bind(R.id.friendlyName)
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
  public void bind(final SsdpDeviceInfo data) {

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

    StringBuilder services = new StringBuilder();
    for (String type : data.getServiceTypes()) {
      if (services.length() > 0) {
        services.append("\n");
      }
      services.append(type);
    }
    serviceTypes.setText(services.toString());
    // TODO hide services for now
    serviceTypes.setVisibility(View.GONE);

    if (data.getIcons().size() > 0) {
      // TODO select best icon
      drawIcon(data.getIcons().get(0));
    } else {
      icon.setImageDrawable(null);
    }

    check.setChecked(serverUdn != null ? serverUdn.equals(data.getUdn()) : false);

    setupFocus();

    this.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        SettingsHelper.getHelper(getContext()).setEpgServer(data.getUdn());
        // new FindEpgDataTask(getContext(),data.getUdn()).doInBackground();
      }
    });
  }

  @Override
  public SsdpDeviceInfo getData() {
    return data;
  }

  public static final String LOG_TAG = "DlnaTest";

  void drawIcon(SsdpServiceHelper.IconInfo iconInfo) {
    Uri uri = Uri.parse(data.getDeviceDescriptorLocation()).buildUpon().path(iconInfo.getUrl()).build();
    Picasso.with(getContext()).load(uri).into(icon);
  }


}
