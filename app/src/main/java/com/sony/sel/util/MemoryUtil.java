// Copyright (C) 2013 Sony Mobile Communications AB.
// All rights, including trade secret rights, reserved.

package com.sony.sel.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * @author christopherperry
 */
public class MemoryUtil {
    private static final long MEGABYTES_TO_BYTES = 1048576;

    public MemoryUtil() { }

    /**
     * How much heap can my app use before a hard error is triggered?
     * This method tells you how many total bytes of heap your app is allowed to use.
     * @return Memory size in bytes
     */
    public long getMaxMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.maxMemory();
    }

    /**
     * This method tells you approximately how many bytes of heap your app should use if it
     * wants to be properly respectful of the limits of the present device, and of the rights of
     * other apps to run without being repeatedly forced into the onStop() / onResume() cycle as
     * they are rudely flushed out of memory while your elephantine app takes a bath in the Android jacuzzi.
     * @param context The context
     * @return Size in bytes
     */
    public long getSuggestedMemoryBytes(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        return memoryClass * MEGABYTES_TO_BYTES;
    }

    public long getCurrentlyAllocatedHeapSizeBytes() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getAvailableHeapSizeBytes() {
        return Runtime.getRuntime().totalMemory();
    }

    public long getFreeHeapSizeBytes() {
        return Runtime.getRuntime().freeMemory();
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public boolean isDiskFull() {
        final long megabyteSize = 1024 * 1024;
        final long minMegabytes = 64;
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availBlocks = stat.getAvailableBlocks();
        long blockSize = stat.getBlockSize();
        long freeMemory = availBlocks * blockSize / megabyteSize;

        return freeMemory <= minMegabytes;
    }
}
