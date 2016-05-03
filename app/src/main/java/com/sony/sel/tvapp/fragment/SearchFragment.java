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
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.BrowseServerCell;
import com.sony.sel.tvapp.view.ServerCell;
import com.sony.sel.util.ViewUtils;

import java.util.List;

import butterknife.Bind;

import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

/**
 * Fragment for choosing the EPG server
 */
public class SearchFragment extends BaseFragment {

  public static final String TAG = SearchFragment.class.getSimpleName();

  private DlnaInterface dlnaHelper;

  @Bind(android.R.id.list)
  RecyclerView list;

  private DeviceAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.search_fragment, null);

    // setup list and adapter
    list = ViewUtils.findViewById(contentView, android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);

    return contentView;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // disable UI timeout
      EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
      adapter.setLoading();
      getDevices();
    }
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

  private void searchServer(String udn) {
    // test: search for all objects on a server
    List<DlnaObject> results = DlnaHelper.getHelper(getActivity()).search(udn, "0", "*", DlnaObject.class);
    Log.d(TAG, String.format("%d item(s) found.", results.size()));
    for (DlnaObject result : results) {
      Log.d(TAG, result.toString());
    }

  }

  /**
   * Load or reload the device list.
   */
  private void getDevices() {
    new GetDevicesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class DeviceAdapter extends TvAppAdapter<UpnpDevice, BrowseServerCell> {

    public DeviceAdapter() {
      super(
          getActivity(),
          R.id.server_cell,
          R.layout.browse_server_cell,
          getString(R.string.searchingForServers),
          getString(R.string.noServersFound),
          new OnClickListener<UpnpDevice, BrowseServerCell>() {
            @Override
            public void onClick(BrowseServerCell view, int position) {
              searchServer(view.getData().getUdn());
            }
          }
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
      return DlnaHelper.getHelper(getActivity()).getDeviceList(contentObserver, false);
    }

    @Override
    protected void onPostExecute(List<UpnpDevice> deviceList) {
      super.onPostExecute(deviceList);
      adapter.setData(deviceList);
    }
  }
}
