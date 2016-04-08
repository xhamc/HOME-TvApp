package com.sony.sel.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for detecting patterns of onScroll gestures.
 * <p/>
 * Instantiate with a GestureDetector.Direction[] containing the sequence of onScroll gestures you
 * want to track (simple right/left/up/down) and receive a callback when the gesture is
 * detected.
 */
public class ScrollSequenceDetector {

  public static final String LOG_TAG = ScrollSequenceDetector.class.getSimpleName();

  /**
   * Interface to receive callback when a gesture is detected.
   */
  public interface GestureListener {
    void onGestureDetected();
  }

  /**
   * Directions for scroll motion.
   */
  public enum Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
  }

  /**
   * Axes for filtering the directions detected.
   */
  public enum Axis {
    HORIZONTAL,
    VERTICAL
  }

  private final List<Direction> detectDirections;
  private final GestureDetector gestureDetector;
  private final GestureListener listener;
  private final Axis axisFilter;

  private List<Direction> scrollDirections = new ArrayList<Direction>();
  private Handler handler = new Handler(Looper.getMainLooper());
  private Runnable clickEraser = new Runnable() {
    @Override
    public void run() {
      scrollDirections.clear();
    }
  };
  private boolean paused;

  /**
   * Constructor.
   *
   * @param context          App context.
   * @param detectDirections Sequence of directions to detect.
   * @param axisFilter       Filter by horizontal or vertical axis (can be null)
   * @param listener         Listener to receive notification when sequence is detected.
   */
  public ScrollSequenceDetector(Context context, Direction[] detectDirections, Axis axisFilter, GestureListener listener) {
    this.detectDirections = Arrays.asList(detectDirections);
    this.listener = listener;
    this.axisFilter = axisFilter;
    gestureDetector = new GestureDetector(context, new ScrollGestureListener());
  }

  /**
   * Forward touch events here from the view that is detecting the gestures.
   *
   * @param motionEvent Motion event from ViewOnTouchListener#onTouchEvent
   * @return true if processed.
   */
  public boolean onTouchEvent(MotionEvent motionEvent) {
    if (!paused) {
      return gestureDetector.onTouchEvent(motionEvent);
    } else {
      return false;
    }
  }

  /**
   * Pause gesture detection.
   */
  public void onPause() {
    scrollDirections.clear();
    handler.removeCallbacks(clickEraser);
    paused = true;
  }

  /**
   * Resume gesture detection.
   */
  public void onResume() {
    paused = false;
  }

  void checkGestures() {
    if (scrollDirections.size() >= detectDirections.size()) {
      for (int i = 0; i < detectDirections.size(); i++) {
        if (scrollDirections.get(i) != detectDirections.get(i)) {
          // no match
          return;
        }
      }
      scrollDirections.clear();
      listener.onGestureDetected();
    }
  }

  private class ScrollGestureListener implements GestureDetector.OnGestureListener {
    @Override
    public boolean onDown(MotionEvent motionEvent) {
      return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
      return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
      handler.removeCallbacks(clickEraser);
      if (Math.abs(v) > 5.0f && Math.abs(v2) > 5.0f) {
        // only detect larger gestures (higher velocity)
        Direction scroll = null;
        if (axisFilter == Axis.HORIZONTAL || (axisFilter == null && Math.abs(v) > Math.abs(v2))) {
          // primary axis = horizontal
          scroll = v > 0 ? Direction.LEFT : Direction.RIGHT;
        }
        if (axisFilter == Axis.VERTICAL || (axisFilter == null && Math.abs(v) < Math.abs(v2))) {
          // primary axis = vertical
          scroll = v2 > 0 ? Direction.UP : Direction.DOWN;
        }
        if (scroll != null) {
          if (scrollDirections.size() > 0) {
            Direction lastScroll = scrollDirections.get(scrollDirections.size() - 1);
            if (!lastScroll.equals(scroll)) {
              scrollDirections.add(scroll);
            }
          } else {
            scrollDirections.add(scroll);
          }
        }
        Log.d(LOG_TAG, "Scroll: " + (scroll != null ? scroll : "null") + " v=" + v + " v2=" + v2);
        checkGestures();
      }
      handler.postDelayed(clickEraser, 50);
      return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
      return true;
    }
  }
}
