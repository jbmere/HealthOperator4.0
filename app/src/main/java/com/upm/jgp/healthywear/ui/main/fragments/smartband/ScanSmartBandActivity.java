package com.upm.jgp.healthywear.ui.main.fragments.smartband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothUtils;
import com.orhanobut.logger.LogLevel;
import com.orhanobut.logger.Logger;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;
import com.upm.jgp.healthywear.ui.main.activity.TabWearablesActivity;
import com.upm.jgp.healthywear.ui.main.adapter.BleScanViewAdapter;
import com.upm.jgp.healthywear.ui.main.adapter.CustomLogAdapter;
import com.upm.jgp.healthywear.ui.main.adapter.DeviceCompare;
import com.upm.jgp.healthywear.ui.main.adapter.DividerItemDecoration;
import com.upm.jgp.healthywear.ui.main.adapter.OnRecycleViewClickCallback;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IABleConnectStatusListener;
import com.veepoo.protocol.listener.base.IABluetoothStateListener;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.util.VPLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Activity to scan SmartBand devices (H-Band) and connect to a selected one.
 *
 * It also handles the reconnection of the SmartBand for better stability
 *
 * Based on MainActivity class of VPBluetoothSDKDemo by timaimee on 2017/2/8.
 * @author Modified by Jorge Garcia Paredes (yoryidan)
 * @version 175
 * @since 2020
 */
public class ScanSmartBandActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, OnRecycleViewClickCallback {
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String YOUR_APPLICATION = "jgp";
    private final int REQUEST_CODE = 1;
    final private String local_device_type = "SmartBand";    //SmartBand

    Context mContext = this;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private BleScanViewAdapter bleConnectAdapter;

    private List<SearchResult> mListData = new ArrayList<>();
    private List<String> mListAddress = new ArrayList<>();

