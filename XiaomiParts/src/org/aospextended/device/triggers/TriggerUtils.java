package org.aospextended.device.triggers;

import android.util.Slog;
import android.media.SoundPool;
import android.content.Context;
import android.util.ArrayMap;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Slog;
import android.view.KeyEvent;

import libcore.io.IoUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.aospextended.device.R;
import org.aospextended.device.util.Action;
import org.aospextended.device.util.Utils;

public class TriggerUtils {
    private List<Integer> LOADED_SOUND_IDS = new ArrayList<>();
    private Map<String, Integer> SOUNDS_MAP = new HashMap<>();
    private static String TAG = "TriggerUtils";
    public static TriggerUtils sInstance;
    private Context mContext;
    private boolean mIsSoundPooLoadComplete;
    private SoundPool mSoundPool;

    private static boolean DEBUG = Utils.DEBUG;

    private TriggerUtils mTriggerUtils;
    long mPrevEventTime;
    int mKeycode, mEventAction, mCount, mTapCount;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                if (Settings.System.getInt(mContext.getContentResolver(), "trigger_sound", 0) == 1) {
                    loadSoundResource();
                }
            }
        }
    };

    public TriggerUtils(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public static TriggerUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TriggerUtils(context);
            if (DEBUG) Slog.d(TAG, "Creating new instance");
        }
        return sInstance;
    }

    public void triggerAction(boolean left, boolean open) {
        if (DEBUG) Slog.d(TAG, "left=" + left + ", open=" + open);
        boolean play = Settings.System.getInt(mContext.getContentResolver(), "trigger_sound", 0) == 1;
        if (!play) return;
        String type = Settings.System.getString(mContext.getContentResolver(), "trigger_sound_type");
        if (type == null) type = "classic";
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("-");
        sb.append(left ? 0 : 1);
        sb.append("-");
        sb.append(open ? 1 : 0);
        if (DEBUG) Slog.d(TAG, "sound=" + sb.toString() );
        playSound(sb.toString(), false);
    }

    private void checkSoundPoolLoadCompleted() {
        if (LOADED_SOUND_IDS.size() == 16) {
            mIsSoundPooLoadComplete = true;
        }
    }

    private void initSoundPool() {
        if (DEBUG) Slog.d(TAG, "initSoundPool");
        mSoundPool = new SoundPool(10, 1, 0);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int n, int n2) {
                if (DEBUG) Slog.d(TAG, "onLoadComplete: n=" + n + ", n2=" + n2);
                if (n2 == 0) {
                    LOADED_SOUND_IDS.add(n);
                    checkSoundPoolLoadCompleted();
                }
            }
        });
    }

    private int load(int n) {
        if (DEBUG) Slog.d(TAG, "load");
        if (mSoundPool == null) {
            if (DEBUG) Slog.d(TAG, "load: soundpool null");
            return -1;
        }
        return mSoundPool.load(mContext, n, 1);
    }

    public void loadSoundResource() {
        if (DEBUG) Slog.d(TAG, "loadSoundResource");
        releaseSoundResource();
        initSoundPool();
        SOUNDS_MAP.put("classic-0-0", load(R.raw.keys_kanata_close_l));
        SOUNDS_MAP.put("classic-1-0", load(R.raw.keys_kanata_close_r));
        SOUNDS_MAP.put("classic-0-1", load(R.raw.keys_kanata_open_l));
        SOUNDS_MAP.put("classic-1-1", load(R.raw.keys_kanata_open_r));
        SOUNDS_MAP.put("bullet-0-0", load(R.raw.keys_mechanicals_close_l));
        SOUNDS_MAP.put("bullet-1-0", load(R.raw.keys_mechanicals_close_r));
        SOUNDS_MAP.put("bullet-0-1", load(R.raw.keys_mechanicals_open_l));
        SOUNDS_MAP.put("bullet-1-1", load(R.raw.keys_mechanicals_open_r));
        SOUNDS_MAP.put("current-0-0", load(R.raw.keys_scifi_close_l));
        SOUNDS_MAP.put("current-1-0", load(R.raw.keys_scifi_close_r));
        SOUNDS_MAP.put("current-0-1", load(R.raw.keys_scifi_open_l));
        SOUNDS_MAP.put("current-1-1", load(R.raw.keys_scifi_open_r));
        SOUNDS_MAP.put("wind-0-0", load(R.raw.keys_car_close_l));
        SOUNDS_MAP.put("wind-1-0", load(R.raw.keys_car_close_r));
        SOUNDS_MAP.put("wind-0-1", load(R.raw.keys_car_open_l));
        SOUNDS_MAP.put("wind-1-1", load(R.raw.keys_car_open_r));
        SOUNDS_MAP.entrySet().forEach(entry -> {
        if (DEBUG) Slog.d(TAG, "SOUNDS_MAP: key=" + entry.getKey() + ", value=" + entry.getValue());
        });
    }

    public void pause(int n) {
        mSoundPool.pause(n);
    }

    public void playSound(String s, boolean b) {
        if (DEBUG) Slog.d(TAG, "playSound: " + s);
        if (!mIsSoundPooLoadComplete) {
            if (DEBUG) Slog.d(TAG, "playSound: " + mIsSoundPooLoadComplete + ", size=" + LOADED_SOUND_IDS.size());
            return;
        }
        if (SOUNDS_MAP.containsKey(s)) {
            int intValue = SOUNDS_MAP.get(s);
            int n;
            if (b) {
                n = -1;
            }
            else {
                n = 0;
            }
            if (DEBUG) Slog.d(TAG, "playSound: playing: intValue=" + intValue);
            mSoundPool.play(intValue, 1.0f, 1.0f, 1, n, 0.95f);
        }
    }

    public void releaseSoundResource() {
        if (mSoundPool != null) {
            if (DEBUG) Slog.d(TAG, "SoundPool release");
            mIsSoundPooLoadComplete = false;
            SOUNDS_MAP.clear();
            LOADED_SOUND_IDS.clear();
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    public void onEvent(KeyEvent event) {
        if (DEBUG) Slog.d(TAG, "onEvent");
        if (isDoubleClick(event)) {
            handleDoubleClick(event.getKeyCode() == 131);
        }
        if (isLongPress(event)) {
            handleLongPress(event.getKeyCode() == 131);
        }
    }

    public void handleDoubleClick(boolean left) {
        String action = null;
        if (left) {
            action = Utils.getStringSystem(mContext, CustomTrigger.PREF_LEFT_TRIGGER_DOUBLE_CLICK,
                    Action.ACTION_NULL);
        } else {
            action = Utils.getStringSystem(mContext, CustomTrigger.PREF_RIGHT_TRIGGER_DOUBLE_CLICK,
                    Action.ACTION_NULL);
        }
        if (DEBUG) Slog.d(TAG, "handleDoubleClick: " + left + " action: " + action);
        processAction(action);
    }

    public void processAction(String action) {
        boolean enableCustomTrigger = Utils.getIntSystem(mContext, CustomTrigger.PREF_CUSTOM_TRIGGER_ENABLE, 1) == 1;
        if (action == null || action.equals(Action.ACTION_NULL)
                || Utils.isGameApp(mContext) || !enableCustomTrigger) return;
        doHapticFeedback();
        Action.processAction(mContext, action, false);
    }

    public void handleLongPress(boolean left) {
        String action = null;
        if (left) {
            action = Utils.getStringSystem(mContext, CustomTrigger.PREF_LEFT_TRIGGER_LONGPRESS,
                    Action.ACTION_NULL);
        } else {
            action = Utils.getStringSystem(mContext, CustomTrigger.PREF_RIGHT_TRIGGER_LONGPRESS,
                    Action.ACTION_NULL);
        }
        if (DEBUG) Slog.d(TAG, "handleLongpress: " + left + " action: " + action);
        processAction(action);
    }

    private void doHapticFeedback() {
        boolean enabled = Utils.getIntSystem(mContext, CustomTrigger.KEY_TRIGGER_HAPTIC_FEEDBACK, 1) != 0;
        Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (enabled && mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(50);
        }
    }

    public boolean isDoubleClick(KeyEvent event) {
        if (DEBUG) Slog.d(TAG, "isDoubleClick");
        if (event.getAction() != KeyEvent.ACTION_UP) {
            mKeycode = event.getKeyCode();
            return false;
        }
        long now = SystemClock.uptimeMillis();
        long time = now - mPrevEventTime;
        boolean isDoubleClick = false;
        if (time < 300 && mTapCount == 2) {
            isDoubleClick = true;
        } else if (time > 300) {
            mTapCount = 0;
        }
        mTapCount++;
        mPrevEventTime = now;
        if (isDoubleClick) mTapCount = 0;
        return isDoubleClick;
    }

    public boolean isLongPress(KeyEvent event) {
        if (DEBUG) Slog.d(TAG, "isLongPress");
/*
        if (mEventAction != event.getAction()) {
             if (mCount > 12 ) {
                 mCount = 0;
                 return true;
             }
             mCount = 0;
             return false;
        }
        mCount++;
        mEventAction = event.getAction();
*/
        if (mEventAction == event.getAction()) {
            if (mCount > 12) {
                mCount = 0;
                return true;
            }
            mCount++;
        } else {
            mEventAction = event.getAction();
            mCount = 0;
        }
        return false;
    }
}
