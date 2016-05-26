package com.sony.sel.tvinput;

import android.app.Activity;
import android.content.Context;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.sony.sel.tvapp.R;
import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.DlnaInterface;
import com.sony.sel.tvapp.util.DlnaObjects;
import com.sony.sel.tvapp.util.SettingsHelper;
import com.sony.sel.tvinput.syncadapter.SyncUtils;
import java.util.List;


public class SetupActivity extends Activity {


    public static final String TAG = "SetupActivity";
    private DlnaInterface dlnaHelper;
    private String udn;
    private SettingsHelper settingsHelper;
    private Context context = null;
    private String mInputId;
    private InputChannelsUtil inputChannelsUtil = null;// = new InputChannelsUtil(mInputId, context);



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = this;
        dlnaHelper = DlnaHelper.getHelper(context);
        settingsHelper = SettingsHelper.getHelper(context);
        udn = settingsHelper.getEpgServer();
        if(udn == null)
        {
            Log.e(TAG, "failed to get epg server udn");
        }
        mInputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        inputChannelsUtil = new InputChannelsUtil(mInputId, context);
        inputChannelsUtil.saveInputId(mInputId);
        setContentView(R.layout.activity_setup);
        onScanButtonClicked();
        /*Button scanButton = (Button)findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Log.d(TAG, "scanClicked");
                onScanButtonClicked();
            }
        });*/
    }

    private void onScanButtonClicked()
    {
        log("onScanButtonClicked");
        TextView messageText = (TextView)findViewById(R.id.textViewSetup);
        messageText.setText("Scanning.....");
        Button scanButton = (Button)findViewById(R.id.button_scan);
        scanButton.setEnabled(false);
        List<DlnaObjects.VideoBroadcast> channels = dlnaHelper.getChannels(udn, null, true);
        if(channels != null)
        {
            Log.d(TAG,channels.toString());
            addChannelsToDatabase(channels);
        }
        else
        {
            Log.d(TAG,"no channels found");
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    private void addChannelsToDatabase(List<DlnaObjects.VideoBroadcast> channels)
    {
        int num_channels = channels.size();
        Log.d(TAG, "Number of channels found is: " + num_channels);
        TextView messageText = (TextView)findViewById(R.id.textViewSetup);
        messageText.setText("Finished scanning.  Found " + Integer.toString(num_channels) + " channels");
        inputChannelsUtil.registerChannels(channels);
        SyncUtils.setUpPeriodicSync(this, mInputId);
        SyncUtils.requestSync(mInputId,false);
    }

    public static void log(String s) {
        Log.d(TAG, s);
    }

}
