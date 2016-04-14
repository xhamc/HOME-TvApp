package com.sony.localserver;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.sony.dtv.discovery.util.CursorUtil;
import com.sony.huey.dlna.DlnaCdsStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gclift on 3/23/16.
 */
public class CDSBrowser {

    public static WebSocketServer s1;
    private static final String TAG="CVP-2";
    Context serviceContext;
    public static String udnShared;

    public static Vector<String> channelList=new Vector<String>();
    public static Long[] timeList=new Long[2];

    Vector<JSONObject> jStationResult=new Vector<>();
    Vector<JSONObject> jEPGresult=new Vector<>();

    public CDSBrowser(Context c){
        serviceContext=c;
    }


    public void browseMediaServer(boolean browseStations, boolean browseEPG) {
//        jEPGresult=new Vector<>();
//        jStationResult=new Vector<>();

        String objectId;

        log( "browseMediaServer");
        Uri googleRootURI;
        Cursor googleRootChildCursor = null;
        Cursor channels =null;

        //browse the root folder
        try {
            googleRootURI = DlnaCdsStore.getObjectUri(udnShared, "0");//.getObjUriWithMaxCount(mUdn, RVU_ROOT, 5); //mUdn is the Direct TV udn
            googleRootChildCursor = serviceContext.getContentResolver().query(googleRootURI, null, null, null, null);
            log( "rootUri: " + googleRootURI.toString());

            if (googleRootChildCursor.moveToFirst()) {
                do {
                    String title = CursorUtil.getString(googleRootChildCursor,
                            DlnaCdsStore.TITLE, "");
                    String id = CursorUtil.getString(googleRootChildCursor, DlnaCdsStore.ID, "");

                    log( "playlist title: " + title);
                    if (browseEPG && title.equalsIgnoreCase("EPG") /*|| title.equalsIgnoreCase("VOD")*/) {
                        objectId = CursorUtil.getString(googleRootChildCursor, DlnaCdsStore.ID, "");
                        log( "ID: " + objectId);
                        String uid = CursorUtil.getString(googleRootChildCursor, DlnaCdsStore.UID, "");
                        log("uid:" + uid);
                        parseEPG(udnShared, objectId);
                    }else if (browseStations && title.equalsIgnoreCase("Channels") ){
                        objectId = CursorUtil.getString(googleRootChildCursor, DlnaCdsStore.ID, "");
                        log( "ID: " + objectId);
                        String uid = CursorUtil.getString(googleRootChildCursor, DlnaCdsStore.UID, "");
                        log("uid:" + uid);
                        parseStations(udnShared, objectId);

                    }

                } while (googleRootChildCursor.moveToNext());

            }

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            //log( "closing cursor");

            googleRootChildCursor.close();
        }
    }


