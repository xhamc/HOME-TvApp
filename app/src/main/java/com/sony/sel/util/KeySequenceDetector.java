package com.sony.sel.util;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for detecting special key sequences.
 */
public class KeySequenceDetector {

  public static final String LOG_TAG = KeySequenceDetector.class.getSimpleName();

  private final List<Integer> sequence = new ArrayList<>();
  private final long timeout;
  private int currentIndex = 0;

  private Handler timeoutHandler = new Handler();
  private Runnable timeoutRunnable = new Runnable() {
    @Override
    public void run() {
      Log.v(LOG_TAG, "Timer expired after "+timeout+"ms.");
      reset();
    }
  };

  public KeySequenceDetector(@NonNull int[] sequence, long timeout) {
    if (sequence == null || sequence.length == 0) {
      throw new IllegalArgumentException("Key sequence must be non-null and contain at least one element.");
    }
    for (int i : sequence) {
      this.sequence.add(i);
    }
    this.timeout = timeout;
  }

  /**
   * Receive onKeyDown events and attempt to match the sequence.
   *
   * @param keyCode Current key code.
   * @param event   current key event.
   */
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == sequence.get(currentIndex)) {
        // key code matched the next code in the sequence
        Log.v(LOG_TAG, "Matched key code " + keyCode + ", index " + currentIndex + ".");
        currentIndex++;
        if (currentIndex == sequence.size()) {
          // matched entire sequence
          Log.v(LOG_TAG, "Sequence matched.");
          reset();
          return true;
        } else if (currentIndex == 1) {
          // matched the first character, start the timer
          Log.v(LOG_TAG, "First character matched, starting timer.");
          timeoutHandler.postDelayed(timeoutRunnable, timeout);
        }

      } else {
        // reset index
        Log.v(LOG_TAG, "Key code " + keyCode + " did not match.");
        reset();
        if (keyCode == sequence.get(currentIndex)) {
          // re-matched first char
          Log.v(LOG_TAG, "Matched key code " + keyCode + ", index " + currentIndex + ".");
          currentIndex++;
        }
      }
    }
    // not matched yet
    return false;
  }

  private void reset() {
    Log.v(LOG_TAG, "Resetting detector.");
    currentIndex = 0;
    timeoutHandler.removeCallbacks(timeoutRunnable);
  }

}
