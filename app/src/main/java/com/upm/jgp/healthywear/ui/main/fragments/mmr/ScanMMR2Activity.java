package com.upm.jgp.healthywear.ui.main.fragments.mmr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBoard;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;
import com.upm.jgp.healthywear.ui.main.activity.TabWearablesActivity;
import com.upm.jgp.healthywear.ui.main.fragments.common.MyService;

import java.util.UUID;

/**
 * Activity to scan MMR devices (mbientlab) and connect to a selected one.
 *
 * It also handles the reconnection of the MMR for better stability
 *
 * Based on MainActivity class of MetaWear-SDK-Android by mbientlab
 * @author Raquel Prous
 * @version 210
 * @since 2022
 */
public class ScanMMR2Activity extends AppCompatActivity implements BleScannerFragment.ScannerCommunicationBus {
    private static final int REQUEST_START_APP= 1;

    private MetaWearBoard metawear = null;
    final String local_device_type = "MMR2";    //MMR2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_mmr2);

        //////Toolbar Settings//////
        Toolbar toolbar = (Toolbar) findViewById(R.id.myToolbar_scan);
        toolbar.setTitle(getString(R.string.app_name)); //setting the title

        setSupportActionBar(toolbar);   //placing toolbar in place of actionbar

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        //////Toolbar Settings//////
    }

    //****** code from uploadMMRData app ***
    @Override
    protected void onResume() {
        super.onResume();
        //System.out.println("onResume...");

        //***Receive textview information from MyService***
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String UIString = intent.getStringExtra(MyService.DATA_STRING);
                        System.out.println(UIString);
                    }
                }, new IntentFilter(MyService.ACTION_UI_BROADCAST)
        );

        //***Restart the application when no data coming for a long period
        //http://blog.scriptico.com/01/how-to-restart-android-application/
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        long timegap=intent.getLongExtra("TIMEGAP", 0);
                        if (timegap>1800000){
                            //finish();
                            restartApp(300);
                        }
                    }
                }, new IntentFilter("APP_RESTART_BROADCAST")
        );

    }

    public void restartApp(int delay) {
        PendingIntent intent = PendingIntent.getActivity(this.getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags());
        AlarmManager manager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
        //System.exit(0);
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_START_APP:
                ((BleScannerFragment) getFragmentManager().findFragmentById(R.id.scanner_fragment2)).startBleScan();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        return new UUID[] {MetaWearBoard.METAWEAR_GATT_SERVICE};
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device) {
        metawear = MainActivity.serviceBinder2.getMetaWearBoard(device);

        final ProgressDialog connectDialog = new ProgressDialog(this);
        connectDialog.setTitle(getString(R.string.title_connecting));
        connectDialog.setMessage(getString(R.string.message_wait));
        connectDialog.setCancelable(false);
        connectDialog.setCanceledOnTouchOutside(false);
        connectDialog.setIndeterminate(true);
        connectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialogInterface, i) -> metawear.disconnectAsync());
        connectDialog.show();

        metawear.connectAsync().continueWithTask(task -> task.isCancelled() || !task.isFaulted() ? task : MainActivity.reconnect(metawear))
                .continueWith(task -> {
                    if (!task.isCancelled()) {
                        runOnUiThread(connectDialog::dismiss);
                        MainActivity.setMmr2Connected(true); //Sets MMR device as connected
                        MainActivity.setMmr2_device_global(device);   //Set device's MAC

                        Intent navActivityIntent = new Intent(ScanMMR2Activity.this, TabWearablesActivity.class);
                        navActivityIntent.putExtra(TabWearablesActivity.DEVICE_TYPE, local_device_type);
                        navActivityIntent.putExtra(TabWearablesActivity.EXTRA_BT_DEVICE2, device);
                        navActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(navActivityIntent, REQUEST_START_APP);
                        //If it's the second device to connect, then it is necessary to refresh the view
                        if(MainActivity.isSmartbandConnected()){
                            TabWearablesActivity.refreshTabs(2);
                        }
                        if(MainActivity.isMmrConnected()){
                            TabWearablesActivity.refreshTabs(2);
                        }
                    }
                    return null;
                });
    }
}
