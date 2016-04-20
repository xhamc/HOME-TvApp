package com.sony.sel.tvapp.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
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
import com.sony.sel.tvapp.view.BrowseServerCell;
import com.sony.sel.tvapp.view.ServerCell;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for browsing and drilling-down into servers
 */
public class BrowseServersFragment extends BaseFragment {

  public static final String TAG = BrowseServersFragment.class.getSimpleName();

  private DlnaInterface dlnaHelper;

  @Bind(android.R.id.list)
  RecyclerView list;

  private DeviceAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.browse_dlna_fragment, null);
    ButterKnife.bind(this, contentView);

    // setup list and adapter
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);

    // set loading state and get device list
    adapter.setLoading();
    getDevices();

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
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
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

  private class DeviceAdapter extends TvAppAdapter<DlnaObjects.UpnpDevice, BrowseServerCell> {

    public DeviceAdapter() {
      super(
          getActivity(),
          R.id.server_cell,
          R.layout.browse_server_cell,
          getString(R.string.searchingForServers),
          getString(R.string.noServersFound),
          new OnClickListener<DlnaObjects.UpnpDevice, BrowseServerCell>() {
            @Override
            public void onClick(BrowseServerCell view, int position) {
              Fragment fragment = BrowseDlnaFragment.newFragment(view.getData().getUdn(),"0");
              FragmentTransaction transaction = getFragmentManager().beginTransaction();
              transaction.add(R.id.contentFrame,fragment);
              transaction.remove(BrowseServersFragment.this);
              transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
              transaction.addToBackStack(view.getData().getUdn());
              transaction.commit();
            }
          }
      );
    }
  }

  /**
   * Async task to get the device list.
   */
  private class GetDevicesTask extends AsyncTask<Void, Void, List<DlnaObjects.UpnpDevice>> {

    @Override
    protected List<DlnaObjects.UpnpDevice> doInBackground(Void... params) {
      Log.d(TAG, "Loading device list.");
      return dlnaHelper.getDeviceList(contentObserver, false);
    }

    @Override
    protected void onPostExecute(List<DlnaObjects.UpnpDevice> deviceList) {
      super.onPostExecute(deviceList);
      adapter.setData(deviceList);
    }
  }
}
