package com.sony.sel.tvapp.util;

import android.animation.Animator;
import android.content.Context;
import android.view.View;

/**
 * Helper that manages zooming on view focus
 */
public class FocusHelper {

  public static final String TAG = FocusHelper.class.getSimpleName();

  private static FocusHelper INSTANCE;

  public enum FocusZoomAlignment {
    LEFT,
    CENTER,
    RIGHT
  }

  /**
   * Get the helper instance.
   */
  public static FocusHelper getHelper() {
    if (INSTANCE == null) {
      INSTANCE = new FocusHelper();
    }
    return INSTANCE;
  }


  private FocusHelper() {
  }

  /**
   * Configure an OnFocusChangeListener that zooms the view it's attached to when focus changes.
   *
   * @param alignment       Alignment of zoom.
   * @param focusZoomFactor Amount to zoom (i.e. 1.02f == 102%) when focused.
   * @param unfocusedAlpha  Alpha value when not focused.
   * @return An OnFocusChangeListener can be attached to any view.
   */
  public View.OnFocusChangeListener createFocusZoomListener(final FocusZoomAlignment alignment, final float focusZoomFactor, final float unfocusedAlpha) {
    View.OnFocusChangeListener listener = new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(final View view, boolean hasFocus) {
        if (hasFocus) {
          float translateX = 0.0f;
          float translateY = 0.0f;
          switch (alignment) {
            case LEFT:
              // keep left side aligned
              translateX = ((view.getMeasuredWidth() * focusZoomFactor) - view.getMeasuredWidth()) / 2.0f;
              break;
            case RIGHT:
              // keep right side aligned
              translateY = -((view.getMeasuredWidth() * focusZoomFactor) - view.getMeasuredWidth()) / 2.0f;
              break;
            default:
              break;
          }
          final float finalTranslateX = translateX;
          final float finalTranslateY = translateY;
          view.animate().scaleX(focusZoomFactor).scaleY(focusZoomFactor).alpha(1.0f).translationX(translateX).translationY(translateY).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(final Animator animation) {
              // just change properties to final values if canceled
              view.setScaleX(focusZoomFactor);
              view.setScaleY(focusZoomFactor);
              view.setAlpha(1.0f);
              view.setTranslationX(finalTranslateX);
              view.setTranslationY(finalTranslateY);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
          }).start();
        } else {
          view.animate().scaleX(1.0f).scaleY(1.0f).alpha(unfocusedAlpha).translationX(0.0f).translationY(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(final Animator animation) {
              // just change properties to final values if canceled
              view.setScaleX(1.0f);
              view.setScaleY(1.0f);
              view.setAlpha(unfocusedAlpha);
              view.setTranslationX(0.0f);
              view.setTranslationY(0.0f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
          }).start();
        }
      }
    };
    return listener;
  }

}
