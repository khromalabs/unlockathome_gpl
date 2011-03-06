package com.accedo.unlockathome;

import com.accedo.unlockathome.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.preference.CheckBoxPreference;

public class Preferences extends PreferenceActivity {
    Preference wifiSuspension;

    public OnPreferenceClickListener overrider = 
        new OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference pref) {
            if(pref == wifiSuspension) {
                Preference alternate_state = 
                    findPreference("alternateState");
                CheckBoxPreference checkbox_pref = (CheckBoxPreference) pref;
                alternate_state.setEnabled(checkbox_pref.isChecked());
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.preferences);

        wifiSuspension = findPreference("wifiSuspension");
        wifiSuspension.setOnPreferenceClickListener(overrider);
        CheckBoxPreference checkbox_pref = 
            (CheckBoxPreference) wifiSuspension;
        if(checkbox_pref.isChecked()) {
            Preference alternate_state = findPreference("alternateState");
            alternate_state.setEnabled(true);
        }
    }

    protected void onStop()
    {
        super.onStop();
        Intent intent = new Intent(this, UnlockService.class);
        ConnectivityManager cm = 
            (ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info != null) {
            intent.putExtra(ConnectivityManager.EXTRA_NETWORK_INFO,
                    info);
        }
        startService(intent);
    }
}

