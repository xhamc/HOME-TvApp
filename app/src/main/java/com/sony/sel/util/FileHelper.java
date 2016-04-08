package com.sony.sel.util;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Helper for files.
 */
public class FileHelper {

  public static final String LOG_TAG = FileHelper.class.getSimpleName();

  /**
   * Write a string to a file in external storage.
   *
   * @param text      Text to write.
   * @param directory Location of file.
   * @param fileName  Name of file to write.
   */
  public static void writeToFile(String text, File directory, String fileName) {
    try {
      String file = directory.getAbsolutePath() + "/" + fileName;
      if (!directory.exists() && !directory.mkdirs()) {
        Log.e(LOG_TAG, "Could not create directory " + directory.getAbsolutePath() + ".");
        return;
      }
      PrintWriter writer = new PrintWriter(file, "UTF-8");
      writer.append(text);
      writer.close();
    } catch (FileNotFoundException e) {
      Log.e(LOG_TAG, "Error writing to file: " + e);
    } catch (UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Error writing to file: " + e);
    }
  }
}
