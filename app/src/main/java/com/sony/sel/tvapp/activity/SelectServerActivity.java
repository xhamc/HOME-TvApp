package com.sony.sel.tvapp.activity;

import android.app.FragmentTransaction;
import android.os.Bundle;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.fragment.SelectServerFragment;
import com.sony.sel.tvapp.service.DlnaService;
import com.sony.sel.tvapp.util.EventBus;
import com.squareup.otto.Subscribe;

/**
 * Activity to select the EPG server.
 */
public class SelectServerActivity extends BaseActivity {

  public static final String TAG = SelectServerActivity.class.getSimpleName();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.select_server_activity);

    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.contentFrame, new SelectServerFragment());
    transaction.commit();

    // make sure DLNA service is started
    DlnaService.startService(this);
  }

  @Subscribe
  public void onServerSelected(EventBus.EpgServerChangedEvent event) {
    finish();
  }

}
