package com.sony.sel.tvapp.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.R;

/**
 * Fragment for displaying the EPG.
 */
public class EpgFragment extends BaseFragment {

  public static final String TAG = EpgFragment.class.getSimpleName();

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.epg_fragment, null);


    return contentView;
  }

}
