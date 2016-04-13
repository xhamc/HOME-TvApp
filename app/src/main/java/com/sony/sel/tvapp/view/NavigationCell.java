package com.sony.sel.tvapp.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.sony.sel.tvapp.ui.NavigationItem;
import com.sony.sel.tvapp.util.EventBus;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NavigationCell extends BaseListCell<NavigationItem> {

  private final static float FOCUS_SCALING = 1.2f;

  private NavigationItem navigationItem;

  @Bind(android.R.id.text1)
  TextView label;

  public NavigationCell(Context context) {
    super(context);
  }

  public NavigationCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public NavigationCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public NavigationCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  @Override
  public void bind(final NavigationItem navigationItem) {
    this.navigationItem = navigationItem;
    label.setText(navigationItem.getTitle(getContext()));
    setupFocus(null, FOCUS_SCALING, 0.7f, 1.0f, FocusZoomAlignment.CENTER);
    this.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // we are selected nav item
        setSelected(true);
        EventBus.getInstance().post(new EventBus.NavigationClickedEvent(navigationItem));
      }
    });
  }

  @Override
  public NavigationItem getData() {
    return navigationItem;
  }

  @Override
  protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (gainFocus) {
      EventBus.getInstance().post(new EventBus.NavigationFocusedEvent(navigationItem));
    }
    // clear selected state after focus change
    setSelected(false);
  }

}