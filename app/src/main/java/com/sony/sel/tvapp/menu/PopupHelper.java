package com.sony.sel.tvapp.menu;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects.VideoBroadcast;
import com.sony.sel.tvapp.util.DlnaObjects.VideoItem;
import com.sony.sel.tvapp.util.DlnaObjects.VideoProgram;
import com.sony.sel.tvapp.util.SettingsHelper;

/**
 * Helper for showing popup menus for Channel, EPG and VOD items.
 */
public class PopupHelper {


  public static final String TAG = PopupHelper.class.getSimpleName();

  private static PopupHelper INSTANCE;

  private final Context context;
  private final SettingsHelper settingsHelper;

  /**
   * Get the helper instance.
   */
  public static PopupHelper getHelper(@NonNull Context context) {
    if (INSTANCE == null) {
      INSTANCE = new PopupHelper(context);
    }
    return INSTANCE;
  }

  public PopupHelper(Context context) {
    this.context = context;
    this.settingsHelper = SettingsHelper.getHelper(context);
  }

  /**
   * Show a channel popup.
   *
   * @param channel Channel the menu is for.
   * @param anchor  View to anchor the popup menu to.
   */
  public void showPopup(@NonNull VideoBroadcast channel, @NonNull View anchor) {
    PopupMenu menu = new PopupMenu(context, anchor);
    menu.inflate(R.menu.program_popup_menu);

    // set channel menu items
    if (settingsHelper.getFavoriteChannels().contains(channel.getChannelId())) {
      menu.getMenu().findItem(R.id.removeFromFavoriteChannels).setVisible(true);
    } else {
      menu.getMenu().findItem(R.id.addToFavoriteChannels).setVisible(true);
    }

    menu.setOnMenuItemClickListener(new ChannelClickListener(channel));
    menu.show();
  }

  /**
   * Show a program (EPG) popup.
   *
   * @param program EPG program the menu is for.
   * @param anchor  View to anchor the popup menu to.
   */
  public void showPopup(@NonNull VideoProgram program, @NonNull View anchor) {
    PopupMenu menu = new PopupMenu(context, anchor);
    menu.inflate(R.menu.program_popup_menu);

    // set channel menu items
    if (settingsHelper.getFavoriteChannels().contains(program.getChannelId())) {
      menu.getMenu().findItem(R.id.removeFromFavoriteChannels).setVisible(true);
    } else {
      menu.getMenu().findItem(R.id.addToFavoriteChannels).setVisible(true);
    }

    // set recording menu items
    if (settingsHelper.isSeriesRecorded(program)) {
      menu.getMenu().findItem(R.id.cancelSeriesRecording).setVisible(true);
    } else if (settingsHelper.isProgramRecorded(program)) {
      menu.getMenu().findItem(R.id.cancelProgramRecording).setVisible(true);
    } else {
      menu.getMenu().findItem(R.id.recordProgram).setVisible(true);
      menu.getMenu().findItem(R.id.recordSeries).setVisible(true);
    }

    // set favorite menu items
    String seriesId = program.getSeriesId();
    if (seriesId != null && seriesId.length() > 0) {
      if (settingsHelper.isFavoriteProgram(program)) {
        menu.getMenu().findItem(R.id.removeFromFavoritePrograms).setVisible(true);
      } else {
        menu.getMenu().findItem(R.id.addToFavoritePrograms).setVisible(true);
      }
    }

    menu.setOnMenuItemClickListener(new ProgramClickListener(program));
    menu.show();
  }

  /**
   * Show a VOD item popup.
   *
   * @param vodItem VOD item the menu is for.
   * @param anchor  View to anchor the popup menu to.
   */
  public void showPopup(@NonNull VideoItem vodItem, @NonNull View anchor) {
    // nothing yet for VOD
  }

  /**
   * OnMenuItemClickListener for program (EPG) popups.
   */
  private class ProgramClickListener implements OnMenuItemClickListener {

    private final VideoProgram program;

    public ProgramClickListener(VideoProgram program) {
      this.program = program;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
      switch (item.getItemId()) {
        case R.id.recordProgram:
          settingsHelper.addRecording(program);
          return true;
        case R.id.recordSeries:
          settingsHelper.addSeriesRecording(program);
          return true;
        case R.id.cancelProgramRecording:
          settingsHelper.removeRecording(program);
          return true;
        case R.id.cancelSeriesRecording:
          settingsHelper.removeSeriesRecording(program);
          return true;
        case R.id.addToFavoritePrograms:
          settingsHelper.addFavoriteProgram(program);
          return true;
        case R.id.removeFromFavoritePrograms:
          settingsHelper.removeFavoriteProgram(program);
          return true;
        case R.id.addToFavoriteChannels:
          settingsHelper.addFavoriteChannel(program.getChannelId());
          return true;
        case R.id.removeFromFavoriteChannels:
          settingsHelper.removeFavoriteChannel(program.getChannelId());
          return true;
      }
      return false;
    }
  }

  /**
   * OnMenuItemClickListener for channel popups.
   */
  private class ChannelClickListener implements OnMenuItemClickListener {

    private final VideoBroadcast channel;

    public ChannelClickListener(VideoBroadcast channel) {
      this.channel = channel;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
      switch (item.getItemId()) {
        case R.id.addToFavoriteChannels:
          settingsHelper.addFavoriteChannel(channel.getChannelId());
          return true;
        case R.id.removeFromFavoriteChannels:
          settingsHelper.removeFavoriteChannel(channel.getChannelId());
          return true;
      }
      return false;
    }
  }

}
