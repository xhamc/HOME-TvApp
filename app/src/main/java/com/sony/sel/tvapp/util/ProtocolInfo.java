package com.sony.sel.tvapp.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpHead;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

/**
 * Class for determining DLNA protocol info from video URLs.
 */
public class ProtocolInfo {


  private static final String TAG = "CVP-2";
  private static final String GET_CONTENT_FEATURES = "getcontentFeatures.dlna.org";
  private static final String CONTENT_FEATURES = "contentfeatures.dlna.org";
  private static final String CONTENT_LENGTH = "content-length";
  private static final String GET_CONTENT_RANGE = "Content-Range";
  private static final String GET_CONTENT_RANGE_DTCP = "Content-Range.dtcp.com";
  private static final String PLAYSPEED_HEADER = "PlaySpeed.dlna.org";

  private static final String GET_RANGE = "Range";
  private static final String GET_TIMESEEK_RANGE = "TimeSeekRange.dlna.org";
  private static final String DLNA_ORG_OP = "dlna\\.org_op";
  private static final String DLNA_ORG_PN = "dlna\\.org_pn";
  private static final String DLNA_ORG_PS = "dlna\\.org_ps";
  private static final String DTCP1HOST = "DTCP1HOST";
  private static final String DTCP1PORT = "DTCP1PORT";
  private static final int SONY_PB_PLAYER = 8;
  private static final int SONY_PB_PLAYER_DTCP = 9;
  private static final int MTK_STREAM_PLAYER = 6; /* This is our good guess */
  private static final int RANGE_OP_INDEX = 1;
  private static final int TIMESEEK_OP_INDEX = 0;
  private static final boolean PLAYSPEED_MODE = true;

  private final String PLAYER_SET_KEY_PROTOCOLINFO = "protocol_info";
  private final String PLAYER_SET_KEY_PLAYMODE = "playmode";

  private final String PLAYER_SET_KEY_DURATION = "duration";
  private final String PLAYER_SET_KEY_CONTENTLENGTH = "contentlength";
  private final String PLAYSPEEDS = "PLAYSPEED";

  protected static final String COOKIE_HEADER = "Cookie";
  protected static final String USER_AGENT_HEADER = "User-Agent";

  private final boolean USE_URI_METHOD = true;

  protected boolean mGetContentFeaturesResult;
  protected boolean mGetOpFlagsResult;
  protected boolean mGetContentRangeResult;
  protected boolean mGetTimeSeekRangeResult;
  protected boolean mGetPSParamResult;
  protected String mAKEhost;
  protected String mAKEport;
  protected String mProtocolInfo;
  private String mUrl;
  private String mCookies;
  private String mUserAgent;
  private int mRequestedPosition;
  protected long mContentLength;
  protected double[] mTimeSeekRange = {-1, -1, -1};
  protected char mOP[] = {'0', '0'};
  protected List<String> mPsString;
  protected List<Double> mPsValue;
  protected String mPsStringTotal;
  protected long[] mContentRange = {-1, -1, -1};

  private int mPlayerType;

  protected String mURLextensions = "";

