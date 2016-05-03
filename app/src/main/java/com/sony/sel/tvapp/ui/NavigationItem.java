package com.sony.sel.tvapp.ui;

import android.app.Fragment;
import android.content.Context;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.ChannelGridFragment;
import com.sony.sel.tvapp.fragment.EpgFragment;
import com.sony.sel.tvapp.fragment.SearchFragment;
import com.sony.sel.tvapp.fragment.SelectServerFragment;

import java.lang.reflect.InvocationTargetException;

/**
 * List of main navigation items
 */
public enum NavigationItem {

  SEARCH(R.string.search, SearchFragment.class, SearchFragment.TAG),
  CHANNELS(R.string.channels, ChannelGridFragment.class, ChannelGridFragment.TAG),
  GUIDE(R.string.guide, EpgFragment.class, EpgFragment.TAG),
  RECORDINGS(R.string.recordings, null, null),
  VOD(R.string.vod, null, null),
  SETTINGS(R.string.settings, SelectServerFragment.class, SelectServerFragment.TAG);

  private final int titleStringId;
  private final Class<? extends Fragment> fragmentClass;
  private final String tag;

  private Fragment fragment;

  NavigationItem(int titleStringId, Class<? extends Fragment> fragmentClass, String tag) {
    this.titleStringId = titleStringId;
    this.tag = tag;
    this.fragmentClass = fragmentClass;

  }

  public String getTitle(Context context) {
    return context.getString(titleStringId);
  }

  public Fragment getFragment() {
    if (fragment == null && fragmentClass != null) {
      try {
        fragment = fragmentClass.getConstructor(null).newInstance();
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
    return fragment;
  }

  public String getTag() {
    return tag;
  }

}
