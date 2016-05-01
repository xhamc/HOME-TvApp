package com.sony.sel.tvapp.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.EpgFragment;

/**
 * Standalone activity for launching EPG.
 */
public class EpgActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.epg_activity);

    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    Fragment fragment = new EpgFragment();
    transaction.add(R.id.contentFrame, fragment);
    transaction.commit();

  }

}
