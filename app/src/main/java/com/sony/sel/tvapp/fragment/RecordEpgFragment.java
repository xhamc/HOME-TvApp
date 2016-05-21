package com.sony.sel.tvapp.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for confirming recording of an EPG item.
 */
public class RecordEpgFragment extends BaseFragment {

  public static final String TAG = RecordEpgFragment.class.getSimpleName();

  // fragment arguments
  private static final String VIDEO_PROGRAM = "VideoProgram";
  private static final String CHANNEL = "Channel";

  @Bind(R.id.programIcon)
  ImageView icon;
  @Bind(R.id.title)
  TextView title;
  @Bind(R.id.programTitle)
  TextView programTitle;
  @Bind(R.id.programChannel)
  TextView channelNumber;
  @Bind(R.id.programTime)
  TextView time;
  @Bind(R.id.programDescription)
  TextView description;
  @Bind(R.id.favoriteChannel)
  ImageView favoriteChannel;
  @Bind(R.id.recordProgram)
  ImageView recordProgram;
  @Bind(R.id.recordSeries)
  ImageView recordSeries;
  @Bind(R.id.favoriteProgram)
  View favoriteProgram;
  @Bind(R.id.recordProgramButton)
  View recordProgramButton;
  @Bind(R.id.cancelProgramRecordingButton)
  View cancelProgramRecordingButton;
  @Bind(R.id.recordSeriesButton)
  View recordSeriesButton;
  @Bind(R.id.cancelSeriesRecordingButton)
  View cancelSeriesRecordingButton;
  @Bind(R.id.cancelButton)
  View cancelButton;

  private VideoProgram program;
  private VideoBroadcast channel;

  private SettingsHelper settingsHelper;
  private DlnaInterface dlnaHelper;

  public static RecordEpgFragment newInstance(VideoProgram program, VideoBroadcast channel) {
    RecordEpgFragment fragment = new RecordEpgFragment();
    Bundle bundle = new Bundle();
    bundle.putString(VIDEO_PROGRAM, program.toString());
    bundle.putString(CHANNEL, channel.toString());
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    View contentView = inflater.inflate(R.layout.record_epg_fragment, null);
    ButterKnife.bind(this, contentView);

    settingsHelper = SettingsHelper.getHelper(getActivity());
    dlnaHelper = DlnaHelper.getHelper(getActivity());

    // set up program & channel
    final VideoProgram program = new Gson().fromJson(getArguments().getString(VIDEO_PROGRAM), VideoProgram.class);
    VideoBroadcast channel = new Gson().fromJson(getArguments().getString(CHANNEL), VideoBroadcast.class);
    bind(program, channel);

    recordProgramButton.requestFocus();

    recordProgramButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        settingsHelper.addRecording(program);
        getActivity().finish();
      }
    });

    cancelProgramRecordingButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        settingsHelper.removeRecording(program);
        getActivity().finish();
      }
    });

    recordSeriesButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        settingsHelper.addSeriesRecording(program);
        getActivity().finish();
      }
    });

    cancelSeriesRecordingButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        settingsHelper.removeSeriesRecording(program);
        getActivity().finish();
      }
    });

    cancelButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        getActivity().finish();
      }
    });

    return contentView;
  }

  public void bind(final VideoProgram program, final DlnaObjects.VideoBroadcast channel) {
    this.program = program;
    this.channel = channel;

    // icon
    if (program.getIcon() != null) {
      // we have a program icon, use it
      setProgramIcon(program.getIcon());
    } else if (channel.getIcon() != null) {
      // fall back to channel icon
      setChannelIcon(channel.getIcon());
    } else {
      // no icon available
      icon.setVisibility(View.GONE);
    }

    // title
    title.setText(program.getTitle());

    // program title
    if (program.getProgramTitle() != null && program.getProgramTitle().length() > 0) {
      programTitle.setText(program.getProgramTitle());
      programTitle.setVisibility(View.VISIBLE);
    } else {
      programTitle.setVisibility(View.GONE);
    }

    // number
    channelNumber.setText(channel.getChannelNumber() + " " + channel.getCallSign());

    // start/end time
    DateFormat format = new SimpleDateFormat("h:mm");
    String programTime = format.format(program.getScheduledStartTime()) + "-" + format.format(program.getScheduledEndTime());
    time.setText(programTime);
    time.setVisibility(View.VISIBLE);

    // long description
    description.setText(program.getLongDescription());

    // favorite program
    favoriteProgram.setVisibility(settingsHelper.isFavoriteProgram(program) ? View.VISIBLE : View.GONE);

    // favorite channel
    if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
      favoriteChannel.setVisibility(View.VISIBLE);
    } else {
      favoriteChannel.setVisibility(View.GONE);
    }

    // series or program recording
    if (settingsHelper.isSeriesRecorded(program)) {
      recordSeries.setVisibility(View.VISIBLE);
      recordProgram.setVisibility(View.GONE);
      recordProgramButton.setVisibility(View.GONE);
      cancelProgramRecordingButton.setVisibility(View.GONE);
      recordSeriesButton.setVisibility(View.GONE);
      cancelSeriesRecordingButton.setVisibility(View.VISIBLE);
    } else if (settingsHelper.isProgramRecorded(program)) {
      recordSeries.setVisibility(View.GONE);
      recordProgram.setVisibility(View.VISIBLE);
      recordProgramButton.setVisibility(View.GONE);
      cancelProgramRecordingButton.setVisibility(View.VISIBLE);
      recordSeriesButton.setVisibility(View.VISIBLE);
      cancelSeriesRecordingButton.setVisibility(View.GONE);
    } else {
      recordSeries.setVisibility(View.GONE);
      recordProgram.setVisibility(View.GONE);
      recordProgramButton.setVisibility(View.VISIBLE);
      cancelProgramRecordingButton.setVisibility(View.GONE);
      recordSeriesButton.setVisibility(View.VISIBLE);
      cancelSeriesRecordingButton.setVisibility(View.GONE);

    }
  }

  /**
   * Set up the icon as a program/show thumbnail.
   *
   * @param uri Icon uri.
   */
  private void setProgramIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    icon.setPadding(0, 0, 0, 0);
    Picasso.with(getActivity()).load(Uri.parse(uri)).into(icon);
  }

  /**
   * Set up the icon as a channel ID thumbnail.
   *
   * @param uri Icon uri.
   */
  private void setChannelIcon(String uri) {
    icon.setVisibility(View.VISIBLE);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int padding = getResources().getDimensionPixelSize(R.dimen.channelThumbPadding);
    icon.setPadding(padding, padding, padding, padding);
    Picasso.with(getActivity()).load(Uri.parse(uri)).into(icon);
  }
}
