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

package org.aospextended.device;

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
    boolean mStop;
int r,g,b;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

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
        stopDisco();
        mStop = !play;
        AsyncTask.execute(() -> disco(play));
    }

    private void disco(boolean play) {
        boolean playInGames = mPrefs.getBoolean("led_in_games", false);
        if (playInGames && !Utils.isGameApp(mContext)) return;
        if (play) {
            mUpdateInfo.run();
        }
    }

    private void stopDisco() {
        Utils.writeValue("/sys/class/leds/red/brightness", "0");
        Utils.writeValue("/sys/class/leds/green/brightness", "0");
        Utils.writeValue("/sys/class/leds/blue/brightness", "0");
        mHandler.removeCallbacks(mUpdateInfo);
    }

    private final Runnable mUpdateInfo = new Runnable() {
        public void run() {
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            Random r = new Random();
            int R = r.nextInt(155) + 50;
            int G = r.nextInt(155) + 50;
            int B = r.nextInt(155) + 50;
//            Utils.writeValue("/sys/class/leds/red/brightness", String.valueOf(R));
//            Utils.writeValue("/sys/class/leds/green/brightness", String.valueOf(G));
//            Utils.writeValue("/sys/class/leds/blue/brightness", String.valueOf(B));
//inc(R,G,B);
//dec(R,G,B);

            loop();
            if (mHandler != null) {
                mHandler.postAtTime(mUpdateInfo, next);
            }
        }
    };

    private void loop() {
/*        setColor(255, 255, 255);
        unsetColor(255,255,255);
        setColor(255,0,0);
unsetColor(255,0,0);
        setColor(255,255,0);
unsetColor(255,255,0);
        setColor(0,255,0);
unsetColor(0,255,0);
        setColor(0,255,255);
unsetColor(0,255,255);
        setColor(0,0,255);
unsetColor(0,0,255);*/
        Thread t1 = new Thread(() ->
        setColor(255,0,255));
        t1.start();
        try {
        t1.join();
        } catch (Exception e) {}
        Thread t2 = new Thread(() ->
        unsetColor(255,0,255));
        t2.start();
        try {
        t2.join();
        } catch (Exception e) {}
    }



    private void setColor(int red, int green, int blue) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<=red;i+=10) {
                    r = i;
                    _setColor();
                }
            }
        });

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<=green;i+=10) {
                    g = i;
                    _setColor();
                }
            }
        });

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<=blue;i+=10) {
                    b = i;
                    _setColor();
                }
            }
        });
/*
        try {
            latch.await();
        } catch (InterruptedException e) {
        }*/
    }

    private void unsetColor(int red, int green, int blue) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=red;i>=0;i-=10) {
                    r = i;
                    _setColor();
                }
            }
        });

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=green;i>=0;i-=10) {
                    g = i;
                    _setColor();
                }
            }
        });

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=blue;i>=0;i-=10) {
                    b = i;
                    _setColor();
                }
            }
        });
/*
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
*/
    }

    private void _setColor() {
        Slog.d("sagarled", "r=" + r + ", g=" + g + ", b=" + b);
        if (mStop) {
            stopDisco();
        } else {
            Utils.writeValue("/sys/class/leds/red/brightness", String.valueOf(r));
            Utils.writeValue("/sys/class/leds/green/brightness", String.valueOf(g));
            Utils.writeValue("/sys/class/leds/blue/brightness", String.valueOf(b));
        }
    }
}

