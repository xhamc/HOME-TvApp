package com.sony.sel.tvapp.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.adapter.TvAppAdapter;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
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

  private DlnaInterface dlnaHelper;

  @Bind(R.id.searchView)
  EditText searchView;

  @Bind(android.R.id.list)
  RecyclerView list;

  private VideoProgramAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    dlnaHelper = DlnaHelper.getHelper(getActivity());

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
    focusSearchText();

    searchView.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // nothing
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0) {
          search(s.toString());
        } else {
          clearSearch();
        }
      }

      @Override
      public void afterTextChanged(Editable s) {
        // nothing
      }
    });

    return contentView;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // disable UI timeout
      EventBus.getInstance().post(new EventBus.CancelUiTimerEvent());
      // reset search text to empty
      searchView.setText("");
      // focus search text
      focusSearchText();
    }
  }

  private void focusSearchText() {
    searchView.requestFocus();
    // show the input UI by faking a click on the text view
    searchView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
    searchView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
  }

  /**
   * Search the EPG server for a specified string.
   */
  private void search(String searchText) {
    new SearchTask(searchText).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
          null,
          false
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


  /**
   * Async task to get the device list.
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
      return dlnaHelper.search(udn, "0/EPG", searchText, VideoProgram.class);
    }

    @Override
    protected void onPostExecute(List<VideoProgram> searchResults) {
      List<VideoProgram> results = new ArrayList<>();
      Date now = new Date();
      for (VideoProgram program : searchResults) {
        if (program.getScheduledEndTime().after(now)) {
          results.add(program);
        }
      }
      list.setAdapter(adapter);
      adapter.setData(results);
    }
  }
}
