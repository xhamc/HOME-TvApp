package com.sony.sel.tvapp.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.ChannelCell;
import com.sony.sel.util.ViewUtils;

import java.util.List;

/**
 * Fragment for displaying the grid of available channels.
 */
public class ChannelGridFragment extends BaseFragment {

  public static final String TAG = ChannelGridFragment.class.getSimpleName();

  private static final int COLUMN_COUNT = 5;

  private RecyclerView grid;
  private ChannelAdapter adapter;
  private GridLayoutManager layoutManager;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.channel_grid_fragment, null);

    grid = ViewUtils.findViewById(contentView, android.R.id.list);
    layoutManager = new GridLayoutManager(getActivity(), 1, GridLayoutManager.VERTICAL, false);
    grid.setLayoutManager(layoutManager);
    adapter = new ChannelAdapter();
    grid.setAdapter(adapter);

    loadChannels();

    return contentView;
  }

  private class ChannelAdapter extends TvAppAdapter<DlnaObjects.VideoBroadcast, ChannelCell> {

    public ChannelAdapter() {
      super(
          getActivity(),
          R.id.channelCell,
          R.layout.channel_cell,
          getString(R.string.loading),
          getString(R.string.noChannelsFound)
      );
    }
  }

  private void loadChannels() {
    layoutManager.setSpanCount(1);
    adapter.setLoading();
    new GetChannelsTask(
        DlnaHelper.getHelper(getActivity()),
        SettingsHelper.getHelper(getActivity()).getEpgServer()
    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // reload channels when re-showing
      loadChannels();
    }
  }

  private class GetChannelsTask extends AsyncTask<Void, Void, List<DlnaObjects.VideoBroadcast>> {

    private final DlnaHelper helper;
    private final String udn;

    public GetChannelsTask(DlnaHelper helper, String udn) {
      this.helper = helper;
      this.udn = udn;
    }

    @Override
    protected List<DlnaObjects.VideoBroadcast> doInBackground(Void... params) {
      return helper.getChannels(udn);
    }

    @Override
    protected void onPostExecute(List<DlnaObjects.VideoBroadcast> channels) {
      super.onPostExecute(channels);
      layoutManager.setSpanCount(COLUMN_COUNT);
      adapter.setData(channels);
    }
  }
}
