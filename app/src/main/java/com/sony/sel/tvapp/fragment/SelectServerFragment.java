package com.sony.sel.tvapp.fragment;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.ServerCell;
import com.sony.sel.util.ViewUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.UpnpDevice;

/**
 * Fragment for choosing the EPG server
 */
public class SelectServerFragment extends BaseFragment {

  public static final String TAG = SelectServerFragment.class.getSimpleName();

  private DlnaInterface dlnaHelper;

  @Bind(android.R.id.list)
  RecyclerView list;
  @Bind(R.id.channelVid)
  CheckBox channelVideo;
  @Bind(R.id.autoPlay)
  CheckBox autoPlay;

  private DeviceAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.select_server_fragment, null);
    ButterKnife.bind(this, contentView);

    // setup list and adapter
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DeviceAdapter();
    list.setAdapter(adapter);
    list.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          ViewHolder holder = list.findViewHolderForAdapterPosition(0);
          if (holder != null && holder.getItemViewType() == R.id.server_cell) {
            holder.itemView.requestFocus();
          }
        }
      }
    });

    // set up check boxes
    final SettingsHelper settingsHelper = SettingsHelper.getHelper(getActivity());
    channelVideo.setChecked(settingsHelper.useChannelVideosSetting());
    channelVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        settingsHelper.setToChannelVideoSetting(isChecked);
      }
    });
    autoPlay.setChecked(settingsHelper.getAutoPlay());
    autoPlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        settingsHelper.setAutoPlay(isChecked);
      }
    });

    // set loading state and get device list
    adapter.setLoading();
    getDevices();

    // disable UI timeout
    EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());

    return contentView;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // disable UI timeout
      EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
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
          getString(R.string.noServersFound),
          new OnClickListener<UpnpDevice, ServerCell>() {
            @Override
            public void onClick(ServerCell view, int position) {
              SettingsHelper.getHelper(getActivity()).setEpgServer(view.getData().getUdn());
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
      list.requestFocus();
    }
  }
}
