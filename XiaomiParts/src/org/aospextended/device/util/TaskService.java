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
import android.os.AsyncTask;
import android.util.Slog;

import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.app.ActivityTaskManager;
import android.content.pm.ActivityInfo;

import android.os.IBinder;
import android.util.Log;
import android.app.Service;
import android.content.Intent;
import android.provider.Settings;

import org.aospextended.device.led.LedUtils;

public class TaskService extends Service {

    private static final String TAG = "TaskService";
    private static final boolean DEBUG = Utils.DEBUG;

    private Context mContext;
    private ComponentName mTaskComponentName;
    private PackageManager mPm;

    private final TaskStackChangeListener mTaskListener = new TaskStackChangeListener() {
        @Override
        public void onTaskStackChanged() {
            AsyncTask.execute(() -> {
                try {
                    final ActivityTaskManager.RootTaskInfo focusedStack =
                            ActivityTaskManager.getService().getFocusedRootTaskInfo();
                    if (focusedStack != null && focusedStack.topActivity != null) {
                        mTaskComponentName = focusedStack.topActivity;
                    }
                } catch (Exception e) {}
                try {
                    final ActivityInfo ai = mPm.getActivityInfo(mTaskComponentName, 0);
                    String appName =  ai.applicationInfo.packageName;
                    saveAppName(appName);
                } catch (PackageManager.NameNotFoundException e) {
                }
            });
        }
    };

    @Override
    public void onCreate() {
        mPm = getPackageManager();

        ActivityManagerWrapper.getInstance().registerTaskStackListener(mTaskListener);

        mTaskListener.onTaskStackChanged();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void saveAppName(String appName) {
        if (DEBUG) Log.d(TAG, "appName=" + appName);
        Settings.System.putString(getContentResolver(), "appName", appName);
        LedUtils ledUtils = LedUtils.getInstance(this);
        ledUtils.play(Utils.isGameApp(this) && Utils.getSharedPreferences(this).getBoolean("led_disco", false));
    }
}