    private BluetoothManager mBManager;
    private BluetoothAdapter mBAdapter;
    private BluetoothLeScanner mBScanner;
    final static int MY_PERMISSIONS_REQUEST_BLUETOOTH = 0x55;
    VPOperateManager mVpoperateManager;
    private boolean mIsOadModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_smartband);

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

        initLog();
        Logger.t(TAG).i("onSearchStarted");

        mVpoperateManager = mVpoperateManager.getMangerInstance(mContext);
        VPLogger.setDebug(true);
        initRecyleView();   //Init swiperefreshlayout
        checkPermission();
        registerBluetoothStateListener();

        if(checkBLE())
            scanDevice();
    }

    /**
     * Listen for the callback status between Bluetooth and the device
     */
    private final IABluetoothStateListener mBluetoothStateListener = new IABluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            Logger.t(TAG).i("open=" + openOrClosed);
        }
    };

    /**
     * Monitor the callback status of the system Bluetooth on and off
     */
    private final IABleConnectStatusListener mBleConnectStatusListener = new IABleConnectStatusListener() {

        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                Logger.t(TAG).i("STATUS_CONNECTED");
            } else if (status == Constants.STATUS_DISCONNECTED) {
                Logger.t(TAG).i("STATUS_DISCONNECTED");

                if(MainActivity.isSmartbandConnected()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Device disconnected", Toast.LENGTH_SHORT).show();
                        }
                    });
                    try {
                        sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SmartBandSetupActivityFragment.deviceDisconnected();
                    MainActivity.setSmartbandConnected(false);

                    reconnectDevice(mac);
                }
            }
        }
    };

    /**
     * Scan callback
     */
    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            Logger.t(TAG).i("onSearchStarted");
        }

        @Override
        public void onDeviceFounded(final SearchResult device) {
            Logger.t(TAG).i(String.format("device for %s-%s-%d", device.getName(), device.getAddress(), device.rssi));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!mListAddress.contains(device.getAddress())) {
                        mListData.add(device);
                        mListAddress.add(device.getAddress());
                    }
                    Collections.sort(mListData, new DeviceCompare());
                    bleConnectAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onSearchStopped() {
            refreshStop();
            Logger.t(TAG).i("onSearchStopped");
        }

        @Override
        public void onSearchCanceled() {
            refreshStop();
            Logger.t(TAG).i("onSearchCanceled");
        }
    };

    /**
     * End refresh
     */
    void refreshStop() {
        Logger.t(TAG).i("refreshComplete");
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * Init logs
     */
    private void initLog() {
        Logger.init(YOUR_APPLICATION)
                .methodCount(0)
                .methodOffset(0)
                .hideThreadInfo()
                .logLevel(LogLevel.FULL)
                .logAdapter(new CustomLogAdapter());
    }

    /**
     * Detect if Bluetooth device is on
     *
     * @return boolean
     */
    private boolean checkBLE() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean bool;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE);
            bool = false;
        } else {
            bool = true;
        }
        return bool;
    }

    private void initRecyleView() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) super.findViewById(R.id.main_swipeRefreshLayout);
        mRecyclerView = (RecyclerView) super.findViewById(R.id.main_recyclerlist);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bleConnectAdapter = new BleScanViewAdapter(this, mListData);
        mRecyclerView.setAdapter(bleConnectAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST));
        bleConnectAdapter.setBleItemOnclick(this);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    private void checkPermission() {
        Logger.t(TAG).i("Build.VERSION.SDK_INT =" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT <= 22) {
            initBLE();
            return;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Logger.t(TAG).i("checkPermission,PERMISSION_GRANTED");
            initBLE();
        } else if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            requestPermission();
            Logger.t(TAG).i("checkPermission,PERMISSION_DENIED");
        }
    }

    private void initBLE() {
        mBManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (null != mBManager) {
            mBAdapter = mBManager.getAdapter();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBScanner = mBAdapter.getBluetoothLeScanner();
        }
        checkBLE();

    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale");

            } else {
                Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale else");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_BLUETOOTH);
            }
        } else {
            Logger.t(TAG).i("requestPermission,shouldShowRequestPermissionRationale hehe");
        }
    }

    /**
     * Bluetooth on or off
     */
    private void registerBluetoothStateListener() {
        mVpoperateManager.registerBluetoothStateListener(mBluetoothStateListener);
    }

    /**
     * Scan for BLE devices
     * @return boolean
     */
    private boolean scanDevice() {
        if (!mListAddress.isEmpty()) {
            mListAddress.clear();
        }
        if (!mListData.isEmpty()) {
            mListData.clear();
            bleConnectAdapter.notifyDataSetChanged();
        }

        if (!BluetoothUtils.isBluetoothEnabled()) {
            Toast.makeText(mContext, "Bluetooth is not turn on", Toast.LENGTH_SHORT).show();
            return true;
        }
        mVpoperateManager.startScanDevice(mSearchResponse);
        return false;
    }

    /**
     * This function connects the smartphone with the selected smartband.
     * It also handles the reconnection of the smartband.
     * */
    private void connectDevice(final String mac) {

        mVpoperateManager.registerConnectStatusListener(mac, mBleConnectStatusListener);

        mVpoperateManager.connectDevice(mac, new IConnectResponse() {

            @Override
            public void connectState(int code, BleGattProfile profile, boolean isoadModel) {
                if (code == Code.REQUEST_SUCCESS) {
                    //Bluetooth connection status with the device
                    Logger.t(TAG).i("CONNECTION SUCCEED!");
                    //Logger.t(TAG).i("Whether it is firmware upgrade mode?=" + isoadModel);
                    mIsOadModel = isoadModel;
                } else {
                    Logger.t(TAG).i("CONNECTION FAILED");
                    reconnectDevice(mac);
                }
            }
        }, new INotifyResponse() {
            @Override
            public void notifyState(int state) {
                if (state == Code.REQUEST_SUCCESS) {
                    //Bluetooth connection status with the device
                    Logger.t(TAG).i("Listener set up. Ready for operations!");
                    MainActivity.setSmartbandConnected(true);   //Set SmartBand connected
                    MainActivity.setSmartband_mac_global(mac);  //Set device's mac

                    if(!MainActivity.isReconnectingSmartband()) {
                        Intent intent = new Intent(mContext, TabWearablesActivity.class);
                        intent.putExtra(TabWearablesActivity.DEVICE_TYPE, local_device_type);
                        intent.putExtra("isoadmodel", mIsOadModel);
                        intent.putExtra("deviceaddress", mac);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        if(MainActivity.isMmrConnected()){
                            TabWearablesActivity.refreshTabs(0);
                        }
                    }else{
                        //If we come from reconnecting, activity is already created, so it is necessary to just start service again, but this gives a PwdData{mStatus=CHECK_FAIL and does not read any data from the smartband
                        //PwdData{mStatus=CHECK_FAIL error was solved by checking the password after reconnection with the following function
                        SmartBandSetupActivityFragment.initializationOfTheConnectionWithSmartband();
                        //TabWearablesActivity.refreshTabs(0);
                        //SmartBandSetupActivityFragment.deviceReconnected();
                    }
                } else {
                    Logger.t(TAG).i("Listener failed. Reconnect!");
                }

            }
        });
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.activity_scan_devices_scan_button:
                if(checkBLE())
                    scanDevice();
                break;
        }
    }

    /**
     *  Tries to reconnect the device every x time (current 1 minute)
     *  It is called every time the connection state gives FAILED
     */
    private void reconnectDevice(final String mac){
        MainActivity.setReconnectingSmartband(true);

        if(!MainActivity.isSmartbandConnected()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Reconnecting...", Toast.LENGTH_SHORT).show();
                }
            });
            SmartBandSetupActivityFragment.drawingReconnectingLayout();
            connectDevice(mac);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (BluetoothUtils.isBluetoothEnabled()) {
                scanDevice();
            } else {
                refreshStop();
            }
        }

    }

    @Override
    public void onRefresh() {
        Logger.t(TAG).i("onRefresh");
        if (checkBLE()) {
            scanDevice();
        }
    }

    @Override
    public void OnRecycleViewClick(int position) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, "Connecting ...",Toast.LENGTH_SHORT).show();
            }
        });
        connectDevice(mListData.get(position).getAddress());

    }

    @Override
    public void onBackPressed() {
        refreshStop();
        super.onBackPressed();
    }
}
