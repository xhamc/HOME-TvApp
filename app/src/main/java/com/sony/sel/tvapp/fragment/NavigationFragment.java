package com.sony.sel.tvapp.fragment;

import android.animation.Animator;
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
import com.sony.sel.tvapp.view.NavigationCell;

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

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    appContext = getActivity().getApplicationContext();

    View root = inflater.inflate(R.layout.navigation_fragment, null);

    ButterKnife.bind(this, root);

    root.setTranslationY(-1.0f);

    if (adapter == null) {
      // fragment is recycled, so only make new adapter the first time
      adapter = new NavigationAdapter();
    }
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(appContext, LinearLayoutManager.HORIZONTAL, false));

    root.setFocusable(true);
    root.setFocusableInTouchMode(true);
    // recyclerView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

    root.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          View navItem = recyclerView.getLayoutManager().findViewByPosition(0);
          if (navItem != null) {
            navItem.requestFocus();
          }
        }
      }
    });

    adapter.setup();

    return root;
  }

  public void show() {
      final View v = getView();
      v.setTranslationY(-v.getMeasuredHeight());
      v.setVisibility(View.VISIBLE);
      v.animate().y(0.0f).setListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
          v.requestFocus();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
      }).start();
  }

  public void hide() {
      final View v = getView();
      v.setY(0.0f);
      v.animate().translationY(-v.getMeasuredHeight()).setListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
          // hide the nav bar to release focus
          v.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
      }).start();
  }

  public boolean isShown() {
    if (getView() != null) {
      return getView().getVisibility() == View.VISIBLE;
    } else {
      return false;
    }
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
  }
}
