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
import android.content.Context;
import android.content.Intent;
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

public class TriggerService extends Service implements View.OnTouchListener, View.OnClickListener {
    private static final boolean DEBUG = Utils.DEBUG;
    private static final String TAG = "TriggerService";

    private SharedPreferences mPrefs;

    private View mView;
    private ImageView image1, image2;
    private WindowManager windowManager;
    private Button button;
    Point p = new Point();
    WindowManager.LayoutParams layoutParams;

    float X, Y, mLX, mLY, mRX, mRY;
    float mBX = 100;
    float mBY = 1800;
    public static void onBoot(Context context) {
        SharedPreferences prefs = Utils.getSharedPreferences(context);
        Utils.writeValue("/proc/touchpanel/left_trigger_x", prefs.getString("left_trigger_x", "540"));
        Utils.writeValue("/proc/touchpanel/left_trigger_y", prefs.getString("left_trigger_y", "700"));
        Utils.writeValue("/proc/touchpanel/right_trigger_x", prefs.getString("right_trigger_x", "540"));
        Utils.writeValue("/proc/touchpanel/right_trigger_y", prefs.getString("right_trigger_y", "1700"));
    }

    @Override
    public void onCreate() {
        mPrefs = Utils.getSharedPreferences(this);

        onBoot(this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(p);

        mView = LayoutInflater.from(this).inflate(R.layout.view, null);
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
/*
        LayoutParams params = (LayoutParams) button.getLayoutParams();
        params.leftMargin = 10;
        params.topMargin = p.y - 500;
        button.setLayoutParams(params);
*/
        button.setOnClickListener(this);
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(TriggerService.this, "Resetted values", Toast.LENGTH_LONG).show();
                reset();
                return true;
            }
        });

        button.animate()
                .x(mBX)
                .y(mBY)
                .setDuration(0)
                .start();

        mView.setAlpha(0.5f);
    }

    public void show() {
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

        mView.setVisibility(View.VISIBLE);
        image1.setVisibility(View.VISIBLE);
        image2.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            X = v.getX() - event.getRawX();
            Y = v.getY() - event.getRawY();
	} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getRawX();
            float y = event.getRawY();

            v.animate()
                    .x(event.getRawX() + X)
                    .y(event.getRawY() + Y)
                    .setDuration(0)
                    .start();

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
        hide();
    }

    public void reset() {
        if (DEBUG) Slog.d(TAG, "reset values");
        mLX = 540;
        mLY = 700;
        mRX = 540;
        mRY = 1700;
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

        updatePosition();
    }


    public void hide() {
        if (DEBUG) Slog.d(TAG, "hide");
        windowManager.removeView(mView);
        stopSelf();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Slog.d(TAG, "Starting service");
        show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Slog.d(TAG, "Destroying service");
        //hide();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updatePosition();
    }

    private void updatePosition() {
        Display defaultDisplay = windowManager.getDefaultDisplay();

        Point size = new Point();
        defaultDisplay.getRealSize(size);

        int rotation = defaultDisplay.getRotation();
        int x, y;
        float LX, LY, RX, RY, BX, BY;
        switch (rotation) {
            case Surface.ROTATION_90:
                LX = mLY;
                LY = mLX;
                RX = mRY;
                RY = mRX;
                BX = mBY;
                BY = size.y - mBX - 100;
                image1.setRotation(0f);
                image2.setRotation(0f);
                button.setRotation(0f);
                break;
            case Surface.ROTATION_180:
                LX = mLX;
                LY = size.y - mLY;
                RX = mRX;
                RY = size.y - mRY;
                BX = mBX;
                BY = size.y - mBY;
                image1.setRotation(0f);
                image2.setRotation(0f);
                break;
            case Surface.ROTATION_270:
                LX = size.x - mLY;
                LY = mLX;
                RX = size.x - mRY;
                RY = mRX;
                BX = size.x - mBY;
                BY = mBX;
                image1.setRotation(180f);
                image2.setRotation(180f);
                button.setRotation(180f);
                break;
            default:
                LX = mLX;
                LY = mLY;
                RX = mRX;
                RY = mRY;
                BX = mBX;
                BY = mBY;
                image1.setRotation(90f);
                image2.setRotation(90f);
                button.setRotation(90f);
        }

        image1.animate()
                .x(LX)
                .y(LY)
                .setDuration(0)
                .start();

        image2.animate()
                .x(RX)
                .y(RY)
                .setDuration(0)
                .start();

        button.animate()
                .x(BX)
                .y(BY)
                .setDuration(0)
                .start();

    }
}
