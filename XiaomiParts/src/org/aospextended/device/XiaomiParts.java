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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
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
import org.aospextended.device.doze.DozeSettingsActivity;
import org.aospextended.device.vibration.VibratorStrengthPreference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

import android.util.Log;
import android.os.SystemProperties;
import java.io.*;
import android.widget.Toast;

import org.aospextended.device.R;
import org.aospextended.device.util.Utils;

public class XiaomiParts extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final boolean DEBUG = Utils.DEBUG;
    private static final String TAG = "XiaomiParts";

    public static final String PREF_OTG = "otg";
    public static final String OTG_PATH = "/sys/class/power_supply/usb/otg_switch";

    private Context mContext;
    private SharedPreferences mPrefs;

    private Preference mDozePref;
    private Preference mGesturesPref;
    private SwitchPreference mOTG;
    private VibratorStrengthPreference mVibratorStrength;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.XiaomiParts, rootKey);

        mPrefs = Utils.getSharedPreferences(getActivity());

        PreferenceCategory gestures = (PreferenceCategory) getPreferenceScreen()
                 .findPreference("gestures_category");
        mGesturesPref = findPreference("screen_gestures");
        mGesturesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getContext(), TouchGesturesActivity.class);
                startActivity(intent);
                return true;
            }
        });
        if (!TouchGestures.isSupported()) {
            getPreferenceScreen().removePreference(gestures);
        }

        mDozePref = findPreference("doze");
        mDozePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getContext(), DozeSettingsActivity.class);
                startActivity(intent);
                return true;
            }
        });

        mOTG = (SwitchPreference) findPreference(PREF_OTG);
        mOTG.setChecked(mPrefs.getBoolean(PREF_OTG, false));
        mOTG.setOnPreferenceChangeListener(this);

/*        PreferenceCategory vib_strength = (PreferenceCategory) getPreferenceScreen()
                 .findPreference("vib_strength_category");
        mVibratorStrength = (VibratorStrengthPreference) findPreference(VibratorStrengthPreference.KEY_VIBSTRENGTH);
        if (!VibratorStrengthPreference.isSupported()) {
            getPreferenceScreen().removePreference(vib_strength);
        }
*/
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (preference == mOTG) {
            mPrefs.edit()
                    .putBoolean(PREF_OTG, (Boolean) newValue).commit();
            enableOTG((Boolean) newValue);
            return true;
        }
        return true;
    }

    public static void enableOTG(boolean enable) {
            if (Utils.fileExists(OTG_PATH)) {
                Utils.writeLine(OTG_PATH, enable ? "1" : "0");
            }
    }
}
