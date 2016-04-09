package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.util.SsdpServiceHelper;
import com.sony.sel.util.ViewUtils;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.sony.sel.util.SsdpServiceHelper.SsdpDeviceInfo;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;

public class ServerCell extends BaseListCell<SsdpDeviceInfo> {

  private SsdpDeviceInfo data;

  private ImageView icon;
  private TextView friendlyName;
  private TextView deviceInfo;
  private TextView udn;
  private TextView deviceType;
  private TextView serviceTypes;
  private CheckBox check;

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

    icon = ViewUtils.findViewById(this, R.id.icon);
    friendlyName = ViewUtils.findViewById(this, R.id.friendlyName);
    deviceInfo = ViewUtils.findViewById(this, R.id.deviceInfo);
    udn = ViewUtils.findViewById(this, R.id.udn);
    deviceType = ViewUtils.findViewById(this, R.id.deviceType);
    serviceTypes = ViewUtils.findViewById(this, R.id.serviceTypes);
    check = ViewUtils.findViewById(this, R.id.check);

    EventBus.getInstance().register(this);
  }

  @Subscribe
  public void onServerChanged(EventBus.EpgServerChangedEvent event) {
    check.setChecked(event.getEpgServer().equals(data.getUdn()));
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
        DlnaHelper dlnaHelper = DlnaHelper.getHelper(getContext());
        List<VideoBroadcast> channels = dlnaHelper.getChannels(data.getUdn());
        if (channels.size() == 0) {
          Log.e(LOG_TAG, "No channels found.");
          return;
        }
        // pick some random channels
        Set<String> channelIds = new HashSet<>();
        for (int i = 0; i < 5; i++) {
          channelIds.add(channels.get(Math.abs(new Random().nextInt()) % channels.size()).getChannelId());
        }
        // set time interval as next 5 hours
        Date start = new Date();
        Calendar end = Calendar.getInstance();
        end.setTime(start);
        end.add(Calendar.HOUR, 3);
        long time = System.currentTimeMillis();
        Log.d(LOG_TAG, "Finding shows on " + channelIds.size() + " channels from " + start + " to " + end.getTime() + ":");
        List<VideoProgram> shows = dlnaHelper.getEpgPrograms(data.getUdn(), channelIds, start, end.getTime());
        Log.d(LOG_TAG, "Query finished in " + (System.currentTimeMillis() - time) + "msec.");
        for (VideoProgram show : shows) {
          Log.d(LOG_TAG, show.toString());
        }
      }
    });
  }

  public static final String LOG_TAG = "DlnaTest";

  void drawIcon(SsdpServiceHelper.IconInfo iconInfo) {
    Uri uri = Uri.parse(data.getDeviceDescriptorLocation()).buildUpon().path(iconInfo.getUrl()).build();
    Picasso.with(getContext()).load(uri).into(icon);
  }

  @Override
  public SsdpDeviceInfo getData() {
    return data;
  }

}
