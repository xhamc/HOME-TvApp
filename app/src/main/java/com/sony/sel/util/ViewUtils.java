package com.sony.sel.util;

import android.app.Activity;
import android.view.View;

/**
 * View utilities.
 */
public class ViewUtils {

  /**
   * Return the results of findViewById() already
   * cast to the required View subclass.
   *
   * @param view Parent view.
   * @param id   Sub-view ID
   * @param <T>  View subclass.
   * @return The found view.
   */
  @SuppressWarnings("unchecked")
  public static <T extends View> T findViewById(View view, int id) {
    return (T) view.findViewById(id);
  }

  /**
   * Return the results of findViewById() already
   * cast to the required View subclass.
   *
   * @param activity The activity.
   * @param id       Sub-view ID
   * @param <T>      View subclass.
   * @return The found view.
   */
  @SuppressWarnings("unchecked")
  public static <T extends View> T findViewById(Activity activity, int id) {
    return (T) activity.findViewById(id);
  }

}
