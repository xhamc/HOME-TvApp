package com.sony.sel.tvapp.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.view.ServerCell;
import com.sony.sel.util.NetworkHelper;
import com.sony.sel.util.SsdpServiceHelper;
import com.sony.sel.util.ViewUtils;
import com.squareup.otto.Subscribe;

import static com.sony.sel.util.SsdpServiceHelper.SsdpDeviceInfo;
import static com.sony.sel.util.SsdpServiceHelper.SsdpDeviceObserver;
import static com.sony.sel.util.SsdpServiceHelper.SsdpServiceType;

/**
 * Activity to select the EPG server.
 */
public class SelectEpgServerActivity extends BaseActivity {

  public static final String TAG = SelectEpgServerActivity.class.getSimpleName();

  private SsdpServiceHelper ssdpServiceHelper;
  private NetworkHelper networkHelper;

  private RecyclerView list;
  private DeviceAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.select_server_activity);
    list = ViewUtils.findViewById(this, android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);
    adapter.setLoading();

    networkHelper = NetworkHelper.getHelper(this);
    ssdpServiceHelper = new SsdpServiceHelper(networkHelper);
    startDiscovery();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    cancelDiscovery();
  }

  private void startDiscovery() {
    adapter.setLoading();
    ssdpServiceHelper.findSsdpDevice(SsdpServiceType.ANY, new SsdpDeviceObserver() {
      @Override
      public void onDeviceFound(@NonNull SsdpDeviceInfo deviceInfo) {
        Log.d(TAG, "Device found: " + deviceInfo.getFriendlyName());
        if (!adapter.contains(deviceInfo)) {
          adapter.add(deviceInfo);
        }
      }

      @Override
      public void onError(@NonNull Exception error) {
        Log.e(TAG, "Error discovering SSDP devices: " + error);
        adapter.onError(error);
      }
    });
  }

  private void cancelDiscovery() {
    ssdpServiceHelper.cancelDiscovery();
  }

  private class DeviceAdapter extends TvAppAdapter<SsdpDeviceInfo, ServerCell> {

    public DeviceAdapter() {
      super(
          SelectEpgServerActivity.this,
          R.id.server_cell,
          R.layout.server_cell,
          getString(R.string.searchingForServers),
          getString(R.string.noServersFound)
      );
    }
  }
}
