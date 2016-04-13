package com.sony.sel.tvapp.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.view.ServerCell;
import com.sony.sel.util.NetworkHelper;
import com.sony.sel.util.SsdpServiceHelper;
import com.sony.sel.util.ViewUtils;

/**
 * Fragment for choosing the EPG server
 */
public class SelectServerFragment extends BaseFragment {

  public static final String TAG = SelectServerFragment.class.getSimpleName();

  private SsdpServiceHelper ssdpServiceHelper;
  private NetworkHelper networkHelper;

  private RecyclerView list;
  private DeviceAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.select_server_fragment, null);

    list = ViewUtils.findViewById(contentView, android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);
    adapter.setLoading();

    networkHelper = NetworkHelper.getHelper(getActivity());
    ssdpServiceHelper = new SsdpServiceHelper(networkHelper);
    startDiscovery();

    return contentView;
  }


  @Override
  public void onDestroy() {
    super.onDestroy();
    cancelDiscovery();
  }

  private void startDiscovery() {
    adapter.setLoading();

    // search for devices
    ssdpServiceHelper.findSsdpDevice(SsdpServiceHelper.SsdpServiceType.ANY, new SsdpServiceHelper.SsdpDeviceObserver() {
      @Override
      public void onDeviceFound(@NonNull SsdpServiceHelper.SsdpDeviceInfo deviceInfo) {
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

  private class DeviceAdapter extends TvAppAdapter<SsdpServiceHelper.SsdpDeviceInfo, ServerCell> {

    public DeviceAdapter() {
      super(
          getActivity(),
          R.id.server_cell,
          R.layout.server_cell,
          getString(R.string.searchingForServers),
          getString(R.string.noServersFound)
      );
    }
  }
}
