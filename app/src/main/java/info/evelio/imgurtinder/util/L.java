package info.evelio.imgurtinder.util;

import android.util.Log;

import info.evelio.imgurtinder.BuildConfig;


public class L {
  // Using String.valueOf as a null msg would result on NPE

  static final boolean ENABLED = BuildConfig.DEBUG;
  public static void e(String tag, String msg, Throwable tr) {
    if (ENABLED) {
      Log.e(tag, String.valueOf(msg), tr);
    }
  }

  //
  public static void e(String tag, String msg) {
    if (ENABLED) {
      Log.e(tag, String.valueOf(msg));
    }
  }

  public static void d(String tag, String msg) {
    if (ENABLED) {
      Log.d(tag, String.valueOf(msg));
    }
  }
}
