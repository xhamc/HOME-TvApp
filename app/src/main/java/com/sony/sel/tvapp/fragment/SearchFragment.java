package com.sony.sel.tvapp.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
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
  EditText searchView;
  @Bind(android.R.id.list)
  RecyclerView list;

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
    focusSearchText();

    searchView.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // nothing
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 1) {
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
    searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          // TODO why isn't it hiding?
          searchView.clearFocus();
          InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
          return true;
        }
        return false;
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
    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(searchView, 0);
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
    searchTask = new SearchTask(searchText);
    searchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void clearSearch() {
    adapter.setData(new ArrayList<DlnaObject>());
  }

  private class VideoProgramAdapter extends TvAppAdapter<DlnaObject, SearchResultCell> {
    public VideoProgramAdapter() {
      super(getActivity(),
          R.id.searchResultCell,
          R.layout.search_result_cell,
          getString(R.string.searching),
          getString(R.string.noItemsFound),
          new OnClickListener<DlnaObject, SearchResultCell>() {
            @Override
            public void onClick(SearchResultCell view, int position) {
              if (view.getData() instanceof VideoProgram) {
                // program popup
                showPopup(view, position);
              } else if (view.getData() instanceof VideoItem) {
                // VOD item
                EventBus.getInstance().post(new EventBus.PlayVodEvent((VideoItem) view.getData()));
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
      }
    }
  }


  /**
   * Async task to get the device list.
   */
  private class SearchTask extends AsyncTask<Void, Void, List<DlnaObject>> {

    private final String searchText;

    public SearchTask(String searchText) {
      this.searchText = searchText;
    }

    @Override
    protected List<DlnaObject> doInBackground(Void... params) {
      String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
      Log.d(TAG, "Searching for \'" + searchText + "\'.");
      List<DlnaObject> results = dlnaHelper.search(udn, "0/EPG", searchText, DlnaObject.class);
      List<DlnaObject> vodResults = dlnaHelper.search(udn, "0/VOD", searchText, DlnaObject.class);
      results.addAll(vodResults);
      return results;
    }

    @Override
    protected void onPostExecute(List<DlnaObject> searchResults) {
      List<DlnaObject> results = new ArrayList<>();
      Date now = new Date();
      for (DlnaObject program : searchResults) {
        if (program instanceof VideoProgram) {
          if (((VideoProgram) program).getScheduledEndTime().after(now)) {
            results.add(program);
          }
        } else {
          // probably VOD item
          results.add(program);
        }
      }
      list.setAdapter(adapter);
      adapter.setData(results);
      searchTask = null;
    }
  }
}
