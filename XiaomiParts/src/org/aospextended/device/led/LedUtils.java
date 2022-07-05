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

package org.aospextended.device.led;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.app.Fragment;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import org.aospextended.device.gestures.TouchGestures;
import org.aospextended.device.gestures.TouchGesturesActivity;
import org.aospextended.device.util.AppListActivity;
import org.aospextended.device.doze.DozeSettingsActivity;
import org.aospextended.device.vibration.VibratorStrengthPreference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.Random;
import java.lang.Runnable;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import android.util.Slog;
import android.os.SystemProperties;
import android.os.Looper;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.*;
import android.widget.Toast;

import org.aospextended.device.R;
import org.aospextended.device.util.Utils;
import org.aospextended.device.triggers.TriggerService;

public class LedUtils {
    private static final boolean DEBUG = Utils.DEBUG;
    private static final String TAG = "LedUtils";

    private Context mContext;
    private SharedPreferences mPrefs;
    public static LedUtils sInstance;
    private boolean mStop;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private static String RED_LED_PATH = "/sys/class/leds/red/brightness";
    private static String GREEN_LED_PATH = "/sys/class/leds/green/brightness";
    private static String BLUE_LED_PATH = "/sys/class/leds/blue/brightness";

    private Executor mExecutor = new ThreadPoolExecutor(/*corePoolSize=*/3,
            Runtime.getRuntime().availableProcessors(), /*keepAliveTime*/ 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    private CountDownLatch latch = new CountDownLatch(3);

    public static LedUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LedUtils(context);
        }
        return sInstance;
    }

    private LedUtils(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("XiaomiParts.HandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mPrefs = Utils.getSharedPreferences(context);
    }

    public void play(boolean play) {
        play(play, false);
    }

    public void play(boolean play, boolean force) {
        stopDisco();
        mStop = !play;
        AsyncTask.execute(() -> disco(play, force));
    }

    private void disco(boolean play, boolean force) {
        boolean playInGames = mPrefs.getBoolean("led_in_games", false);
        if ((play && !(playInGames && !Utils.isGameApp(mContext))) || force) {
            mUpdateInfo.run();
        }
    }

    private void stopDisco() {
        Utils.writeValue(RED_LED_PATH, "0");
        Utils.writeValue(GREEN_LED_PATH, "0");
        Utils.writeValue(BLUE_LED_PATH, "0");
        mHandler.removeCallbacks(mUpdateInfo);
    }

    private int rgb_limit(int value) {
            int x = 35;
            int y = 1;
            int _value = value;

            if (y == 1) {
                _value += x;
            } else {
                _value -= x;
            }

            if (_value >= 255) {
                _value = 255;
                y = 0;
            }

            if (_value < 0) {
                _value = 0;
                y = 1;
            }

            return _value;
    }

    private final Runnable mUpdateInfo = new Runnable() {
        public void run() {
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            Random r = new Random();
            int R = rgb_limit(r.nextInt(255));
            int G = rgb_limit(r.nextInt(255));
            int B = rgb_limit(r.nextInt(255));

            Utils.writeValue(RED_LED_PATH, String.valueOf(R));
            Utils.writeValue(GREEN_LED_PATH, String.valueOf(G));
            Utils.writeValue(BLUE_LED_PATH, String.valueOf(B));

            if (mHandler != null) {
                mHandler.postAtTime(mUpdateInfo, next);
            }
        }
    };
}

