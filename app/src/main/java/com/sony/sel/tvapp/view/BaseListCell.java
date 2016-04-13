package com.sony.sel.tvapp.view;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.sony.sel.tvapp.adapter.Bindable;


/**
 * Base class for TV Care List Cells.
 * <p/>
 * The primary job of this class is to provide consistent and reusable focus animation behavior to subclasses.
 * <p/>
 * One of the {@link #setupFocus} methods should be called from
 * {@link #onFinishInflate()} of derived classes to configure the focus animation behavior.
 */
public abstract class BaseListCell<T> extends FrameLayout implements Bindable<T> {

  protected enum FocusZoomAlignment {
    LEFT,
    CENTER,
    RIGHT
  }

  private float focusZoomFactor;
  private float unfocusedAlpha;
  private float selectedAlpha;
  private FocusZoomAlignment alignment;
  private View actions;
  private boolean focusEnabled;

  private static final float DEFAULT_FOCUS_ZOOM = 1.05f;
  private static final float DEFAULT_UNFOCUSED_ALPHA = 0.8f;

  public BaseListCell(Context context) {
    super(context);
  }

  public BaseListCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BaseListCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public BaseListCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  /**
   * Set up focus for the list cell.
   * Configures focus listener, cell zooming and revealing the action item view when focused.
   *
   * @param actions        View that contains action items to be hidden/shown on focus
   * @param zoomFactor     Amount to zoom the cell when it's focused.
   * @param unfocusedAlpha Alpha of list cell when unfocused;
   */
  protected void setupFocus(final View actions, float zoomFactor, final float unfocusedAlpha, final float selectedAlpha, final FocusZoomAlignment alignment) {

    this.focusZoomFactor = zoomFactor;
    this.unfocusedAlpha = unfocusedAlpha;
    this.selectedAlpha = selectedAlpha;
    this.alignment = alignment;
    this.actions = actions;
    this.focusEnabled = true;

    // make the entire item focusable
    this.setFocusable(true);
    this.setFocusableInTouchMode(true);

    // set initial alpha
    this.setAlpha(this.hasFocus() ? 1.0f : unfocusedAlpha);

    if (actions != null) {
      // set initial actions alpha
      actions.setAlpha(this.hasFocus() ? 1.0f : 0.0f);
    }
  }

  protected void disableFocus() {
    this.focusEnabled = false;
    this.setFocusable(false);
    this.setFocusableInTouchMode(false);
  }

  protected void setupFocus() {
    setupFocus(null, DEFAULT_FOCUS_ZOOM, DEFAULT_UNFOCUSED_ALPHA, DEFAULT_UNFOCUSED_ALPHA, FocusZoomAlignment.CENTER);
  }

  protected void setupFocus(final View actions) {
    setupFocus(actions, DEFAULT_FOCUS_ZOOM, DEFAULT_UNFOCUSED_ALPHA, DEFAULT_UNFOCUSED_ALPHA, FocusZoomAlignment.CENTER);
  }

  protected void setupFocus(final View actions, float focusZoomFactor) {
    setupFocus(actions, focusZoomFactor, DEFAULT_UNFOCUSED_ALPHA, DEFAULT_UNFOCUSED_ALPHA, FocusZoomAlignment.CENTER);
  }

  @Override
  protected void onFocusChanged(boolean hasFocus, int direction, Rect previouslyFocusedRect) {
    if (!focusEnabled) {
      // not configured
      return;
    }
    if (hasFocus) {
      float translateX = 0.0f;
      float translateY = 0.0f;
      switch (alignment) {
        case LEFT:
          // keep left side aligned
          translateX = ((getMeasuredWidth() * focusZoomFactor) - getMeasuredWidth()) / 2.0f;
          break;
        case RIGHT:
          // keep right side aligned
          translateY = -((getMeasuredWidth() * focusZoomFactor) - getMeasuredWidth()) / 2.0f;
          break;
        default:
          break;
      }
      final float finalTranslateX = translateX;
      final float finalTranslateY = translateY;
      animate().scaleX(focusZoomFactor).scaleY(focusZoomFactor).alpha(1.0f).translationX(translateX).translationY(translateY).setListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(final Animator animation) {
          // just change properties to final values if canceled
          setScaleX(focusZoomFactor);
          setScaleY(focusZoomFactor);
          setAlpha(1.0f);
          setTranslationX(finalTranslateX);
          setTranslationY(finalTranslateY);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
      }).start();
      if (actions != null) {
        actions.animate().alpha(1.0f).start();
      }
    } else {
      animate().scaleX(1.0f).scaleY(1.0f).alpha(isSelected() ? selectedAlpha : unfocusedAlpha).translationX(0.0f).translationY(0.0f).setListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(final Animator animation) {
          // just change properties to final values if canceled
          setScaleX(1.0f);
          setScaleY(1.0f);
          setAlpha(unfocusedAlpha);
          setTranslationX(0.0f);
          setTranslationY(0.0f);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
      }).start();
      if (actions != null) {
        actions.animate().alpha(0.0f).start();
      }
    }
    super.onFocusChanged(hasFocus, direction, previouslyFocusedRect);
  }



}
