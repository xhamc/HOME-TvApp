package com.sony.sel.tvapp.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.sony.localserver.ServerService;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.SettingsHelper;

/**
 * Fragment for displaying the EPG.
 */
public class EpgFragment extends BaseFragment {

  public static final String TAG = EpgFragment.class.getSimpleName();

  private WebView webView;

  private BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      onWebServiceStarted();
    }
  };

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.epg_fragment, null);

    webView = (WebView) contentView.findViewById(R.id.webView);

    webView.getSettings().setJavaScriptEnabled(true);
    webView.setInitialScale(100);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

    // on back key, send a "B" keypress to the webview
    // TODO add switch in sockets server to turn this on/off
    webView.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
          // send the key press back to webview as "B" key
          webView.dispatchKeyEvent(new KeyEvent(event.getAction(), KeyEvent.KEYCODE_B));
          return true;
        }
        return false;
      }
    });

    startWebService();

    // long UI hiding
    EventBus.getInstance().post(new EventBus.ResetUiTimerLongEvent());

    return contentView;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    webView.loadUrl("about:blank");
    stopWebService();
    getActivity().unregisterReceiver(receiver);
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      // long UI hiding
      EventBus.getInstance().post(new EventBus.ResetUiTimerLongEvent());
    }
  }

  private void startWebService() {
    // start epg web server
    String udn = SettingsHelper.getHelper(getActivity()).getEpgServer();
    Intent intent = new Intent(getActivity(), ServerService.class);
    intent.setAction(ServerService.START);
    if (udn != null) {
      intent.putExtra(ServerService.EXTRA_UDN, udn);
    }
    getActivity().registerReceiver(receiver, new IntentFilter(ServerService.SERVICE_STARTED));
    getActivity().startService(intent);
  }

  private void stopWebService() {
    Intent intent = new Intent(getActivity(), ServerService.class);
    intent.setAction(ServerService.STOP);
    getActivity().stopService(new Intent(getActivity(), ServerService.class));
  }

  private void onWebServiceStarted() {
    webView.loadUrl("http://127.0.0.1:9000/guide/index.html");
    webView.requestFocus();
  }
}
