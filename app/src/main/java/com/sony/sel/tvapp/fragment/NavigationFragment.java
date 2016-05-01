package com.sony.sel.tvapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.view.NavigationCell;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment that performs navigation
 */
public class NavigationFragment extends BaseFragment {

  public static final String TAG = NavigationFragment.class.getSimpleName();

  @Bind(android.R.id.list)
  RecyclerView recyclerView;

  NavigationAdapter adapter;
  Context appContext;
  NavigationItem currentNavItem;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    appContext = getActivity().getApplicationContext();

    View root = inflater.inflate(R.layout.navigation_fragment, null);

    ButterKnife.bind(this, root);

    if (adapter == null) {
      // fragment is recycled, so only make new adapter the first time
      adapter = new NavigationAdapter();
    }
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(appContext, LinearLayoutManager.HORIZONTAL, false));

    adapter.setup();

    return root;
  }

  public void requestFocus() {
    int index = 0;
    if (currentNavItem != null) {
      index = adapter.indexOf(currentNavItem);
    }
    RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(index);
    if (holder != null && holder.itemView != null) {
      holder.itemView.requestFocus();
    }

  }

  @Subscribe
  public void onNavigationFocusChanged(EventBus.NavigationFocusedEvent event) {
    currentNavItem = event.getItem();
    // reset UI timer while navigating the menu items
    EventBus.getInstance().post(new EventBus.ResetUiTimerShortEvent());
  }

  private class NavigationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<NavigationItem> navigationItems = new ArrayList<>();

    public void setup() {
      navigationItems.clear();
      for (NavigationItem item : NavigationItem.values()) {
        navigationItems.add(item);
      }
      notifyDataSetChanged();
    }

    public NavigationCell getNavigationCell(NavigationItem item) {
      int pos = 0;
      for (NavigationItem navItem : navigationItems) {
        if (navItem == item) {
          return (NavigationCell) recyclerView.getLayoutManager().findViewByPosition(pos);
        }
        pos++;
      }
      return null;
    }

    @Override
    public int getItemViewType(int position) {
      return R.id.navigationCell;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      switch (viewType) {
        case R.id.navigationCell:
          return new RecyclerView.ViewHolder(LayoutInflater.from(appContext).inflate(R.layout.navigation_cell, parent, false)) {
          };
        default:
          return null;
      }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      switch (holder.getItemViewType()) {
        case R.id.navigationCell: {
          NavigationCell cell = (NavigationCell) holder.itemView;
          cell.bind(navigationItems.get(position));
          if (currentNavItem == null) {
            currentNavItem = navigationItems.get(position);
            cell.requestFocus();
          }
        }
        break;
        default:
          break;
      }
    }

    @Override
    public int getItemCount() {
      return navigationItems.size();
    }

    public int indexOf(NavigationItem item) {
      return navigationItems.indexOf(item);
    }
  }
}
