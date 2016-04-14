package com.sony.localserver;

import android.app.IntentService;
import android.content.Intent;

import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;



public class ServerService extends IntentService {

	private static WebSocketServer s1;
	private static final String TAG="CVP-2";
	private CDSBrowser cdsBrowser;
	private static String udn;
	private static String location;

	@Override
	public void onHandleIntent(Intent arg0) {


		log("Intent processing..\n");

		if (arg0.hasExtra("start")) {

				start();
		}
		else if (arg0.hasExtra("stop")) {

				stop();
		}
		if (arg0.hasExtra("udn") && arg0.hasExtra("location")){
			udn=arg0.getStringExtra("udn");
			location=arg0.getStringExtra("location");
			refreshServiceList(udn, location);
		}

		if (arg0.hasExtra("serve") && udn!=null && location!=null){
			log("serving..");

			refreshServiceList(udn, location);
		}



	}


	public ServerService(){
		super("ServerService");
		int lengthAssets=0;
		cdsBrowser=new CDSBrowser(this);
	}

	private void start(){
		log("ServerService::start");
		int port1 = 9000;
		String host1 = "127.0.0.1";
		if (s1==null) {

			s1 = new WebServer(host1, port1, getApplicationContext(), cdsBrowser);
			cdsBrowser.s1 = s1;
			ServerRunner.executeInstance(s1);
		}
	}

	private void stop(){

		log("ServerService::stop");
		s1.stop();

	}


	private void refreshServiceList(final String udn, final String location) {
		log("refreshServiceList(udn=" + udn + ", location=" + location + ")");
		final Uri locationUri = Uri.parse(location);
		final String u=udn;

		String[] dateList=new String[2];
		Long[] timeList=new Long[2];
		long epgStartTime=System.currentTimeMillis()-(3600*1000);
//		Calendar cal=Calendar.getInstance();
//		cal.setTimeInMillis(epgStartTime);
//		SimpleDateFormat df=new SimpleDateFormat("MM-dd");
//		String todaysDate=df.format(cal.getTime());
//		dateList[0]=(todaysDate);
//		dateList[1]=(todaysDate);
//		df=new SimpleDateFormat("HH:00:00");
//		for (int i=0; i<2; i++){
//			String todaysTime=df.format(cal.getTime());
//			timeList[i]=todaysTime;
//			log(timeList[i]);
//			epgStartTime+=1000*3600*5;
//			cal.setTimeInMillis(epgStartTime);
//		}

		timeList[0]=epgStartTime;
		epgStartTime+=1000*3600*10;
		timeList[1]=epgStartTime;

		log("startTime: "+ timeList[0]+ " endtime: "+timeList[1]);

		String[] channelList={};
		cdsBrowser.udnShared=u;

		//cdsBrowser.dateList=dateList;
		//cdsBrowser.timeList=timeList;
		//cdsBrowser.browseMediaServer(true, false);
//		cdsBrowser.browseMediaServer(false, true);

	}

	private void log(String s){
		Log.d(TAG, s);
	}


}
