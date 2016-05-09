package com.sony.sel.tvapp.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.sony.sel.tvapp.R;

/**
 * Animated spinner
 */
public class SpinnerView extends ImageView {

  private Animation animation;

  public SpinnerView(Context context) {
    super(context);
  }

  public SpinnerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SpinnerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SpinnerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    animation = AnimationUtils.loadAnimation(getContext(), R.anim.spinner);
    animation.setDuration(2000);
    animation.setRepeatCount(RotateAnimation.INFINITE);
    animation.setRepeatMode(RotateAnimation.RESTART);
    animation.setInterpolator(new LinearInterpolator());
    startAnimation(animation);
  }

  public void show() {
    setAlpha(0f);
    setVisibility(View.VISIBLE);
    animate().alpha(1.0f).setDuration(1000).start();
    startAnimation(animation);
  }

  public void hide() {
    animate().alpha(0f).setDuration(1000).start();
  }

  public void spin() {
    startAnimation(animation);
  }

}
