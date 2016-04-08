package com.sony.sel.tvapp.adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sony.sel.util.ViewUtils;

public class ErrorCell extends FrameLayout implements Bindable<String> {

  public ErrorCell(Context context) {
    super(context);
  }

  public ErrorCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ErrorCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ErrorCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public void bind(String message) {
    TextView textView = ViewUtils.findViewById(this, android.R.id.text1);
    textView.setText(message);
  }

  @Override
  public String getData() {
    TextView textView = ViewUtils.findViewById(this, android.R.id.text1);
    return textView.getText().toString();
  }

}
