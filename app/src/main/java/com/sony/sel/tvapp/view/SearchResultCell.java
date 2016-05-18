package com.sony.sel.tvapp.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects.DlnaObject;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.EventBus;
import com.sony.sel.tvapp.util.EventBus.RecordingsChangedEvent;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * View
 */
public class SearchResultCell extends BaseListCell<DlnaObject> {

  private DlnaObject program;
  private SettingsHelper settingsHelper;

  @Bind(R.id.title)
  TextView title;
  @Bind(R.id.programTitle)
  TextView programTitle;
  @Bind(R.id.time)
  TextView time;
  @Bind(R.id.description)
  TextView description;
  @Bind(R.id.icon)
  ImageView icon;
  @Bind(R.id.recordProgram)
  ImageView recordProgram;
  @Bind(R.id.recordSeries)
  ImageView recordSeries;
  @Bind(R.id.favoriteProgram)
  View favoriteProgram;
  @Bind(R.id.favoriteChannel)
  View favoriteChannel;

  public SearchResultCell(Context context) {
    super(context);
  }

  public SearchResultCell(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SearchResultCell(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SearchResultCell(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    if (isInEditMode()) {
      return;
    }
    ButterKnife.bind(this);
    setupFocus(null, 1.03f);
    settingsHelper = SettingsHelper.getHelper(getContext());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    EventBus.getInstance().register(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    EventBus.getInstance().unregister(this);
  }

  @Override
  public void bind(DlnaObject data) {
    program = data;

    // main title
    title.setText(data.getTitle());

    // program title
    String progTitle = null;
    if (data instanceof VideoProgram) {
      progTitle = ((VideoProgram) data).getProgramTitle();
    } else if (data instanceof VideoItem) {
      progTitle = ((VideoItem) data).getProgramTitle();
    }
    if (progTitle != null && progTitle.length() > 0) {
      programTitle.setVisibility(View.VISIBLE);
      programTitle.setText(progTitle);
    } else {
      programTitle.setVisibility(View.GONE);
    }

    // time
    if (data instanceof VideoProgram) {
      // display program date/time
      String t = new SimpleDateFormat("M/d/yy h:mm a").format(((VideoProgram) data).getScheduledStartTime());
      time.setText(t);
    } else {
      // video on demand
      time.setText(R.string.onDemand);
    }

    // description
    String desc = null;
    if (data instanceof VideoProgram) {
      desc = ((VideoProgram) data).getLongDescription();
    } else if (data instanceof VideoItem) {
      desc = ((VideoItem) data).getLongDescription();
    }
    if (desc != null && desc.length() > 0) {
      description.setVisibility(View.VISIBLE);
      description.setText(desc);
    } else {
      description.setVisibility(View.GONE);
    }

    // recording icons
    recordSeries.setVisibility(View.GONE);
    recordProgram.setVisibility(View.GONE);
    favoriteChannel.setVisibility(View.GONE);
    favoriteProgram.setVisibility(View.GONE);
    if (program instanceof VideoProgram) {
      if (settingsHelper.isSeriesRecorded((VideoProgram) program)) {
        recordSeries.setVisibility(View.VISIBLE);
      } else if (settingsHelper.isProgramRecorded((VideoProgram) program)) {
        recordProgram.setVisibility(View.VISIBLE);
      }
      if (settingsHelper.isFavoriteProgram((VideoProgram)program)) {
        favoriteProgram.setVisibility(View.VISIBLE);
      }
      if (settingsHelper.getFavoriteChannels().contains(((VideoProgram)program).getChannelId())) {
        favoriteProgram.setVisibility(View.VISIBLE);
      }
    }


    // thumbnail icon
    if (program.getIcon() != null && program.getIcon().length() > 0) {
      Picasso.with(getContext()).load(Uri.parse(program.getIcon())).into(icon);
    } else {
      icon.setImageDrawable(null);
    }

  }

  @Override
  public DlnaObject getData() {
    return program;
  }

  @Subscribe
  public void onRecordingsChanged(RecordingsChangedEvent event) {
    // rebind to refresh display
    bind(program);
  }

  @Subscribe
  public void onFavoriteChannelsChanged(EventBus.FavoriteChannelsChangedEvent event) {
    // rebind to refresh display
    bind(program);
  }

  @Subscribe
  public void onFavoriteProgramsChanged(EventBus.FavoriteProgramsChangedEvent event) {
    // rebind to refresh display
    bind(program);
  }

}
