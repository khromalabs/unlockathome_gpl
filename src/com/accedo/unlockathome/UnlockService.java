package com.accedo.unlockathome;

import java.lang.System;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.RemoteViews;
import java.util.List;
import java.util.Map;


public class UnlockService extends Service
{
    String TAG = getClass().getName();
	private Notification mNotification;

    @Override
        public IBinder onBind(Intent intent) 
        {
            return null;
        }

    @Override
        public void onCreate() 
        {
            super.onCreate();
            //Log.d(TAG, "UnlockService Created");
        }

    @Override
        public void onDestroy() 
        {
            super.onDestroy();
            //Log.d(TAG, "UnlockService Destroyed");
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_PATTERN_ENABLED, 1);
        }


    @Override
        public void onStart(Intent intent, int startId) 
        {
            super.onStart(intent, startId);
            //Log.d(TAG, "UnlockService Started");
            updateLockState(intent);
            stopSelf(0);
        }


    private void updateLockState(Intent i)
    {
        String clase = getClass().getPackage().getName() + ".UnlockAtHome";
        SharedPreferences settings = getSharedPreferences(clase, 1);
        Map<String, ?> settingsMap = settings.getAll();
        if(settingsMap.size() < 1) {
            // Require at least one configured net
            return;
        }

        NetworkInfo info =
            (NetworkInfo) i.getParcelableExtra(
                    android.net.ConnectivityManager.EXTRA_NETWORK_INFO);

        if(info != null && info.getType() == 
                android.net.ConnectivityManager.TYPE_WIFI) {

            WifiManager wifi =
                (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifi_info = wifi.getConnectionInfo();
            String Current_SSID = wifi_info.getSSID() + " [" + 
                wifi_info.getNetworkId() + "]";

            if(info.isConnected()) {
                List<WifiConfiguration> networks =
                    wifi.getConfiguredNetworks();
                for(WifiConfiguration network : networks) {
                    String SSID = network.SSID;
                    SSID = SSID.replaceAll("^\"", "");
                    SSID = SSID.replaceAll("\"$", "");
                    SSID = SSID + " [" + network.networkId + "]";

                    if(SSID.equals(Current_SSID)) {
                        for(String key : settingsMap.keySet()) {
                            if(SSID.equals(key) && 
                                    settings.getBoolean(key, false)) {
                                // Log.d(TAG, "S '"+SSID+"' K '"+key+"'");
                                setLockPattern(false);
                                return;
                                    }
                        }
                    }
                }
            }
                }

        setLockPattern(true);
    }


    private void setLockPattern(boolean pattern_status_boolean)
    {

        int pattern_status = pattern_status_boolean ? 1 : 0;
        
        // @todo Obtener informacion de si ya estaba activo
        //Log.d(TAG, pattern_status ? "lock true" : "lock false");
        int last_pattern_status =
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.LOCK_PATTERN_ENABLED, 1);

        if(last_pattern_status != pattern_status) {
            showNotification(pattern_status_boolean);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_PATTERN_ENABLED, 
                    pattern_status);

            SharedPreferences settings = 
                PreferenceManager.getDefaultSharedPreferences(this);
            boolean nosleep = settings.getBoolean("wifiSuspension", false);
            if(nosleep) {
                if(pattern_status != 0) {
                    // Log.d(TAG, "sleep default");
                    int alternate_state = Integer.parseInt(
                            settings.getString("alternateState",
                                "0"));
                    Log.d(TAG, "alternate state: "+alternate_state);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.WIFI_SLEEP_POLICY, 
                            alternate_state);
                } else {
                    // Log.d(TAG, "sleep never");
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.WIFI_SLEEP_POLICY, 
                            Settings.System.WIFI_SLEEP_POLICY_NEVER);
                }
            }

        } else {
            showNotification(pattern_status_boolean);
        }
    }


    private void showNotification(boolean locked)
    {
        boolean inverse = false;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        // String clase = getClass().getPackage().getName() + ".Preferences";
        // SharedPreferences settings = getSharedPreferences(clase, 1);
        boolean visible = settings.getBoolean("iconStatusBar", true);

        Notification notification = mNotification;

		if (notification == null) {
            notification = new Notification();
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            RemoteViews view = notification.contentView = 
                new RemoteViews(getPackageName(), R.layout.status_bar);
            int color = inverse ? Color.WHITE : Color.BLACK;
            view.setTextColor(R.id.text1, color);
            view.setTextColor(R.id.text2, color);
            Intent notificationIntent = new Intent();
            notificationIntent.setClassName("com.accedo.unlockathome",
                    "com.accedo.unlockathome.UnlockAtHome");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notification.contentIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
            // PendingIntent pendingIntent =
            // PendingIntent.getActivity(this, 0, notificationIntent, 0);
            // CharSequence contentTitle = "UnlockAtHome";
            // CharSequence contentText = 
            // "Tap to modify networks lock configuration";
            // notification.setLatestEventInfo(this, contentTitle,
            // contentText, pendingIntent);
            mNotification = notification;
        }

        NotificationManager notificationManager =
            (NotificationManager) 
            getSystemService(Context.NOTIFICATION_SERVICE);
        notification.icon = visible ? (locked ? R.drawable.locker_closed :
            R.drawable.locker_opened) : -1;
        notification.when = visible ? System.currentTimeMillis() :
            Long.MAX_VALUE;
        notificationManager.notify(12345, notification);
        // notificationManager.cancel(12345);
    }
};
