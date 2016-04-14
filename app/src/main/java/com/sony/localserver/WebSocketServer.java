package com.sony.localserver;


import android.content.Context;
import android.util.Log;

import com.sony.localserver.NanoWSD.WebSocketFrame.CloseCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class WebSocketServer extends NanoWSD {
	public static LocalWebSocket ws;
	private static final String TAG="CVP-2";

	protected static CDSBrowser cdsContext;


    public WebSocketServer(String host, int port, CDSBrowser b){
    	super(host, port);
		cdsContext=b;

    }



	@Override
	protected WebSocket openWebSocket(IHTTPSession handshake) {
		Log.d(TAG, "OPEN WEBSOCKET");
			ws=new LocalWebSocket(handshake);
			return ws;

	}
	
	
	public static class LocalWebSocket extends WebSocket {
		int localPort;


		public LocalWebSocket(IHTTPSession handshakeRequest) {
			super(handshakeRequest);

			// TODO Auto-generated constructor stub
		}


			// TODO Auto-generated constructor stub

		@Override
		protected void onOpen() {
			// TODO Auto-generated method stub
			Log.d(TAG, "OPEN");

			
		}

		@Override
		protected void onClose(CloseCode code, String reason,
				boolean initiatedByRemote) {
			Log.d(TAG, "CLOSE");
			// TODO Auto-generated method stub
			
		}
		

		@Override
		protected void onMessage(WebSocketFrame message) {
			Log.d(TAG, "MESSAGE RECEIVED from Port: " + localPort + " that says: " + message.getTextPayload());
			if (message.getTextPayload().equals("Ping")){

				try {
					ws.send("Ping");
				} catch (IOException e) {
					e.printStackTrace();
				}
//			} else if (message.getTextPayload().equals("UpdateStations")){
//
//				new Thread(new Runnable() {
//					public void run() {
//
////						cdsContext.browseMediaServer(true, false);
//						updateWebSocketServerStation(ws);
//					}
//				}).start();
//
//
//
//			}else if (message.getTextPayload().equals("UpdateEPG")) {
//				new Thread(new Runnable() {
//					public void run() {
//
////						cdsContext.browseMediaServer(false, true);
//						updateWebSocketServerEPG(ws);
//					}
//				}).start();


//			}else if (message.getTextPayload().contains("setChannelList")) {
//				log("setting channel list...");
//				try {
//					JSONObject jo = new JSONObject(message.getTextPayload());
//					log ("length object: "+jo.length());
//					JSONArray channelListJSONArray =  jo.getJSONArray("setChannelList");
//					log ("length array: "+channelListJSONArray.length());
//					cdsContext.channelList.removeAllElements();
//					for (int i = 0; i < channelListJSONArray.length(); i++){
//						log("i: " + i + " value: " + channelListJSONArray.getString(i));
//						cdsContext.channelList.add(i, channelListJSONArray.getString(i));
//
//					}
//
//				}catch (Exception e){
//					log("error: "+e);
//					e.printStackTrace();
//				}
//
//			}else if (message.getTextPayload().contains("setTimeList")) {
//
//				try {
//					JSONArray timeListJSONArray = (new JSONObject(message.getTextPayload())).getJSONArray("setTimeList");
//					log("setting timelist...");
//
//
//					for (int i = 0; i < timeListJSONArray.length(); i++) {
//						cdsContext.timeList[i] = Long.parseLong(timeListJSONArray.getString(i));
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}else if (message.getTextPayload().contains("getChannelList")){
//				JSONArray ja=new JSONArray();
//				for (int i=0; i<cdsContext.channelList.size(); i++) {
//					ja.put(cdsContext.channelList.get(i));
//
//				}
//				JSONObject jo=new JSONObject();
//				try {
//					jo.put("channelList",ja);
//					ws.send(jo.toString());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//			}else if (message.getTextPayload().contains("getTimeList")){
//				JSONArray ja=new JSONArray();
//				ja.put (cdsContext.timeList[0]);
//				ja.put (cdsContext.timeList[1]);
//				JSONObject jo=new JSONObject();
//				try {
//					jo.put("TIMELIST",ja);
//					ws.send(jo.toString());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
			}else if (message.getTextPayload().contains("browseEPGData")){
				if (message.getTextPayload().startsWith("{") && message.getTextPayload().endsWith("}")){
					try {
						JSONObject jo=new JSONObject(message.getTextPayload());
						JSONObject params=jo.getJSONObject("browseEPGData");

						JSONArray ja=params.getJSONArray("TIMELIST");
						for (int i=0; i<ja.length(); i++) {
							cdsContext.timeList[i] =ja.getLong(i);
						}
						JSONArray channelListJSONArray =  params.getJSONArray("CHANNELLIST");
						log ("length array: "+channelListJSONArray.length());
						cdsContext.channelList.removeAllElements();
						for (int i = 0; i < channelListJSONArray.length(); i++){
							log("i: " + i + " value: " + channelListJSONArray.getString(i));
							cdsContext.channelList.add(i, channelListJSONArray.getString(i));

						}


					} catch (JSONException e) {
						e.printStackTrace();
					}


				}
				new Thread(new Runnable() {
					public void run() {

						cdsContext.browseMediaServer(false, true);
						updateWebSocketServerEPG(ws);
					}
				}).start();

			}else if (message.getTextPayload().equals("browseEPGStations")){
				new Thread(new Runnable() {
					public void run() {
//
//						cdsContext.browseMediaServer(false, true);
						cdsContext.browseMediaServer(true, false);
						updateWebSocketServerStation(ws);
//						updateWebSocketServerEPG(ws);
					}
				}).start();
			}

		}

		@Override
		protected void onPong(WebSocketFrame pong) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void onException(IOException exception) {
			// TODO Auto-generated method stub
			
		}


		public void updateWebSocketServerEPG(WebSocketServer.LocalWebSocket ws){
			log("updateWebSocketServerEPG");


			if (!cdsContext.jEPGresult.isEmpty()){
				do{
					try {
						log("sending..");
						ws.send(cdsContext.jEPGresult.elementAt(0).toString());
						cdsContext.jEPGresult.remove(0);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}while (!cdsContext.jEPGresult.isEmpty());
			}

		}

		public void updateWebSocketServerStation(WebSocketServer.LocalWebSocket ws){
			log("updateWebSocketServerStation");

			if (!cdsContext.jStationResult.isEmpty()){
				do{
					try {
						log("sending Station Data .." + cdsContext.jStationResult.size() );
						ws.send(cdsContext.jStationResult.elementAt(0).toString());
						cdsContext.jStationResult.remove(0);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}while (!cdsContext.jStationResult.isEmpty());
			}


		}



		private void log(String s){
			Log.d(TAG, s);
		}

	}


}
