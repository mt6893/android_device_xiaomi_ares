package org.aospextended.device.led;

import android.content.BroadcastReceiver;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.content.Intent;

import org.aospextended.device.util.Utils;

public class LedOnCall extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                enableLed(context, true);
            } else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                enableLed(context, true);
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                enableLed(context, false);
            }
        }
    }

    public static void enableLed(Context context, boolean enable) {
        enable = enable && Utils.getIntSystem(context, "led_in_calls", 1) == 1;
        LedUtils ledUtils = LedUtils.getInstance(context);
        ledUtils.play(enable, enable);
    }
}
