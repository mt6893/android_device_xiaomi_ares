/*
 * Copyright (C) 2020 The AospExtended Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aospextended.device.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings;
import android.util.Slog;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String TAG = "XiaomiParts";
    public static final boolean DEBUG = true;

    public static final String PREFERENCES = "XiaomiPartsPreferences";
    public static final String AMBIENT_GESTURE_HAPTIC_FEEDBACK =
            "AMBIENT_GESTURE_HAPTIC_FEEDBACK";
    public static final String TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK =
            "TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK";

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    public static SharedPreferences getSharedPreferences(Context context) {
        return getAppContext(context).getSharedPreferences("org.aospextended.device_preferences",
                Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public static Context getAppContext(Context context) {
        try {
            return context.createPackageContext(
                    "org.aospextended.device", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
        }
        return context;
    }


    public static boolean putStringSystem(Context context, String name, String value) {
        boolean ret = Settings.System.putString(context.getContentResolver(), name, value);
        return ret;
    }


    public static String getStringSystem(Context context, String name, String def) {
        String ret = Settings.System.getString(context.getContentResolver(), name);
        return ret == null ? def : ret;
    }


    public static int getIntSystem(Context context, String name, int def) {
        int ret = Settings.System.getInt(context.getContentResolver(), name, def);
        return ret;
    }

    public static boolean putIntSystem(Context context, String name, int value) {
        boolean ret = Settings.System.putInt(context.getContentResolver(), name, value);
        return ret;
    }

    public static int getInt(Context context, String name, int def) {
        SharedPreferences settings = getSharedPreferences(context);
        return settings.getInt(name, def);
    }

    public static boolean putInt(Context context, String name, int value) {
        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(name, value);
        return editor.commit();
    }

    public static boolean isGameApp(Context context) {
        String appName = Settings.System.getString(context.getContentResolver(), "appName");
        String appList = Settings.System.getString(context.getContentResolver(), "game_app_list");
        boolean isGameApp = appList != null && appList.contains(appName);
        if (DEBUG) Slog.d(TAG, "appName: " + appName + " appList: " + appList + " isGameApp: " + isGameApp);
        return isGameApp;
    }

    /**
     * Write a string value to the specified file.
     * @param filename The filename
     * @param value The value
     */
    public static void writeValue(String filename, String value) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(filename));
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the "color value" to the specified file. The value is scaled from
     * an integer to an unsigned integer by multiplying by 2.
     * @param filename The filename
     * @param value The value of max value Integer.MAX
     */
    public static void writeColor(String filename, int value) {
        writeValue(filename, String.valueOf((long) value * 2));
    }

    /**
     * Write the "gamma value" to the specified file.
     * @param filename The filename
     * @param value The value
     */
    public static void writeGamma(String filename, int value) {
        writeValue(filename, String.valueOf(value));
    }

    /**
     * Check if the specified file exists.
     * @param filename The filename
     * @return Whether the file exists or not
     */
    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    public static boolean fileWritable(String filename) {
        return fileExists(filename) && new File(filename).canWrite();
    }

    public static String readLine(String filename) {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(filename), 1024);
            line = br.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return line;
    }

    public static boolean getFileValueAsBoolean(String filename, boolean defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return (fileValue.equals("0")?false:true);
        }
        return defValue;
    }

    public static String getFileValue(String filename, String defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return fileValue;
        }
        return defValue;
    }

    /**
     * Reads the first line of text from the given file
     */
    public static String readOneLine(String fileName) {
        String line = null;
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(fileName), 512);
            line = reader.readLine();
        } catch (IOException e) {
            Slog.e(TAG, "Could not read from file " + fileName, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignored, not much we can do anyway
            }
        }

        return line;
    }

    public static boolean getTriggerEnabled(int position) {
        File file = new File("/dev/gamekey");
        if (file.exists()) {
            file.length();
            byte[] b3 = new byte[4];
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                fileInputStream.read(b3);
                fileInputStream.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            if (DEBUG) Slog.d(TAG, "triggerleft=" + b3[0] + ", triggerright=" + b3[1]);
            return b3[position] == 1;
        }
        return false;
    }

    /**
     * Writes the given value into the given file
     *
     * @return true on success, false on failure
     */
    public static boolean writeLine(String fileName, String value) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Slog.e(TAG, "Could not write to file " + fileName, e);
            return false;
        }

        return true;
    }
}
