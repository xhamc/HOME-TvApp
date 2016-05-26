package com.sony.sel.tvapp.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.media.tv.TvInputManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.activity.MainActivity;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.menu.PopupHelper;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvapp.view.SearchResultCell;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * Fragment for searching EPG
 */
public class SearchFragment extends BaseFragment {

  public static final String TAG = SearchFragment.class.getSimpleName();


  @Bind(R.id.searchView)
  SearchView searchView;
  @Bind(android.R.id.list)
  RecyclerView list;
  EditText searchViewEditText;

  private DlnaInterface dlnaHelper;
  private SettingsHelper settingsHelper;
  private SearchTask searchTask;

  private VideoProgramAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());
    settingsHelper = SettingsHelper.getHelper(getActivity());

    View contentView = inflater.inflate(R.layout.search_fragment, null);

    ButterKnife.bind(this, contentView);

    list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    list.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          if (list.getChildAt(0) != null) {
            list.getChildAt(0).requestFocus();
          }
        }
      }
    });
    adapter = new VideoProgramAdapter();

    // disable UI timeout
    EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        hideSoftKeyboard(searchViewEditText);
        list.requestFocus();
        search(query);
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });

    int id = searchView.getContext()
        .getResources()
        .getIdentifier("android:id/search_src_text", null, null);
    searchViewEditText = (EditText) searchView.findViewById(id);
    if (searchViewEditText != null) {
      searchViewEditText.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          showSoftKeyboard(v);
        }
      });
    }

    // extract search configuration from manifest
    SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
    ComponentName componentName = new ComponentName(getActivity().getApplicationContext(), MainActivity.class);
    SearchableInfo info = searchManager.getSearchableInfo(componentName);
    searchView.setSearchableInfo(info);

    setVoiceSearchResult();

    // disable UI timeout
    EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());

    return contentView;
  }

  private void setVoiceSearchResult() {
    if (settingsHelper.getCurrentSearchQuery() != null) {
      String query = settingsHelper.getCurrentSearchQuery();
      settingsHelper.setCurrentSearchQuery(null);
      searchView.setQuery(query, false);
      search(query);
      list.requestFocus();
    } else {
      searchView.requestFocus();
    }
  }

  private void showSoftKeyboard(View v) {
    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(v, 0);
  }

  private void hideSoftKeyboard(View v) {
    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // set search text
      setVoiceSearchResult();
      // disable UI timeout
      EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
    } else if (searchView != null) {
      // clear the search when hiding
      searchView.setQuery("", false);
      clearSearch();
    }
  }

  private void showPopup(SearchResultCell cell, int position) {
    final DlnaObject program = cell.getData();

    // all menu items are hidden by default

    if (program instanceof VideoProgram) {
      // popup for an EPG item
      PopupHelper.getHelper(getActivity()).showPopup((VideoProgram) program, cell);
    } else if (program instanceof VideoItem) {
      // popup for a VOD item
      PopupHelper.getHelper(getActivity()).showPopup((VideoItem) program, cell);
    }
  }

  /**
   * Search the EPG server for a specified string.
   */
  private void search(String searchText) {
    if (searchTask != null) {
      searchTask.cancel(true);
    }
    adapter.setLoading();
    searchTask = new SearchTask(searchText);
    searchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void clearSearch() {
    adapter.setData(new ArrayList<VideoProgram>());
  }

  private class VideoProgramAdapter extends TvAppAdapter<VideoProgram, SearchResultCell> {
    public VideoProgramAdapter() {
      super(getActivity(),
          R.id.searchResultCell,
          R.layout.search_result_cell,
          getString(R.string.searching),
          getString(R.string.noItemsFound),
          new OnClickListener<VideoProgram, SearchResultCell>() {
            @Override
            public void onClick(SearchResultCell view, int position) {
              if (view.getData().getId().startsWith("0/VOD")) {
                // VOD item
                EventBus.getInstance().post(new EventBus.PlayVodEvent(view.getData()));
              } else {
                // program popup
                showPopup(view, position);
              }
            }
          },
          false
      );
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      super.onBindViewHolder(holder, position);
      holder.itemView.setNextFocusLeftId(R.id.searchView);
      if (position == 0) {
        holder.itemView.setNextFocusUpId(R.id.searchView);
        if (holder.itemView instanceof SearchResultCell && list.hasFocus()) {
          holder.itemView.requestFocus();
        }
      }
    }
  }


  /**
   * Async task to get the search results.
   */
  private class SearchTask extends AsyncTask<Void, Void, List<VideoProgram>> {

    private final String searchText;

    public SearchTask(String searchText) {
      this.searchText = searchText;
    }

    @Override
    protected List<VideoProgram> doInBackground(Void... params) {
      String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
      Log.d(TAG, "Searching for \'" + searchText + "\'.");
      List<VideoProgram> results = dlnaHelper.search(udn, "0/EPG", searchText, VideoProgram.class);
      List<VideoProgram> vodResults = dlnaHelper.search(udn, "0/VOD", searchText, VideoProgram.class);
      results.addAll(vodResults);
      return results;
    }

    @Override
    protected void onPostExecute(List<VideoProgram> searchResults) {
      List<VideoProgram> results = new ArrayList<>();
      Date now = new Date();
      for (VideoProgram program : searchResults) {
        if (program.getId().startsWith("0/VOD")) {
          // a VOD item
          results.add(program);
        } else {
          // an EPG item
          if (program.getScheduledEndTime().after(now)) {
            results.add(program);
          }
        }
      }
      list.setAdapter(adapter);
      adapter.setData(results);
      searchTask = null;
    }
  }

  void blah() {
    TvInputManager manager = (TvInputManager) getActivity().getSystemService(Activity.TV_INPUT_SERVICE);
    manager.getTvInputList();
  }
}
