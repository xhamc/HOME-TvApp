package com.sony.sel.tvapp.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.sony.sel.tvapp.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for displaying the EPG.
 */
public class EpgFragment extends BaseFragment {

  public static final String TAG = EpgFragment.class.getSimpleName();

  private WebView webView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View contentView = inflater.inflate(R.layout.epg_fragment, null);

    webView = (WebView)contentView.findViewById(R.id.webView);

    webView.getSettings().setJavaScriptEnabled(true);
    webView.setInitialScale(100);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
    webView.loadUrl("http://127.0.0.1:9000/guide/index.html");

    return contentView;
  }

}
