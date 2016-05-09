package com.sony.sel.tvapp.adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.view.SpinnerView;
import com.sony.sel.util.ViewUtils;

public class LoadingCell extends FrameLayout implements Bindable<String> {

  private SpinnerView spinner;
  private TextView loadingMessage;

  public LoadingCell(Context context) {
    super(context);
  }

  public LoadingCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public LoadingCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public LoadingCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    spinner = ViewUtils.findViewById(this, R.id.spinner);
    loadingMessage = ViewUtils.findViewById(this, android.R.id.text1);
  }

  @Override
  public void bind(String data) {
    loadingMessage.setText(data);
    spinner.spin();
  }

  @Override
  public String getData() {
    return loadingMessage.getText().toString();
  }
}
