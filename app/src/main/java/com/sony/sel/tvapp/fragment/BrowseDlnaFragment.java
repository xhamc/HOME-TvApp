package com.sony.sel.tvapp.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
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
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.BrowseDlnaCell;
import com.sony.sel.util.ViewUtils;

import java.util.List;

import static com.sony.sel.tvapp.util.DlnaObjects.Container;
import static com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoItem;

/**
 * Fragment for browsing a level of a DLNA server.
 */
public class BrowseDlnaFragment extends BaseFragment {

  public static final String TAG = BrowseDlnaFragment.class.getSimpleName();

  public static final String UDN = "udn";
  public static final String PARENT_ID = "parentId";

  private DlnaHelper dlnaHelper;
  private String udn;
  private String parentId;
  private RecyclerView list;
  private DlnaAdapter adapter;
  private SettingsHelper settingsHelper;

  /**
   * Factory method to create the fragment with required arguments.
   * @param udn
   * @param parentId
   * @return
   */
  public static BrowseDlnaFragment newFragment(String udn, String parentId) {
    BrowseDlnaFragment fragment = new BrowseDlnaFragment();
    Bundle bundle = new Bundle();
    bundle.putString(UDN,udn);
    bundle.putString(PARENT_ID, parentId);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());
    settingsHelper = SettingsHelper.getHelper(getActivity());

    udn = getArguments().getString(UDN);
    parentId = getArguments().getString(PARENT_ID);

    View contentView = inflater.inflate(R.layout.browse_dlna_fragment, null);

    // setup list and adapter
    list = ViewUtils.findViewById(contentView, android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    adapter = new DlnaAdapter();
    list.setAdapter(adapter);

    // set loading state and get device list
    adapter.setLoading();
    getChildren();

    return contentView;
  }

  /**
   * Load or reload the device list.
   */
  private void getChildren() {
    new GetDlnaChildrenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class DlnaAdapter extends TvAppAdapter<DlnaObject, BrowseDlnaCell> {

    public DlnaAdapter() {
      super(
          getActivity(),
          R.id.dlnaCell,
          R.layout.browse_dlna_cell,
          getString(R.string.loading),
          getString(R.string.noItemsFound),
          new OnClickListener<DlnaObject, BrowseDlnaCell>() {
            @Override
            public void onClick(BrowseDlnaCell view, int position) {
              if (view.getData() instanceof Container) {
                // drill down into container
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = BrowseDlnaFragment.newFragment(udn, view.getData().getId());
                transaction.add(R.id.contentFrame, fragment);
                transaction.remove(BrowseDlnaFragment.this);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.addToBackStack(view.getData().getId());
                transaction.commit();
              } else if (view.getData() instanceof VideoItem) {
                // select or de-select the video
                VideoItem videoItem = (VideoItem) view.getData();
                if (settingsHelper.getChannelVideos().contains(videoItem)) {
                  settingsHelper.removeChannelVideo(videoItem);
                } else {
                  settingsHelper.addChannelVideo(videoItem);
                }
                // re-bind to update view
                view.bind(view.getData());
              }
            }
          }
      );
    }
  }

  /**
   * Async task to get the device list.
   */
  private class GetDlnaChildrenTask extends AsyncTask<Void, Void, List<DlnaObject>> {

    @Override
    protected List<DlnaObject> doInBackground(Void... params) {
      Log.d(TAG, "Loading device list.");
      return (List<DlnaObject>) (List<?>) dlnaHelper.getChildren(udn, parentId, VideoItem.class, null, false);
    }

    @Override
    protected void onPostExecute(List<DlnaObject> dlnaObjects) {
      super.onPostExecute(dlnaObjects);
      adapter.setData(dlnaObjects);
    }
  }
}
