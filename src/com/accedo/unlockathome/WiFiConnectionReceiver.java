package com.accedo.unlockathome;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.System;
import android.provider.Settings;
import android.util.Log;
import android.os.Bundle;


public class WiFiConnectionReceiver extends BroadcastReceiver
{
    String TAG = getClass().getName();

    public WiFiConnectionReceiver()
    {
        super();
    }

    @Override
        public void onReceive(Context c, Intent i) 
        {
            // Log.d(TAG, "Action Received: " + i.getAction() +
                    // " From intent: " + i);
            // Bundle extras = i.getExtras();
            // for (String key : extras.keySet()) {
                // Log.d(TAG, key + "::" + extras.get(key));
            // }

            // obtiene los extras
            NetworkInfo info = 
                (NetworkInfo)
                i.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            Intent intent = new Intent(c, UnlockService.class);
            if(info != null) {
                intent.putExtra(ConnectivityManager.EXTRA_NETWORK_INFO,
                        info);
            }
            c.startService(intent);
        }
}
