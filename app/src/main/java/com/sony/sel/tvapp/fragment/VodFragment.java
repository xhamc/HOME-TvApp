package com.sony.sel.tvapp.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.BrowseDlnaCell;
import com.sony.sel.tvapp.view.VodCell;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for browsing VOD files.
 */
public class VodFragment extends BaseFragment {

  public static final String TAG = VodFragment.class.getSimpleName();

  @Bind(android.R.id.list)
  RecyclerView list;

  private DlnaInterface dlnaHelper;
  private SettingsHelper settingsHelper;

  private VodAdapter adapter;
  private String dlnaContainerId;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());
    settingsHelper = SettingsHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.vod_fragment, null);

    ButterKnife.bind(this, contentView);

    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new VodAdapter();
    list.setAdapter(adapter);

    // disable UI timeout
    EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
    // show VOD root
    drillDown("0/VOD");

    return contentView;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // disable UI timeout
      EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
      // show VOD root
      drillDown("0/VOD");
    }
  }

  private class VodAdapter extends TvAppAdapter<DlnaObject, VodCell> {
    public VodAdapter() {
      super(getActivity(),
          R.id.vodCell,
          R.layout.vod_cell,
          getString(R.string.loading),
          getString(R.string.noItemsFound),
          new OnClickListener<DlnaObject, VodCell>() {
            @Override
            public void onClick(VodCell view, int position) {
              if (DlnaObjects.DlnaClass.CONTAINER.isKindOf(view.getData())) {
                // browse
                drillDown(view.getData().getId());
              } else {
                // send event to play video item
                EventBus.getInstance().post(new EventBus.PlayVodEvent((VideoItem) view.getData()));
              }
            }
          }
      );
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      super.onBindViewHolder(holder, position);
      holder.itemView.setNextFocusLeftId(R.id.searchView);
      if (position == 0) {
        holder.itemView.setNextFocusUpId(R.id.searchView);
      }
    }
  }

  private void drillDown(String parentId) {
    adapter.setLoading();
    new GetDlnaChildrenTask(settingsHelper.getEpgServer(), parentId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class GetDlnaChildrenTask extends AsyncTask<Void, Void, List<DlnaObject>> {

    private final String parentId;
    private final String udn;


    public GetDlnaChildrenTask(String udn, String parentId) {
      this.parentId = parentId;
      this.udn = udn;
    }

    @Override
    protected List<DlnaObject> doInBackground(Void... params) {
      return dlnaHelper.getChildren(udn, parentId, DlnaObject.class, null, true);
    }

    @Override
    protected void onPostExecute(List<DlnaObject> dlnaObjects) {
      super.onPostExecute(dlnaObjects);
      adapter.setData(dlnaObjects);
      dlnaContainerId = parentId;
    }
  }


}
