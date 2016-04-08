// Copyright (C) 2013 Sony Mobile Communications AB.
// All rights, including trade secret rights, reserved.

package com.sony.sel.util;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

public class DisplayHelper {
    private final WindowManager mWindowManager;
    private final Resources mResources;

    public DisplayHelper(WindowManager windowManager, Resources resources) {
        mWindowManager = windowManager;
        mResources = resources;
    }

    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    /**
     * Return the width and height of the primary display.
     */
    public Point getDisplaySize() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        return size;
    }

    public Point getRealDisplaySize() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(size);
        return size;
    }

    public int dipsToPixels(int dips) {
        final float scale = mResources.getDisplayMetrics().density;
        return Math.round(dips * scale);
    }

    /**
     * Return the rotation of the primary display in degrees.
     */
    public int getDisplayRotation() {
        int mRotation = mWindowManager.getDefaultDisplay().getRotation();
        switch (mRotation) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

  /**
   * Return the display orientation as ActivityInfo SCREEN_ORIENTATION_XXXX values.
   */
  public int getDisplayOrientation() {
    int mRotation = mWindowManager.getDefaultDisplay().getRotation();
    switch (mRotation) {
      case Surface.ROTATION_90:
        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      case Surface.ROTATION_180:
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
      case Surface.ROTATION_270:
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
      default:
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }
  }

    public boolean isLandscape() {
        int displayRotation = getDisplayRotation();
        return displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270;
    }
}
