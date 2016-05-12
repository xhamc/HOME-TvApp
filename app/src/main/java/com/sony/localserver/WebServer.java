package com.sony.localserver;

import android.content.Context;
import android.util.Log;

import com.sony.sel.tvapp.util.DlnaHelper;
import com.sony.sel.tvapp.util.SettingsHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Class to serve files from the "locahost" directory in the application assets.
 */
public class WebServer extends WebSocketServer {

  private static final String LOCALHOST = "localhost";
  private static final String TAG = "CVP-2";

  private HashMap<String, String> assetsHash = new HashMap<>();
  private Context context;

  public WebServer(String host, int port, Context context) {
    super(host, port, DlnaHelper.getHelper(context), DlnaHelper.getCache(context), SettingsHelper.getHelper(context));
    this.context = context;
    iterateFiles(LOCALHOST);
  }

  public void iterateFiles(String startingDirectory) {
    Vector<String> directories = new Vector<>();
    Vector<String> files = new Vector<>();
    directories.add(startingDirectory);
    int directoryIndex = 0;
    try {
      boolean directoryfound;
      int directoryCount = 0;
      do {
        directoryfound = false;
        String[] assetslist = context.getAssets().list(directories.elementAt(directoryIndex));

        for (String file : assetslist) {
          if (!file.contains(".")) {
            String fullPath = directories.elementAt(directoryIndex) + "/" + file;
            directories.add(fullPath);
            Log.d(TAG, "Added directory: " + fullPath);
            directoryfound = true;
            directoryCount++;
          } else {
            String fullPath = directories.elementAt(directoryIndex) + "/" + file;
            Log.d(TAG, "Added file: " + fullPath);
            files.add(fullPath);
          }
        }

        directoryIndex++;

      } while (directoryfound || (directoryIndex <= directoryCount));


      for (String file : files) {

        Log.d(TAG, "File: " + file);
        String extension = file.substring(file.lastIndexOf(".") + 1).toLowerCase();
        String mimeType = "";
        switch (extension) {
          case "png":
            mimeType = "png";
            break;
          case "html":
            mimeType = "HTML";
            break;
          default:
            mimeType = "";
        }
        assetsHash.put(file, mimeType);
      }


    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public Response serve(IHTTPSession session) {
    Map<String, String> header = session.getHeaders();
    Map<String, String> parms = session.getParms();
    String uri = session.getUri();
    Response r = super.serve(session);
    if (r != null) {
      Log.d(TAG, "   WEBSOCKET REQUEST");
      return r;
    }

    uri = LOCALHOST + uri.trim().replace(File.separatorChar, '/');

    if (assetsHash.containsKey(uri)) {

      InputStream is;
      try {
        is = context.getAssets().open(uri);
        if (is != null) {
          Log.d(TAG, "Serving out: " + uri + " with mime type: " + assetsHash.get(uri));
          r = new NanoHTTPD.Response(Response.Status.OK, assetsHash.get(uri), is);
          return r;
        }

      } catch (Exception e) {
        Log.e(TAG, "Error with file: " + e.getMessage() + "\n");
      }

    }
    return null;
  }


}
