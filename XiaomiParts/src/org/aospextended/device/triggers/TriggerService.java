/*
 * Copyright (C) 2019 The LineageOS Project
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

package org.aospextended.device.triggers;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

import android.annotation.NonNull;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.aospextended.device.R;
import org.aospextended.device.util.Utils;

public class TriggerService implements View.OnTouchListener, View.OnClickListener {
    private static final boolean DEBUG = Utils.DEBUG;
    private static final String TAG = "TriggerService";

    private SharedPreferences mPrefs;

    private View mView;
    private ImageView image1, image2;
    private WindowManager windowManager;
    private ImageView button;
    Point p = new Point();
    WindowManager.LayoutParams layoutParams;

    float X, Y, mLX, mLY, mRX, mRY, lx, ly, rx, ry, bx, by;
    float mBX = 200;
    float mBY = 2000;
    int mHeight, mRotation;

    private Context mContext;
    private static TriggerService mInstance;

    private boolean mShowing;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                updatePosition(false);
            }
        }
    };

    public static void onBoot(Context context) {
        SharedPreferences prefs = Utils.getSharedPreferences(context);
        Utils.writeValue("/proc/touchpanel/left_trigger_x", prefs.getString("left_trigger_x", "540"));
        Utils.writeValue("/proc/touchpanel/left_trigger_y", prefs.getString("left_trigger_y", "700"));
        Utils.writeValue("/proc/touchpanel/right_trigger_x", prefs.getString("right_trigger_x", "540"));
        Utils.writeValue("/proc/touchpanel/right_trigger_y", prefs.getString("right_trigger_y", "1700"));
    }

    public static TriggerService getInstance(Context context) {
        if (mInstance == null) {
            Slog.d(TAG, "NEW INSTANCE");
            mInstance = new TriggerService(context);
        }
        return mInstance;
    }

    private TriggerService(Context context) {
        mContext = context;
    }

    public void init(Context context) {
        mPrefs = Utils.getSharedPreferences(context);

        onBoot(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        mContext.registerReceiver(mIntentReceiver, filter);

        mHeight = context.getResources().getDimensionPixelSize(R.dimen.image_height);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(p);

        mView = LayoutInflater.from(context).inflate(R.layout.view, null);
        image1 = mView.findViewById(R.id.image1);
        image2 = mView.findViewById(R.id.image2);

        image1.setOnTouchListener(this);
        image2.setOnTouchListener(this);

        mLX = Float.parseFloat(Utils.getFileValue("/proc/touchpanel/left_trigger_x", "0"));
        mLY = Float.parseFloat(Utils.getFileValue("/proc/touchpanel/left_trigger_y", "0"));

        image1.animate()
                .x(mLX)
                .y(mLY)
                .setDuration(0)
                .start();

        mRX = Float.parseFloat(Utils.getFileValue("/proc/touchpanel/right_trigger_x", "0"));
        mRY = Float.parseFloat(Utils.getFileValue("/proc/touchpanel/right_trigger_y", "0"));

        image2.animate()
                .x(mRX)
                .y(mRY)
                .setDuration(0)
                .start();

        if (DEBUG) Slog.d(TAG, "lxlyrxry " + mLX + " " + mLY + " " + mRX + " " + mRY);

        button = mView.findViewById(R.id.button);

        button.setOnClickListener(this);
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, "Resetted values", Toast.LENGTH_LONG).show();
                reset();
                return true;
            }
        });

        mBX = 200;
        mBY = 2000;

        button.animate()
                .x(mBX)
                .y(mBY)
                .setDuration(0)
                .start();

        mView.setAlpha(0.5f);
    }

    public void show() {
        if (mShowing) return;
        init(mContext);
        if (DEBUG) Slog.d(TAG, "show");
        layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSPARENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.setFitInsetsTypes(0);
        layoutParams.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {
            windowManager.addView(mView, layoutParams);
        } catch (RuntimeException e) {
        }
        mShowing = true;
        mView.setVisibility(View.VISIBLE);
        image1.setVisibility(View.VISIBLE);
        image2.setVisibility(View.VISIBLE);

        updatePosition(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            X = v.getX() - event.getRawX();
            Y = v.getY() - event.getRawY();
	} else if (event.getAction() == MotionEvent.ACTION_MOVE) {

            v.animate()
                    .x(event.getRawX() + X)
                    .y(event.getRawY() + Y)
                    .setDuration(0)
                    .start();
            float x = v.getX() + mHeight / 2;
            float y = v.getY() + mHeight / 2;
            boolean left = v.getId() == R.id.image1;
            if (left) {
                mLX = x;
                mLY = y;
            } else {
                mRX = x;
                mRY = y;
            }

            if (DEBUG) Slog.d(TAG, "action move x,y : " + x + "   " + y  + "  , isleft=" + (v.getId() == R.id.image1));
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (DEBUG) Slog.d(TAG, "wrote values");
        updatePosition(false, false);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("left_trigger_x", String.valueOf(lx));
        editor.putString("left_trigger_y", String.valueOf(ly));
        editor.putString("right_trigger_x", String.valueOf(rx));
        editor.putString("right_trigger_y", String.valueOf(ry));
        editor.commit();
	Utils.writeValue("/proc/touchpanel/left_trigger_x", String.valueOf(lx));
        Utils.writeValue("/proc/touchpanel/left_trigger_y", String.valueOf(ly));
        Utils.writeValue("/proc/touchpanel/right_trigger_x", String.valueOf(rx));
        Utils.writeValue("/proc/touchpanel/right_trigger_y", String.valueOf(ry));
        hide();
    }

    public void reset() {
        if (DEBUG) Slog.d(TAG, "reset values");
        mLX = 540;
        mLY = 700;
        mRX = 540;
        mRY = 1700;
        mBX = 200;
        mBY = 2000;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("left_trigger_x", String.valueOf(mLX));
        editor.putString("left_trigger_y", String.valueOf(mLY));
        editor.putString("right_trigger_x", String.valueOf(mRX));
        editor.putString("right_trigger_y", String.valueOf(mRY));
        editor.commit();
        Utils.writeValue("/proc/touchpanel/left_trigger_x", String.valueOf(mLX));
        Utils.writeValue("/proc/touchpanel/left_trigger_y", String.valueOf(mLY));
        Utils.writeValue("/proc/touchpanel/right_trigger_x", String.valueOf(mRX));
        Utils.writeValue("/proc/touchpanel/right_trigger_y", String.valueOf(mRY));

        updatePosition(false);
    }


    public void hide() {
        if (!mShowing) return;
        mShowing = false;
        if (DEBUG) Slog.d(TAG, "hide");
        windowManager.removeView(mView);
        mContext.unregisterReceiver(mIntentReceiver);
    }

    private void updatePosition(boolean def) {
        updatePosition(def, true);
    }

    private void updatePosition(boolean def, boolean update) {
        if (DEBUG) Slog.d(TAG, "updatePosition");
        Display defaultDisplay = windowManager.getDefaultDisplay();

        Point size = new Point();
        defaultDisplay.getRealSize(size);

        int rot = defaultDisplay.getRotation();
        int lastRotation = 0;
        if (def) mRotation = lastRotation;
        int x, y;
        float LX = mLX;
        float LY = mLY;
        float RX = mRX;
        float RY = mRY;
        float BX = mBX;
        float BY = mBY;
        int rotation = rot;
        if (!update) rotation = 0;
        switch (rotation) {
            case Surface.ROTATION_90:
                if (DEBUG) Slog.d(TAG, "ROTATION_90");
                if (mRotation == Surface.ROTATION_270) {
                    LX = size.x - mLX;
                    LY = size.y - mLY;
                    RX = size.x - mRX;
                    RY = size.y - mRY;
                    BX = size.x - mBX;
                    BY = size.y - mBY;
                } else {
                    LX = mLY;
                    LY = size.y - mLX;
                    RX = mRY;
                    RY = size.y - mRX;
                    BX = mBY;
                    BY = size.y - mBX;
                }
                image1.setRotation(0f);
                image2.setRotation(0f);
                button.setRotation(0f);
                break;
            case Surface.ROTATION_270:
                if (DEBUG) Slog.d(TAG, "ROTATION_270");
                if (mRotation == Surface.ROTATION_90) {
                    LX = size.x - mLX;
                    LY = size.y - mLY;
                    RX = size.x - mRX;
                    RY = size.y - mRY;
                    BX = size.x - mBX;
                    BY = size.y - mBY;
               } else {
                    LX = size.x - mLY;
                    LY = mLX;
                    RX = size.x - mRY;
                    RY = mRX;
                    BX = size.x - mBY;
                    BY = mBX;
                }
                image1.setRotation(180f);
                image2.setRotation(180f);
                button.setRotation(180f);
                break;
            default:
                if (DEBUG) Slog.d(TAG, "ROTATION_0");
                if (mRotation == Surface.ROTATION_90) {
                    LX = (!update ? 1080 : size.x) - mLY;
                    LY = mLX;
                    RX = (!update ? 1080 : size.x) - mRY;
                    RY = mRX;
                    BX = (!update ? 1080 : size.x) - mBY;
                    BY = mBX;
                } else if (mRotation == Surface.ROTATION_270) {
                    LX = mLY;
                    LY = (!update ? 2400 : size.y) - mLX;
                    RX = mRY;
                    RY = (!update ? 2400 : size.y) - mRX;
                    BX = mBY;
                    BY = (!update ? 2400 : size.y) - mBX;
                }
                image1.setRotation(90f);
                image2.setRotation(90f);
                button.setRotation(90f);
        }

        if (update) {
            image1.animate()
                    .x(LX - mHeight / 2)
                    .y(LY - mHeight / 2)
                    .setDuration(0)
                    .start();

            image2.animate()
                    .x(RX - mHeight / 2)
                    .y(RY - mHeight / 2 - (def ? mHeight : 0))
                    .setDuration(0)
                    .start();

            button.animate()
                    .x(BX - mHeight / 2)
                    .y(BY - mHeight / 2 - (def ? 2 * mHeight : 0))
                    .setDuration(0)
                    .start();

            mRotation = rotation;

            mLX = LX;
            mLY = LY;
            mRX = RX;
            mRY = RY;
            mBX = BX;
            mBY = BY;
        }

        lx = LX;
        ly = LY;
        rx = RX;
        ry = RY;
        bx = BX;
        by = BY;
    }
}
