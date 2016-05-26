package com.sony.sel.tvinput;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvinput.syncadapter.SyncUtils;

import java.util.List;

/**
 * Created by breeze on 5/25/16.
 */
public class TvInputUtil
{
    public static final String TAG = "TvInputUtil";
    private String mInputId;
    private Context mContext;
    private InputChannelsUtil inputChannelsUtil = null;

    public TvInputUtil(String tvInputId, Context context)
    {
        mInputId = tvInputId;
        mContext = context;
        inputChannelsUtil = new InputChannelsUtil(mInputId, context);
        inputChannelsUtil.saveInputId(mInputId);

    }

    public void addChannelsToDatabase(List<DlnaObjects.VideoBroadcast> channels)
    {
        int num_channels = channels.size();
        Log.d(TAG, "Number of channels found is: " + num_channels);
        inputChannelsUtil.registerChannels(channels);
        SyncUtils.setUpPeriodicSync(mContext, mInputId);
        SyncUtils.requestSync(mInputId,false);
    }
}