    private synchronized void parseStations(String udn, String id){


        final ReentrantLock lock = new ReentrantLock();
        lock.lock();

        final JSONObject jStation=new JSONObject();
        final String udnLocal=udn;
        final String objectId=id;

        log("udn: " + udn + "  objectID: " + objectId);
//        new Runnable() {
//            public void run() {
        try {
            log("getting Stations...");
            jStation.put("STATIONS", parseStationArray(udnLocal, objectId));
            storeStationResult(jStation);
        } catch (JSONException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
//            }
//        }.run();
    }

    private JSONObject parseStationObject (Cursor stationContent){

        JSONObject jEPGStation = new JSONObject();
        try {
            String jsonName;
            for (int k = 0; k < stationContent.getColumnCount(); k++) {
                log("KEY: "+stationContent.getColumnName(k)+" VALUE: "+ stationContent.getString(k));
                String jsonValue = stationContent.getString(k);
                switch (stationContent.getColumnName(k)) {

                    case "upnp:channelNr":
                        jsonName = "channelNumber";
                        break;
                    case "upnp:channelName":
                        jsonName = "channelName";
                        break;
                    case "upnp:callSign":
                        jsonName = "callSign";
                        break;
                    case "upnp:channelID":
                        jsonName = "channelID";
                        break;
                    case "res@protocolInfo":
                        jsonName = "protocolInfo";
                        break;
                    case "upnp:icon":
                        jsonName = "channelIcon";
                        break;
                    default:
                        jsonName = "";
                }
                if (!jsonName.isEmpty()) {
                    try {
                        jEPGStation.put(jsonName, jsonValue);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return jEPGStation;


        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return null;
    }

    private JSONObject parseStationArray(String udn, String objectId){
        JSONObject jEPGStations=new JSONObject();
        log("browsing playlist folder");
        String[] objectSelection={"upnp:class","@id", "_id","upnp:icon","upnp:channelNr","upnp:channelName","upnp:callSign","upnp:channelID" };
        Cursor stationContent = serviceContext.getContentResolver().query(DlnaCdsStore.getObjectUriWithMaxCount(udn, objectId, 100), objectSelection, null, null, null);
        if (stationContent.moveToFirst()) {
            try {
                do {
                    JSONObject station=parseStationObject(stationContent);
                    String stationKey="";
                    try {
                        stationKey = station.get("channelID").toString();
                    }catch (NullPointerException e){
                        stationKey="";
                    }

                    if (stationKey!=""){

                        jEPGStations.put(stationKey, station);
                    }

                }
                while (stationContent.moveToNext());
//                stationContent.close();
                return jEPGStations;
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                stationContent.close();
            }
        }

        return null;
    }



    private synchronized void parseEPG(String udn, String id){

        final ReentrantLock lock = new ReentrantLock();
        lock.lock();

        final JSONObject jEPG=new JSONObject();
        final String udnLocal=udn;
        final String objectId=id;
//        final String[] channelList=cList;
//        final String[] dateList=dList;
//        final String[] timeList=tList;
        log("udn: " + udn + "  objectID: " + objectId);
//        new Runnable() {
//            public void run() {
                try {
                    log("gettingEPG...");
                    jEPG.put("EPG", parseChannelArray(udnLocal, objectId));
                    log("storeEpgResult...");
                    storeEpgResult(jEPG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }


    }


    private JSONObject parseChannelArray(String udn, String objectId){
        JSONObject jEPGChannelArray=new JSONObject();
        log("browsing playlist folder");
        String[] objectSelection={"upnp:class","@id", "_id","dc:title" };
        Cursor epgContent = serviceContext.getContentResolver().query(DlnaCdsStore.getObjectUriWithMaxCount(udn, objectId, 100), objectSelection, null, null, null);
        try {
            if (epgContent.moveToFirst()) {

                do {
                    String parentID = epgContent.getString(epgContent.getColumnIndex("@parentID"));
                    String iD = epgContent.getString(epgContent.getColumnIndex("@id"));
                    String upnpclass = epgContent.getString(epgContent.getColumnIndex("upnp:class"));
                    String channelRef = iD.substring(iD.indexOf(parentID) + parentID.length() + 1);
                    log("Channel reference: " + channelRef);
                    if (channelList!=null ){
                        for (String channel:channelList){
                            if (channel.equals(channelRef)){
                                if (upnpclass.contains("object.container")) {
                                    jEPGChannelArray.put(channelRef, parseDayArray(udn, iD));
                                }
                            }
                        }
                        continue;
                    }
                    if (upnpclass.contains("object.container")) {
                        jEPGChannelArray.put(channelRef, parseDayArray(udn, iD));
                    }
                }

                while (epgContent.moveToNext());
                epgContent.close();
                return jEPGChannelArray;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            epgContent.close();
        }

        return null;

    }

    private JSONObject parseDayArray( String udn, String objectId){

        JSONObject jEPGDayArray=new JSONObject();
        String[] objectSelection={"upnp:class","@id", "_id","dc:title" };
        Cursor epgContent = serviceContext.getContentResolver().query(DlnaCdsStore.getObjectUriWithMaxCount(udn, objectId, 100), objectSelection, null, null, null);
        try {
            if (epgContent.moveToFirst()) {
                do {
                    String iD = epgContent.getString(epgContent.getColumnIndex("@id"));
                    String parentID2 = epgContent.getString(epgContent.getColumnIndex("@parentID"));
                    String upnpclass = epgContent.getString(epgContent.getColumnIndex("upnp:class"));
                    String dateRef = iD.substring(iD.indexOf(parentID2) + parentID2.length() + 1);

                        if (upnpclass.contains("object.container")) {
                            log("Date:  " + dateRef);
                            TimeArrayReturn jo=parseTimeArray(udn, iD);
                            if (jo.value.length()!=0) {
                                jEPGDayArray.put(dateRef, jo.value);
                            }

                            if (jo.end) break;
                        }


                }while (epgContent.moveToNext()) ;
//                epgContent.close();
                return jEPGDayArray;

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally{

            epgContent.close();
        }
        return null;
    }

    private class TimeArrayReturn{

        public TimeArrayReturn(JSONObject v, boolean noMoreDatesToCheck){
            value=v;
            end=noMoreDatesToCheck;
        }

        public JSONObject value;
        public boolean end;

    }


    private TimeArrayReturn parseTimeArray(String udn, String objectId){

//        SimpleDateFormat df=new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");
        long startTime=0;
        long endTime=Integer.MAX_VALUE;
        log("starttime: "+ timeList[0]+"  endtime:  "+timeList[1]);
        if (timeList!=null ) {
            if (timeList.length==2){
                startTime=timeList[0];
                endTime=timeList[1];
            }
        }
        JSONObject jEPGTimeArray=new JSONObject();
        String[] objectSelection={"upnp:class","@id", "_id","dc:title","upnp:scheduledStartTime","upnp:scheduledEndTime","upnp:scheduledDurationTime",
                "upnp:programTitle", "upnp:episodeType", "upnp:longDescription", "upnp:genre", "upnp:rating","upnp:channelNr", "upnp:icon"};
        Cursor epgContent = serviceContext.getContentResolver().query(DlnaCdsStore.getObjectUri(udn, objectId), objectSelection, null, null, null);

        try {
            if (epgContent.moveToFirst()) {
                do {
                    String timeRef = epgContent.getString(epgContent.getColumnIndex("upnp:scheduledStartTime"));
                    String timeEnd = epgContent.getString(epgContent.getColumnIndex("upnp:scheduledEndTime"));
                    Date d=df.parse(timeRef);
                    long timems=d.getTime();
                    d=df.parse(timeEnd);
                    long timeEndms=d.getTime();
//
//                    log("timeRefms: " + timems + "  upnp:scheduledStartTime: "+ timeRef);
//                    log("timeEndms: "+timeEndms+ "  upnp:scheduledEndTime"+ timeEnd);
                    if(timeEndms>=startTime && timems<=endTime){
                        JSONObject jo=parseTimeObject(String.valueOf(timems), epgContent);
                        if (jo.length()==0) continue;
                        log("Time: " + timems);
                        jEPGTimeArray.put(String.valueOf(timems), jo);
                    }
                    if (timems>endTime) {
                        return new TimeArrayReturn(jEPGTimeArray,true);
                    }
                } while (epgContent.moveToNext());
//                epgContent.close();
                return new TimeArrayReturn(jEPGTimeArray,false);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            epgContent.close();
        }
        return null;
    }



    private JSONObject parseTimeObject(String timeMs, Cursor epgContent){

        JSONObject jEPGMetadata = new JSONObject();
        try {
            String jsonName;


            //TODO: filter by time list
            SimpleDateFormat df;
            Date d;

            for (int k = 0; k < epgContent.getColumnCount(); k++) {
                log("KEY: "+epgContent.getColumnName(k)+" VALUE: "+ epgContent.getString(k));
                String jsonValue = epgContent.getString(k);
                switch (epgContent.getColumnName(k)) {
                    case "upnp:scheduledStartTime":
                        jsonName = "start";
                        jsonValue=timeMs;
                        break;

                    case "upnp:scheduledDurationTime":
                        jsonName = "length";
                        df=new SimpleDateFormat("'P'HH:mm:ss");
                        Calendar cal=Calendar.getInstance();
                        d=df.parse(jsonValue);
                        cal.setTime(d);

                        long duration=((cal.get(Calendar.HOUR)*60+cal.get(Calendar.MINUTE))*60+cal.get(Calendar.SECOND))*1000;
                        log("duration: getTime(): "+duration);
                        jsonValue=String.valueOf(duration);
                        break;
                    case "upnp:genre":
                        jsonName = "genre";
                        break;
                    case "dc:title":
                        jsonName = "title";
                        break;
                    case "upnp:programTitle":
                        jsonName = "programTitle";
                        break;
                    case "res@protocolInfo":
                        jsonName = "protocolInfo";
                        break;
                    case "upnp:rating":
                        jsonName = "rating";
                        break;
                    case "upnp:longDescription":
                        jsonName = "description";
                        break;
                    case "upnp:icon":
                        jsonName = "programIcon";
                        break;
                    default:
                        jsonName = "";
                }
                if (!jsonName.isEmpty()) {
                    try {
                        jEPGMetadata.put(jsonName, jsonValue);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return jEPGMetadata;



        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    private void storeEpgResult(JSONObject jEPG){
        log("storeEpgResult");
        jEPGresult.add(jEPG);

    }
    private void storeStationResult(JSONObject jStation){
        log("storeStationResult");
        jStationResult.add(jStation);

    }

    private void log(String s){
        Log.d(TAG, s);
    }
}
