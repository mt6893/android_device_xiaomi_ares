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

import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.Display.INVALID_DISPLAY;

import android.content.BroadcastReceiver;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManagerGlobal;
import android.view.ViewConfiguration;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;
import org.aospextended.device.util.Action;
import org.aospextended.device.util.Action;
import org.aospextended.device.util.Utils;
import org.aospextended.device.doze.DozeUtils;
import org.aospextended.device.gestures.TouchGestures;
import org.aospextended.device.triggers.TriggerService;
import org.aospextended.device.triggers.TriggerUtils;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = Utils.TAG;
    private static final boolean DEBUG = Utils.DEBUG;

    private static final int GESTURE_REQUEST = 1;
    private static final int GESTURE_WAKELOCK_DURATION = 2000;

    private static final int GESTURE_DOUBLE_TAP_SCANCODE = 250;
    private static final int GESTURE_W_SCANCODE = 246;
    private static final int GESTURE_M_SCANCODE = 247;
    private static final int GESTURE_CIRCLE_SCANCODE = 249;
    private static final int GESTURE_TWO_SWIPE_SCANCODE = 248;
    private static final int GESTURE_UP_ARROW_SCANCODE = 252;
    private static final int GESTURE_DOWN_ARROW_SCANCODE = 251;
    private static final int GESTURE_LEFT_ARROW_SCANCODE = 254;
    private static final int GESTURE_RIGHT_ARROW_SCANCODE = 253;
    private static final int GESTURE_SWIPE_UP_SCANCODE = 256;
    private static final int GESTURE_SWIPE_DOWN_SCANCODE = 255;
    private static final int GESTURE_SWIPE_LEFT_SCANCODE = 258;
    private static final int GESTURE_SWIPE_RIGHT_SCANCODE = 257;

    private final Context mContext;
    private Context mAppContext = null;

    private EventHandler mEventHandler;

    private Vibrator mVibrator;

    private int mTriggerAction;

    long mPrevEventTime;
    boolean mLeft;
    boolean mRight;

    private final CustomSettingsObserver mCustomSettingsObserver;

    public TriggerUtils tr = null;

    public KeyHandler(Context context) {
        mContext = context;
        mEventHandler = new EventHandler();

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mAppContext = Utils.getAppContext(mContext);
        mCustomSettingsObserver = new CustomSettingsObserver(new Handler(Looper.getMainLooper()));
        mCustomSettingsObserver.observe();
        tr = TriggerUtils.getInstance(mAppContext);
    }

    private class CustomSettingsObserver extends ContentObserver {
        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void update() {
            onChange(false, Settings.System.getUriFor("trigger_sound"));
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("triggerleft"),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("triggerright"),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("trigger_sound"),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor("trigger_sound"))) {
                if (Settings.System.getInt(mContext.getContentResolver(), "trigger_sound", 0) == 1) {
                    tr.loadSoundResource();
                } else {
                    tr.releaseSoundResource();
                }
                return;
            }
            boolean left = uri.equals(Settings.System.getUriFor("triggerleft"));
            boolean open = Utils.getIntSystem(mContext, left ? "triggerleft" : "triggerright", -1) == 1;
            tr.triggerAction(left, open);
            long now = SystemClock.uptimeMillis();
            long time = now - mPrevEventTime;
            if (DEBUG) Slog.d(TAG, "new intent: mLeft=" + mLeft + ", mRight=" + mRight + ", left=" + left + ", open=" + open + ", now=" + now + ", time=" + time + ", (mLeft && !left && open)=" + (mLeft && !left && open) + ", (mRight && left && open)=" + (mRight && left && open));
            if (time < 10000 && ((mLeft && !left && open) || (mRight && left && open))) {
                if (DEBUG) Slog.d(TAG, "starting service");
                mAppContext.startService(new Intent(mAppContext, TriggerService.class));
            }
            mPrevEventTime = now;
            mLeft = left && open;
            mRight = !left && open;
        }

    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            KeyEvent event = (KeyEvent) msg.obj;
            String action = null;

            // Utils.getSharedPreferences does not work here
            SharedPreferences mPref = mAppContext.getSharedPreferences("org.aospextended.device_preferences",
                    Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

            switch(event.getScanCode()) {
            case GESTURE_DOUBLE_TAP_SCANCODE:
                if (mPref.getBoolean(TouchGestures.PREF_DT2W_ENABLE, true)) {
                    action = mPref.getString(TouchGestures.PREF_GESTURE_DOUBLE_TAP,
                            Action.ACTION_WAKE_DEVICE);
                            doHapticFeedback();

                    if (mPref.getBoolean(DozeUtils.GESTURE_DOUBLE_TAP, false)) {
                        action = null;
                        DozeUtils.launchDozePulse(mAppContext);
                        doHapticFeedback();
                    }
                }
                break;
            case GESTURE_W_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_W,
                        Action.ACTION_CAMERA);
                        doHapticFeedback();
                break;
            case GESTURE_M_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_M,
                        Action.ACTION_MEDIA_PLAY_PAUSE);
                        doHapticFeedback();
                break;
            case GESTURE_CIRCLE_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_CIRCLE,
                        Action.ACTION_TORCH);
                        doHapticFeedback();
                break;
            case GESTURE_TWO_SWIPE_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_TWO_SWIPE,
                        Action.ACTION_MEDIA_PREVIOUS);
                        doHapticFeedback();
                break;
            case GESTURE_UP_ARROW_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_UP_ARROW,
                        Action.ACTION_WAKE_DEVICE);
                        doHapticFeedback();
                break;
            case GESTURE_DOWN_ARROW_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_DOWN_ARROW,
                        Action.ACTION_VIB_SILENT);
                        doHapticFeedback();
                break;
            case GESTURE_LEFT_ARROW_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_LEFT_ARROW,
                        Action.ACTION_MEDIA_PREVIOUS);
                        doHapticFeedback();
                break;
            case GESTURE_RIGHT_ARROW_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_RIGHT_ARROW,
                        Action.ACTION_MEDIA_NEXT);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_UP_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_SWIPE_UP,
                        Action.ACTION_WAKE_DEVICE);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_DOWN_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_SWIPE_DOWN,
                        Action.ACTION_VIB_SILENT);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_LEFT_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_SWIPE_LEFT,
                        Action.ACTION_MEDIA_PREVIOUS);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_RIGHT_SCANCODE:
                action = mPref.getString(TouchGestures.PREF_GESTURE_SWIPE_RIGHT,
                        Action.ACTION_MEDIA_NEXT);
                        doHapticFeedback();
                break;
            }

            if (DEBUG) Slog.d(TAG, "scancode: " + event.getScanCode() + ", action: " + action);

            if (action == null || action.equals(Action.ACTION_NULL)) return;

            if (action.equals(Action.ACTION_CAMERA)) {
                Action.processAction(mContext, Action.ACTION_WAKE_DEVICE, false);
            }
            Action.processAction(mContext, action, false);
        }
    }

    private void doHapticFeedback() {
        boolean enabled = Utils.getInt(mAppContext, Utils.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0;
        if (enabled && mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(50);
        }
    }

    public KeyEvent handleKeyEvent(KeyEvent event) {
        if (DEBUG) Slog.d(TAG, "Got KeyEvent: " + event);

        if (event.getDevice().getProductId() == 1576) {
            return handleTriggerEvent(event);
        }

        if (event.getAction() != KeyEvent.ACTION_UP) {
            return event;
        }
        int scanCode = event.getScanCode();
        if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
            Message msg = getMessageForKeyEvent(event);
            mEventHandler.sendMessage(msg);
        }

        return event;
    }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }

    public KeyEvent handleTriggerEvent(KeyEvent event) {
        if (!Utils.isGameApp(mContext)) return event;
        boolean down = event.getAction() == MotionEvent.ACTION_DOWN;
        Utils.writeValue(event.getKeyCode() == 131 ?
                "/proc/touchpanel/left_trigger_enable" :
                "/proc/touchpanel/right_trigger_enable" , down ? "1" : "0");

        return event;
    }

    private void injectMotionEvent(int id, int inputSource, int action, long downTime, long when,
            float x, float y, float pressure, int displayId) {
        final int pointerCount = id;
        MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerProperties[i].id = i;
            pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            pointerCoords[i] = new MotionEvent.PointerCoords();
            pointerCoords[i].x = x;
            pointerCoords[i].y = y;
            pointerCoords[i].pressure = pressure;
            pointerCoords[i].size = 1.0f;
        }
        if (displayId == INVALID_DISPLAY
                && (inputSource & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            displayId = DEFAULT_DISPLAY;
        }
        MotionEvent event = MotionEvent.obtain(downTime, when, action, pointerCount,
                pointerProperties, pointerCoords, 0, 0,
                1.0f, 1.0f, getInputDeviceId(inputSource),
                0, inputSource, displayId, 0);
        InputManager.getInstance().injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private int getInputDeviceId(int inputSource) {
        int[] devIds = InputDevice.getDeviceIds();
        for (int devId : devIds) {
            InputDevice inputDev = InputDevice.getDevice(devId);
            if (inputDev.supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }
}
