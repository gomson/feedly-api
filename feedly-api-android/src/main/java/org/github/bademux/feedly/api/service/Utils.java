/*
 * Copyright 2014 Bademus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *    Contributors:
 *                 Bademus
 */

package org.github.bademux.feedly.api.service;

import com.google.api.client.util.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.github.bademux.feedly.api.service.ServiceManager.Status;

public final class Utils {

  public static void store(final Context context, Status status) {
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(context.openFileOutput(STATUS_TMP, Context.MODE_PRIVATE));
      out.writeObject(status);
    } catch (IOException e) {
      Log.e(ServiceManager.TAG, "can't write status file", e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          Log.e(ServiceManager.TAG, "", e);
        }
      }
    }
  }

  public static Status load(final Context context) {
    ObjectInput in = null;
    try {
      in = new ObjectInputStream(context.openFileInput(STATUS_TMP));
      return (Status) in.readObject();
    } catch (FileNotFoundException e) {
      //ignore and creare new one
    } catch (IOException e) {
      Log.e(ServiceManager.TAG, "can't read status file", e);
    } catch (ClassNotFoundException e) {
      Log.e(ServiceManager.TAG, "", e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          Log.e(ServiceManager.TAG, "", e);
        }
      }
    }

    return null;
  }

  public static void initAll(final Context context, final Status status) {
    initNetworkStatus(context, status);
    initBatteryStatus(context, status);
  }

  public static void initNetworkStatus(final Context context, final Status status) {
    Intent intent = context.registerReceiver(null,
                                             new IntentFilter(
                                                 ConnectivityManager.CONNECTIVITY_ACTION)
    );
    if (intent == null) {
      return;
    }

    status.isNetworkAvailable =
        !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
  }

  public static void initBatteryStatus(final Context context, final Status status) {
    Intent intent = context.registerReceiver(null,
                                             new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (intent == null) {
      return;
    }

    final int state = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    status.isPowerConnected = state == BatteryManager.BATTERY_STATUS_CHARGING
                              || state == BatteryManager.BATTERY_STATUS_FULL;

    final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    if (level > 0 && scale > 0) {
      status.isBatteryOk = (level / (float) scale) * 100 > BATTERY_LOW;
    }
  }

  /**
   * Given a URL, establishes an HttpUrlConnection and retrieves
   * content as a OutputStream
   */
  public static int download(String myurl, OutputStream outputStream) throws IOException {
    InputStream is = null;
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(myurl).openConnection();
      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      // Starts the query
      conn.connect();

      IOUtils.copy(conn.getInputStream(), outputStream, false);
      return conn.getResponseCode();
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  public final static File getFilesDir(Context context) {
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      throw new RuntimeException("External media is not mounted");
    }
    return context.getExternalFilesDir(null);
  }

  public static final File getCacheDir(final Context context) {
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      throw new RuntimeException("External media is not mounted");
    }
    return context.getExternalCacheDir();
  }

  public static boolean cleanDir(File dir) {
    if (dir.isDirectory()) {
      for (String children : dir.list()) {
        new File(dir, children).delete();
      }
    }
    return dir.delete(); // The directory is empty now and can be deleted.
  }

  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      for (String children : dir.list()) {
        if (!deleteDir(new File(dir, children))) {
          return false;
        }
      }
    }
    return dir.delete(); // The directory is empty now and can be deleted.
  }

  public final static int BATTERY_LOW = 15;

  private static final String STATUS_TMP = "status.tmp";

  private Utils() {}
}
