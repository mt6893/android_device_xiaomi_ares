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
                enableLed(true);
            } else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                enableLed(true);
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                enableLed(false);
            }
        }
    }

    private static String LED_PATH = "/sys/class/leds/blue/brightness";

    public static void enableLed(boolean enable) {
        if (Utils.fileExists(LED_PATH)) {
            Utils.writeLine(LED_PATH, enable ? "255" : "0");
        }
    }
}