  public ProtocolInfo(String url, int position, Map<String, String> hdrs) {
    mGetContentFeaturesResult = false;
    mGetOpFlagsResult = false;
    mGetContentRangeResult = false;
    mGetTimeSeekRangeResult = false;
    mGetPSParamResult = false;
    mProtocolInfo = "";
    mContentLength = -1;
    mAKEhost = "";
    mUrl = url;
    if (hdrs != null && hdrs.containsKey(COOKIE_HEADER))
      mCookies = hdrs.get(COOKIE_HEADER);
    else
      mCookies = "";
    if (hdrs != null && hdrs.containsKey(USER_AGENT_HEADER))
      mUserAgent = hdrs.get(USER_AGENT_HEADER);
    else
      mUserAgent = "";

    getProtocolInfo();

    try {

      if (getPlayerType() == SONY_PB_PLAYER) {
        mUrl = mUrl.replaceFirst("(?i)http", "dlna://URI=http");
        if (USE_URI_METHOD) {
          mUrl = mUrl.concat(mURLextensions);
        }
        Log.d("CVP-2", "Using Sony PB Player by replacing http with dlna : " + mUrl);
      } else if (getPlayerType() == SONY_PB_PLAYER_DTCP) {

        String insertIntoUrl = "";
        if (!"".equals(mAKEhost) && !"".equals(mAKEport) && USE_URI_METHOD)
          insertIntoUrl = insertIntoUrl.concat("dtcpip://AKEHost=" + mAKEhost + ",AKEPort=" + mAKEport + ",URI=http");
        else
          insertIntoUrl = insertIntoUrl.concat("dtcpip://URI=http");
        mUrl = mUrl.replaceFirst("(?i)http", insertIntoUrl);
        if (USE_URI_METHOD) {
          mUrl = mUrl.concat(mURLextensions);
        }
        Log.v(TAG, "URL with DTCP-IP extensions: " + mUrl);
        Log.d("CVP-2", "Using Sony PB Player by replacing http with dtcpip : " + mUrl);
      } else {
        Log.d(TAG, "Using default player selected by android factory: URL:" + mUrl);
      }


    } catch (Exception e) {
      Log.e(TAG, e.toString());
      if (e.getMessage() != null) {
        Log.e(TAG, e.getMessage());
      }
    }

  }

  public String getUrl() {
    return mUrl;
  }


