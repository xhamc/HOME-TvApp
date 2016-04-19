package com.sony.sel.tvapp.activity;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.BrowseServersFragment;
import com.sony.sel.tvapp.util.SettingsHelper;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity for DLNA content browsing
 */
public class SelectChannelVideosActivity extends BaseActivity {

  public static final String TAG = SelectChannelVideosActivity.class.getSimpleName();

  @Bind(R.id.done)
  Button done;
  @Bind(R.id.clear)
  Button clear;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.browse_dlna_activity);
    ButterKnife.bind(this);

    done.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    clear.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        SettingsHelper.getHelper(getApplicationContext()).clearChannelVideos();
      }
    });

    // load the servers list fragment first
    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.contentFrame, new BrowseServersFragment());
    transaction.commit();
  }
}