package com.accedo.unlockathome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.View;
import android.widget.CheckedTextView;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
// import android.widget.Toast;
// import android.util.Log;


public class UnlockAtHome extends ListActivity implements Eula.OnEulaAgreedTo
{
    final String TAG = getClass().getName();
    final String TAG_LOCK = getClass().getName()+"_ACCEPT_LOCK";
    String[] wifiArray;
    WiFiConnectionReceiver wifiConnection = new WiFiConnectionReceiver();
    DialogInterface.OnClickListener acceptInterface = 
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // void
            }
        };

    /** Called when the activity is first created. */
    @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            if(Eula.show(this)) {
                onEulaAgreedTo();
            }
        }


    @Override
        public void onDestroy()
        {
            super.onDestroy();
        }

    public void onStop()
    {
        super.onStop();
    }


    public void onEulaAgreedTo()
    {
        if(!_wifiIsActive()) {
            _notifyExit(getString(R.string.msg_wifi));
        } else if(!_acceptedLock() && !_lockIsActive()) {
            _notifyExit(getString(R.string.msg_lock));
        } else {
            if(!_acceptedLock()) {
                _acceptLock();
            }
            setContentView(R.layout.main);
            _addConfiguredNetworks();
            _intentService();
        }
    }


    protected void onListItemClick(ListView l, View v, int pos, long id)
    {
        CheckedTextView textView = (CheckedTextView) v;
        textView.setChecked(!textView.isChecked());
        _saveConfiguredNetworks();
        _intentService();
    }


    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item)
    {
        AlertDialog.Builder builder;

        if(item.getTitle().equals("Info")) {
            builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.msg_info)
                .setCancelable(false)
                .setTitle(R.string.info)
                .setNeutralButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                int id) {
                                // @todo evento?
                            }
                        });
            builder.create().show();
        }

        if(item.getTitle().equals("Preferences")) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }

        if(item.getTitle().equals("Quit")) {
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                int id) {
                                UnlockAtHome.this.finish();
                            }
                        })
            .setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();
        }
        return true;
    }


    private void _intentService()
    {
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


    private void _addConfiguredNetworks()
    {
        WifiManager wifi_manager;

        wifi_manager= (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi_manager.getConnectionInfo();
        List<WifiConfiguration> configs =
            wifi_manager.getConfiguredNetworks();
        List<String> wifi_configs = new ArrayList<String>();

        // Add configured networks
        for (WifiConfiguration config : configs) {
            String SSID = config.SSID;
            SSID = SSID.replaceAll("^\"", "");
            SSID = SSID.replaceAll("\"$", "");
            SSID = SSID + " [" + config.networkId + "]";
            wifi_configs.add(SSID);
            // Log.i(TAG, config.toString());
        }
        wifiArray = new String[wifi_configs.size()];
        wifi_configs.toArray(wifiArray);
        setListAdapter(new ArrayAdapter<String>(this, 
                    android.R.layout.simple_list_item_checked,
                    wifiArray));

        // Prepare ListView
        ListView lv = getListView();
        lv.setChoiceMode(lv.CHOICE_MODE_MULTIPLE);

        // Check already saved networks on list
        SharedPreferences sharedPreferences = getSharedPreferences(TAG, 0);
        Map<String, ?> sharedPreferencesMap = sharedPreferences.getAll();
        for(String key : sharedPreferencesMap.keySet()) {
            int i = 0;
            for(String network : wifiArray) {
                // Log.i(TAG, "key:" + key + " network:" + network);
                if(network.equals(key)) {
                    lv.setItemChecked(i, true);
                    // Log.i(TAG, "Checked");
                    break;
                }
                i++;
            }
        }
    }


    private void _saveConfiguredNetworks()
    {
        SharedPreferences settings = getSharedPreferences(TAG, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        ListView lv = getListView();
        for(int i = 0; i < wifiArray.length; i++) {
            if(lv.isItemChecked(i)) {
                editor.putBoolean(wifiArray[(int) i], true);
            }
        }
        editor.commit();
    }


    private boolean _wifiIsActive()
    {
        WifiManager wifi_manager;

        wifi_manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int wifi_state = wifi_manager.getWifiState();
        return
            wifi_state == android.net.wifi.WifiManager.WIFI_STATE_ENABLED ||
            wifi_state == android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
    }


    private boolean _lockIsActive()
    {
        int ret = Settings.Secure.getInt(getContentResolver(), 
                Settings.Secure.LOCK_PATTERN_ENABLED, 0);
        return ret == 1 ? true : false;
    }


    private void _notifyExit(String msg)
    {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
            .setCancelable(false)
            .setTitle(R.string.info)
            .setNeutralButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                            int id) {
                            UnlockAtHome.this.finish();
                        }
                    });
        builder.create().show();
    }


    private void _accept(String msg, DialogInterface.OnClickListener 
            accept_interface)
    {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("I accept", accept_interface)
        .setNegativeButton("No!",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            UnlockAtHome.this.finish();
                    }
                });
        builder.create().show();
    }


    private boolean _acceptedLock()
    {
        SharedPreferences sharedPreferences =
            getSharedPreferences(TAG_LOCK, 0);
        return sharedPreferences.getBoolean("acceptLock", false);
    }


    private boolean _acceptLock()
    {
        DialogInterface.OnClickListener acceptInterface = 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int id) {
                        SharedPreferences sharedPreferences =
                            getSharedPreferences(TAG_LOCK, 0);
                        SharedPreferences.Editor editor =
                            sharedPreferences.edit(); 
                        editor.putBoolean("acceptLock", true);
                        editor.commit();
                        return;
                    }
                };

        SharedPreferences sharedPreferences =
            getSharedPreferences(TAG_LOCK, 0);
        boolean acceptedLock = sharedPreferences.getBoolean("acceptLock",
                false);
        if(!acceptedLock) {
            _accept(getString(R.string.msg_networks), acceptInterface);
        }
        return acceptedLock;
    }
}
