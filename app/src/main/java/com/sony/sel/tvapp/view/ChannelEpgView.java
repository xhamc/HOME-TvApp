package com.sony.sel.tvapp.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.FocusHelper;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import static com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;

/**
 * View for showing past, current and future EPG for a channel.
 */
public class ChannelEpgView extends FrameLayout {

  public static final String TAG = ChannelEpgView.class.getSimpleName();

  /// zoom for focused sub-items
  private static final float FOCUS_ZOOM = 1.05f;

  private List<VideoProgram> epgData;

  @Bind(R.id.currentProgram)
  ProgramInfoView programInfoView;
  @Bind(R.id.nowPlaying)
  TextView nowPlaying;
  @Bind(R.id.upNext)
  TextView upNext;
  @Bind(R.id.upNextLayout)
  LinearLayout upNextLayout;
  @Bind(R.id.scrollView)
  HorizontalScrollView scrollView;

  private SettingsHelper settingsHelper;

  public ChannelEpgView(Context context) {
    super(context);
  }

  public ChannelEpgView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChannelEpgView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ChannelEpgView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    if (isInEditMode()) {
      return;
    }
    ButterKnife.bind(this);
    settingsHelper = SettingsHelper.getHelper(getContext());
    programInfoView.setOnFocusChangeListener(
        FocusHelper.getHelper().createFocusZoomListener(FocusHelper.FocusZoomAlignment.CENTER, FOCUS_ZOOM, 1.0f)
    );
  }

  public void bind(final VideoBroadcast channel, List<VideoProgram> data) {
    epgData = data;

    // configure the current program
    VideoProgram currentProgram = getCurrentProgram();
    nowPlaying.setVisibility(currentProgram != null ? View.VISIBLE : View.INVISIBLE);
    programInfoView.bind(currentProgram, channel);
    programInfoView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        showChannelPopup(v, channel);
      }
    });

    // configure the "up next" programs
    List<VideoProgram> nextPrograms = getNextPrograms();
    upNext.setVisibility(nextPrograms.size() > 0 ? View.VISIBLE : View.INVISIBLE);
    upNextLayout.removeAllViews();
    for (final VideoProgram program : nextPrograms) {
      final ProgramInfoView upNext = (ProgramInfoView) View.inflate(getContext(), R.layout.program_info_view_small, null);
      upNext.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          showProgramPopup(v, program);
        }
      });
      upNext.bind(program, channel);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
      params.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.channelThumbPadding));
      upNextLayout.addView(upNext, params);
      final OnFocusChangeListener listener = FocusHelper.getHelper().createFocusZoomListener(FocusHelper.FocusZoomAlignment.CENTER, FOCUS_ZOOM, 1.0f);
      upNext.setOnFocusChangeListener(listener);
    }
    // focus the main view
    programInfoView.requestFocus();
    // reset scroll
    scrollView.scrollTo(0, 0);
  }

  private void showChannelPopup(View v, final VideoBroadcast channel) {
    PopupMenu menu = new PopupMenu(getContext(), v);
    menu.inflate(R.menu.channel_popup_menu);
    if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
      menu.getMenu().removeItem(R.id.addToFavoriteChannels);
    } else {
      menu.getMenu().removeItem(R.id.removeFromFavoriteChannels);
    }
    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.addToFavoriteChannels:
            settingsHelper.addFavoriteChannel(channel.getChannelId());
            // re-bind to update UI
            programInfoView.bind(getCurrentProgram(),channel);
            return true;
          case R.id.removeFromFavoriteChannels:
            settingsHelper.removeFavoriteChannel(channel.getChannelId());
            // re-bind to update UI
            programInfoView.bind(getCurrentProgram(),channel);
            return true;
        }
        return false;
      }
    });
    menu.show();
  }

  private void showProgramPopup(View v, VideoProgram program) {
    PopupMenu menu = new PopupMenu(getContext(), v);
    menu.inflate(R.menu.program_popup_menu);
    menu.show();
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    boolean result = super.requestFocus(direction, previouslyFocusedRect);
    programInfoView.requestFocus();
    scrollView.scrollTo(0, 0);
    return result;
  }

  /**
   * Return the currently running program.
   *
   * @return Current program, or null if none was found.
   */
  @Nullable
  private VideoProgram getCurrentProgram() {
    Date now = new Date();
    if (epgData != null) {
      for (VideoProgram program : epgData) {
        if (program.getScheduledStartTime().before(now) && program.getScheduledEndTime().after(now)) {
          return program;
        }
      }
    }
    // not found
    return null;
  }

  /**
   * Return the upcoming programs.
   *
   * @return List of upcoming programs, an empty list if none are found.
   */
  @NonNull
  private List<VideoProgram> getNextPrograms() {
    List<VideoProgram> programs = new ArrayList<>();
    if (epgData != null) {
      Date now = new Date();
      for (VideoProgram program : epgData) {
        if (program.getScheduledStartTime().after(now)) {
          programs.add(program);
        }
      }
    }
    return programs;
  }

  @NonNull
  private List<VideoProgram> getPastPrograms() {
    List<VideoProgram> programs = new ArrayList<>();
    Date now = new Date();
    for (VideoProgram program : epgData) {
      if (program.getScheduledEndTime().before(now)) {
        programs.add(program);
      }
    }
    return programs;
  }

}
