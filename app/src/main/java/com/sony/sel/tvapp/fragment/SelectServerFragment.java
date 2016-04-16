package com.sony.sel.tvapp.fragment;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.view.ServerCell;
import com.sony.sel.util.ViewUtils;

import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

/**
 * Fragment for choosing the EPG server
 */
public class SelectServerFragment extends BaseFragment {

  public static final String TAG = SelectServerFragment.class.getSimpleName();

  private DlnaHelper dlnaHelper;

  private RecyclerView list;
  private DeviceAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.select_server_fragment, null);

    // setup list and adapter
    list = ViewUtils.findViewById(contentView, android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);

    // set loading state and get device list
    adapter.setLoading();
    getDevices();

    // request focus for the list
    list.requestFocus();

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // stop listening to device list changes
    dlnaHelper.unregisterContentObserver(contentObserver);
  }

  /**
   * Observer to receive notification of changes to the device list.
   */
  private ContentObserver contentObserver = new ContentObserver(null) {
    @Override
    public void onChange(boolean selfChange, Uri uri) {
      super.onChange(selfChange, uri);
      // reload channels on content changes
      Log.d(TAG, "Received notification device list changed.");
      getDevices();
    }
  };

  /**
   * Load or reload the device list.
   */
  private void getDevices() {
    new GetDevicesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class DeviceAdapter extends TvAppAdapter<UpnpDevice, ServerCell> {

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

  /**
   * Async task to get the device list.
   */
  private class GetDevicesTask extends AsyncTask<Void, Void, List<UpnpDevice>> {

    @Override
    protected List<UpnpDevice> doInBackground(Void... params) {
      Log.d(TAG, "Loading device list.");
      return DlnaHelper.getHelper(getActivity()).getDeviceList(contentObserver);
    }

    @Override
    protected void onPostExecute(List<UpnpDevice> deviceList) {
      super.onPostExecute(deviceList);
      adapter.setData(deviceList);
    }
  }
}