  public int getPlayerType() {

    int playerType = MTK_STREAM_PLAYER;
    Pattern pattern = Pattern.compile(".*" + DLNA_ORG_PN + "=([^;]+);.*", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(mProtocolInfo);
    if (matcher.find()) {
      Log.d(TAG, "DLNA profile name: " + matcher.group(1));
      if (!matcher.group(1).contains("DASH")) {

        if (!matcher.group(1).contains("DTCP"))
          playerType = SONY_PB_PLAYER;
        else
          playerType = SONY_PB_PLAYER_DTCP;
      }

    } else {
      Log.d(TAG, "Didn't find DLNA profile name");
    }
    Log.d(TAG, "getPlayerType() returns " + playerType + " (Sony: " + SONY_PB_PLAYER + ", MTK: " + MTK_STREAM_PLAYER + ")");
    return playerType;
  }

  public double getDuration() {

    return mTimeSeekRange[2];
  }

  public long getContentLength() {
    if (mContentRange[2] != -1) {
      return mContentRange[2];
    } else {
      return mContentLength;
    }
  }

  public String getProtocolInfoString() {
    return mProtocolInfo;
  }

  public boolean playSpeedSupported() {
    return mGetPSParamResult;
  }

  public boolean testPlaySpeedSupport(String sp) {
    return mPsString.contains(sp);
  }

  public boolean testPlaySpeedSupport(Double sp) {
    return mPsValue.contains(sp);
  }

  private Map<String, String> headerResult = new HashMap<>();

  public Map<String, String> getHeaders() {
    return headerResult;
  }


  public void getProtocolInfo() {
    Log.d(TAG, "getProtocolInfo, url=" + mUrl);
    Pattern pattern = Pattern.compile(".mpd", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(mUrl);
    if (matcher.find()) {
      mProtocolInfo = "DASH";
      Log.d(TAG, "found DASH Content through .mpd extension");
      return;
    }

    Header[] mHeaders;
    //HttpClient httpclient = new DefaultHttpClient();
    HttpClient httpclient = HttpClientBuilder.create().build();

    HttpHead httphead = new HttpHead(mUrl);
    //headerResult.put(PLAYER_SET_KEY_USEFLAG, "on");
    httphead.setHeader(COOKIE_HEADER, mCookies);
    httphead.setHeader(USER_AGENT_HEADER, mUserAgent);
    headerResult.put(COOKIE_HEADER, mCookies);
    headerResult.put(USER_AGENT_HEADER, mUserAgent);

    try {
      mAKEhost = getAKE(DTCP1HOST, mUrl);
      mAKEport = getAKE(DTCP1PORT, mUrl);
      if (mGetContentFeaturesResult = mGetContentFeatures(httphead, httpclient)) {

        if (mGetOpFlagsResult = getOPFlags(mProtocolInfo)) {
          if (mOP[RANGE_OP_INDEX] == '1') {
            httphead.setHeader(GET_RANGE, "bytes=0-");
          }
          if (mOP[TIMESEEK_OP_INDEX] == '1') {
            httphead.setHeader(GET_TIMESEEK_RANGE, "npt=0.0-");
          }
          Log.d(TAG, "before httpclient.execute(httphead).getAllHeaders();");
          mHeaders = httpclient.execute(httphead).getAllHeaders();
          Log.d(TAG, "after httpclient.execute(httphead).getAllHeaders();");
          Log.d(TAG, "mHeaders: " + mHeaders);
          mURLextensions = mURLextensions.concat(",SEEK_TYPE=" + mOP[0] + mOP[1]);
          if (mOP[TIMESEEK_OP_INDEX] == '1') {
            //mURLextensions=mURLextensions.concat(",SeekType=Time");
            if (mRequestedPosition > 0)
              mURLextensions = mURLextensions.concat(",CurrentPosition=" + Integer.toString(mRequestedPosition));
            else
              mURLextensions = mURLextensions.concat(",CurrentPosition=0");
            httphead.removeHeaders(GET_TIMESEEK_RANGE);
            mGetTimeSeekRangeResult = getTimeSeekRange(mHeaders, GET_TIMESEEK_RANGE);
            mGetContentRangeResult = getContentRange(mHeaders, GET_CONTENT_RANGE);
            if (!mGetContentRangeResult)
              mGetContentRangeResult = getContentRange(mHeaders, GET_CONTENT_RANGE_DTCP);
            headerResult.put(PLAYER_SET_KEY_PROTOCOLINFO, mProtocolInfo);
            if (mGetTimeSeekRangeResult)
              mURLextensions = mURLextensions.concat(",Duration=" + Double.toString(mTimeSeekRange[2] * 1000));
            headerResult.put(PLAYER_SET_KEY_DURATION, Double.toString(mTimeSeekRange[2] * 1000));
            if (mGetContentRangeResult)
              headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentRange[2]));

          }
          if (mOP[RANGE_OP_INDEX] == '1') {
            //mURLextensions=mURLextensions.concat(",SeekType=Byte");
            httphead.removeHeaders(GET_RANGE);
            if (!mGetContentRangeResult) {
              mGetContentRangeResult = getContentRange(mHeaders, GET_CONTENT_RANGE);
              if (!mGetContentRangeResult)
                mGetContentRangeResult = getContentRange(mHeaders, GET_CONTENT_RANGE_DTCP);
            }
            if (mGetContentRangeResult)
              headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentRange[2]));

          }
        }
        if (!mGetContentRangeResult) {
          Log.d(TAG, "We don't have ContentRange so lets try ContentLength");
          headerResult.put(PLAYER_SET_KEY_CONTENTLENGTH, Long.toString(mContentLength));
          //mURLextensions=mURLextensions.concat(",&size=" + Long.toString(mContentLength));
          mURLextensions = mURLextensions.concat(",SIZE=" + Long.toString(mContentLength));

        } else {
          //mURLextensions=mURLextensions.concat(",&size=" + Long.toString(mContentRange[2]));
          mURLextensions = mURLextensions.concat(",SIZE=" + Long.toString(mContentLength));

        }
        mGetPSParamResult = getPSParam(mProtocolInfo);
        if (mGetPSParamResult) {
          mURLextensions = mURLextensions.concat(",PLAYSPEED=" + mPsStringTotal);
          headerResult.put(PLAYSPEEDS, mPsStringTotal);
        }
          /*	mURLextensions=mURLextensions.concat(",PLAYSPEED=");
            Iterator<Double> i = mPsValue.iterator();
            while (i.hasNext())
              mURLextensions=mURLextensions.concat(Double.toString(i.next()));
              if (i.hasNext()){
                mURLextensions=mURLextensions.concat(",");
              }
          }*/

      }

    } catch (Exception e) {
      Log.e(TAG, "Error trying to initialize backend" + e);
    }
    headerResult.put(PLAYER_SET_KEY_PLAYMODE, "video");
  }


  private boolean mGetContentFeatures(HttpHead head, HttpClient client) {
    head.setHeader(GET_CONTENT_FEATURES, "1");
    mProtocolInfo = "";
    mContentLength = -1;
    Header[] mHeaders;
    boolean result = false;
    try {

      Log.d(TAG, "before client.execute(head).getAllHeaders(); in mGetContentFeatures()");
      mHeaders = client.execute(head).getAllHeaders();
      Log.d(TAG, "after client.execute(head).getAllHeaders(); in mGetContentFeatures()");
      Log.d(TAG, "mHeaders: " + mHeaders);
      head.removeHeaders(GET_CONTENT_FEATURES);
      for (Header h : mHeaders) {
        Log.d(TAG, "name: " + h.getName() + ", value: " + h.getValue());
        if (h.getName().toLowerCase().equals(CONTENT_FEATURES)) {
          mProtocolInfo = h.getValue();
          Log.d(TAG, "Protocol Info: " + mProtocolInfo);
          result = true;
        }
        if (h.getName().toLowerCase().equals(CONTENT_LENGTH)) {
          try {
            mContentLength = Long.parseLong(h.getValue());
            Log.d(TAG, "PContent length: " + mContentLength);
          } catch (Exception e) {
            Log.e(TAG, "Not a long integer in content length");
          }
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception caught in getContentFeatures" + e);
    }
    return result;
  }

  private boolean mGetContentFeatures(String cdsData) {

    mProtocolInfo = "";
    mContentLength = -1;
    Header[] mHeaders;
    boolean result = false;

    try {
      String[] cdsDataSplit = cdsData.split("&");
      if (cdsDataSplit.length == 2) {
        mProtocolInfo = cdsDataSplit[0];
        mTimeSeekRange[2] = Double.valueOf(cdsDataSplit[1]);
        result = true;
      }
    } catch (Exception e) {

      Log.e(TAG, "Error parsing cdsProtocolInfo: cdsData=" + cdsData + "  error: " + e);
    }


    return result;
  }

  private boolean getOPFlags(String protocolInfo) {
    try {
      Pattern pattern = Pattern.compile(".*" + DLNA_ORG_OP + "=([01])([01]).*", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(protocolInfo);
      if (matcher.find()) {
        mOP[0] = matcher.group(1).charAt(0);
        mOP[1] = matcher.group(2).charAt(0);
        Log.d(TAG, "getOP parsing step4: " + mOP[0] + " , " + mOP[1]);
        return true;
      }
    } catch (Exception e) {
      Log.e(TAG, "getOP string parsing exception: " + e);
    }
    return false;
  }

  private String getAKE(String tag, String url) {
    try {
      Pattern pattern = Pattern.compile(tag + "=([0-9.]+)", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
        Log.d(TAG, "getAKE pattern found in " + url + " that is " + matcher.group(1));
        return matcher.group(1);
      } else {
        Log.d(TAG, "getAKE no pattern found in " + url);
        return "";
      }
    } catch (Exception e) {
      Log.e(TAG, "getAKE string parsing exception: " + e);
    }
    return "";
  }

  private boolean getPSParam(String protocolInfo) {
    try {
      mPsStringTotal = "";
      mPsString = new ArrayList<>();

      mPsValue = new ArrayList<>();
      double spval;
      Pattern pattern = Pattern.compile(".*" + DLNA_ORG_PS + "=(.*);", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(protocolInfo);
      if (matcher.find()) {
        String s = "([^,]+),?";
        Pattern speedPattern = Pattern.compile(s);
        mPsStringTotal = matcher.group(1);
        Log.v(TAG, "getPSParam(): match string: " + mPsStringTotal);
        Matcher speedMatcher = speedPattern.matcher(mPsStringTotal);
        int n = 0;
        while (speedMatcher.find()) {
          n++;
          String sp = speedMatcher.group(1);
          Log.v(TAG, "getPSParam(): Found speeds: " + sp);
          mPsString.add(sp);

          String[] fractions = sp.split("/");
          if (fractions.length == 1) {
            spval = Double.parseDouble(sp);
            Log.v(TAG, "getPSParam(): Found speed double: " + spval);
            mPsValue.add(spval);

          } else if (fractions.length == 2 && Double.parseDouble(fractions[1]) > 0) {
            spval = Double.parseDouble(fractions[0]) / Double.parseDouble(fractions[1]);
            Log.v(TAG, "getPSParam(): Found speed double: " + spval);
            mPsValue.add(spval);
          } else {
            return false;
          }
        }
        if (mPsString.size() > 0) {
          Log.v(TAG, "Number of speeds found: " + mPsString.size());
          return true;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "getPSParam() string parsing exception: " + e);
    }
    Log.v(TAG, "getPSParam(): Did not find pattern");
    return false;

  }

  private boolean getTimeSeekRange(Header[] headers, String headName) {
    boolean result = false;
    try {
      Log.d(TAG, "getTimeSeekRange() starts");
      for (Header h : headers) {
        Log.d(TAG, "looking for [" + headName + "]");
        Log.d(TAG, "getTimeSeekRange: name=" + h.getName() + ", value=" + h.getValue());
        if (h.getName().toLowerCase().equals(headName.toLowerCase())) {
          Log.d(TAG, "value=" + h.getValue());
          Pattern pattern = Pattern.compile("(([0-9]+):)?(([0-9]+):)?([0-9]+(\\.[0-9]+)?)");
          Matcher matcher = pattern.matcher(h.getValue());
          for (int j = 0; j < 3 && matcher.find(); j++) {
            // for(int k = 1; k <= 6; k++) {
            //   Log.d(TAG, "group(" + k + ")=" + matcher.group(k));
            // }
            mTimeSeekRange[j] = 0;
            if (matcher.group(2) != null)
              mTimeSeekRange[j] += Double.parseDouble(matcher.group(2)) * 3600f;
            if (matcher.group(4) != null)
              mTimeSeekRange[j] += Double.parseDouble(matcher.group(4)) * 60f;
            mTimeSeekRange[j] += Double.parseDouble(matcher.group(5));
          }
          if (mTimeSeekRange[2] == -1.0)
            mTimeSeekRange[2] = mTimeSeekRange[1]; //Allow for x:y/* format, set end time to max seek
          Log.d(TAG, "mTimeSeekRange[3]: " + mTimeSeekRange[0] + ", " + mTimeSeekRange[1] + ", " + mTimeSeekRange[2]);
          // return true;
          result = true;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception caught in getTimeSeekRange" + e);
    }
    return result;
  }

  private boolean getContentRange(Header[] headers, String headName) {
    try {
      for (Header h : headers) {
        Log.d(TAG, "getContentRange: name=" + h.getName() + ", value=" + h.getValue());
        if (h.getName().toLowerCase().equals(headName.toLowerCase())) {
          Pattern pattern = Pattern.compile(".*bytes[ =]*([0-9]+)-([0-9]+)/([0-9*]+).*");
          Matcher matcher = pattern.matcher(h.getValue());
          if (matcher.find()) {
            for (int i = 0; i < 3; i++) {
              Log.d(TAG, "ContentRange matcher output" + matcher.group(i + 1));
              if (!matcher.group(i + 1).startsWith("*"))
                mContentRange[i] = Long.parseLong(matcher.group(i + 1));
              else if (i == 2)
                Log.d(TAG, "ContentRange substituting * for max content seekable range");
              mContentRange[2] = mContentRange[1] + 1; //Allow for x:y/* format, set length to max seek range +1
            }

            Log.d(TAG, "Content Range start, end, range: "
                + mContentRange[0] + " ," + mContentRange[1] + " ," + mContentRange[2]);
            return true;
          }
        }
      }

    } catch (Exception e) {
      Log.e(TAG, "Exception caught in getContentFeatures" + e);
    }
    return false;
  }
}
