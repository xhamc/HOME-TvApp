package com.sony.sel.tvapp.fragment;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
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
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
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

  private VideoBroadcast currentChannel;
  private boolean currentChannelFocused;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.channel_grid_fragment, null);

    grid = ViewUtils.findViewById(contentView, android.R.id.list);
    layoutManager = new GridLayoutManager(getActivity(), 1, GridLayoutManager.VERTICAL, false);
    grid.setLayoutManager(layoutManager);
    adapter = new ChannelAdapter();
    grid.setAdapter(adapter);

    getChannels();

    return contentView;
  }

  private class ChannelAdapter extends TvAppAdapter<VideoBroadcast, ChannelCell> {

    public ChannelAdapter() {
      super(
          getActivity(),
          R.id.channelCell,
          R.layout.channel_cell,
          getString(R.string.loading),
          getString(R.string.noChannelsFound),
          null,
          false
      );
    }

    @Override
    public void setData(List<VideoBroadcast> data) {
      super.setData(data);
      // scroll to position of current channel
      for (int position = 0; position < data.size(); position++) {
        if (data.get(position).equals(currentChannel)) {
          grid.scrollToPosition(position);
          break;
        }
      }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      super.onBindViewHolder(holder, position);
      if (holder.itemView instanceof ChannelCell) {
        ChannelCell channelCell = (ChannelCell) holder.itemView;
        if (!currentChannelFocused && currentChannel != null && currentChannel.equals(channelCell.getData())) {
          // focus the current channel
          channelCell.requestFocus();
        } else if (!currentChannelFocused && currentChannel == null && position == 0) {
          // focus the first channel in the list
          channelCell.requestFocus();
        }
      }
    }
  }

  private void getChannels() {
    currentChannelFocused = false;
    currentChannel = SettingsHelper.getHelper(getActivity()).getCurrentChannel();
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
      getChannels();
    }
  }

  /**
   * Observer to receive notification of changes to the channel list.
   */
  private ContentObserver channelObserver = new ContentObserver(null) {
    @Override
    public void onChange(boolean selfChange, Uri uri) {
      super.onChange(selfChange, uri);
      // reload channels on content changes
      Log.d(TAG, "Received notification channel list changed.");
      getChannels();
    }
  };

  /**
   * Async task to retrieve the channel list.
   */
  private class GetChannelsTask extends AsyncTask<Void, Void, List<VideoBroadcast>> {

    private final DlnaInterface helper;
    private final String udn;

    public GetChannelsTask(DlnaInterface helper, String udn) {
      this.helper = helper;
      this.udn = udn;
    }

    @Override
    protected List<VideoBroadcast> doInBackground(Void... params) {
      return helper.getChannels(udn, channelObserver);
    }

    @Override
    protected void onPostExecute(List<VideoBroadcast> channels) {
      super.onPostExecute(channels);
      layoutManager.setSpanCount(COLUMN_COUNT);
      adapter.setData(channels);
    }
  }
}
