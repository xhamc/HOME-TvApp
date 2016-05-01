package com.sony.sel.tvapp.activity;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

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
  @Bind(R.id.channelVid)
  CheckBox channelVid;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.browse_dlna_activity);
    ButterKnife.bind(this);
    if (!SettingsHelper.getHelper(getApplicationContext()).useChannelVideosSetting()){
      channelVid.setChecked(false);
    }else{
      channelVid.setChecked(true);
    }
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

    channelVid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        SettingsHelper.getHelper(getApplicationContext()).setToChannelVideoSetting(b);
      }
    });

    // load the servers list fragment first
    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.contentFrame, new BrowseServersFragment());
    transaction.commit();
  }
}