package com.sony.sel.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

/**
 * Helper for displaying icon badges on Sony and Samsung devices.
 * <p/>
 * Note: for this to work on Sony devices, the following line MUST appear in AndroidManifest.xml
 * <p/>
 * <uses-permission android:name="com.sonyericsson.home.permission.BROADCAST_BADGE"/>
 */
public class BadgeHelper {

  private static BadgeHelper INSTANCE;

  public static BadgeHelper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new BadgeHelper();
    }
    return INSTANCE;
  }

  private BadgeHelper() {
  }

  public void setBadge(Context context, int count) {
    if (count > 0) {
      setBadgeSamsung(context, count);
      setBadgeSony(context, count);
    } else {
      clearBadge(context);
    }
  }

  public void clearBadge(Context context) {
    setBadgeSamsung(context, 0);
    clearBadgeSony(context);
  }


  private void setBadgeSamsung(Context context, int count) {
    String launcherClassName = getLauncherClassName(context);
    if (launcherClassName == null) {
      return;
    }
    Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
    intent.putExtra("badge_count", count);
    intent.putExtra("badge_count_package_name", context.getPackageName());
    intent.putExtra("badge_count_class_name", launcherClassName);
    context.sendBroadcast(intent);
  }

  private void setBadgeSony(Context context, int count) {
    String launcherClassName = getLauncherClassName(context);
    if (launcherClassName == null) {
      return;
    }

    Intent intent = new Intent();
    intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherClassName);
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", true);
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(count));
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());

    context.sendBroadcast(intent);
  }


  private void clearBadgeSony(Context context) {
    String launcherClassName = getLauncherClassName(context);
    if (launcherClassName == null) {
      return;
    }

    Intent intent = new Intent();
    intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherClassName);
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", false);
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(0));
    intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());

    context.sendBroadcast(intent);
  }

  private String getLauncherClassName(Context context) {

    PackageManager pm = context.getPackageManager();

    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);

    List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
    for (ResolveInfo resolveInfo : resolveInfos) {
      String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
      if (pkgName.equalsIgnoreCase(context.getPackageName())) {
        String className = resolveInfo.activityInfo.name;
        return className;
      }
    }
    return null;
  }


}
