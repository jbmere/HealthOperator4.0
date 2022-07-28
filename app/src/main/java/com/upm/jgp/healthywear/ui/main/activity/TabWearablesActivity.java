package com.upm.jgp.healthywear.ui.main.activity;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MMR2SetupActivityFragment;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MMRSetupActivityFragment;
import com.upm.jgp.healthywear.ui.main.fragments.smartband.SmartBandSetupActivityFragment;
import com.upm.jgp.healthywear.ui.main.fragments.tabs.SectionsTabsAdapter;

/**
 * This Activity contains tabs with the various information from the connected devices
 * <p>
 * It contains multiple fragments for each of the device types. Currently it is possible to connect an SmartBand and an MMR device at the same time with fine stability.
 * Each of the devices can be connected, disconnected, added or deleted from favourites devices list (FAB button) or ask for information about the device.
 * Each fragmetn shows real-time information about the device and the communication with the DataBase can be started or stopped (Start stop buttons)
 *
 * @author Jorge Garcia Paredes (yoryidan)
 * Modified by Raquel Prous 2022
 * @version 210
 * @since 2020
 */
public class TabWearablesActivity extends AppCompatActivity implements ServiceConnection, MMRSetupActivityFragment.FragmentSettings, MMR2SetupActivityFragment.FragmentSettings2 {
    public static final String DEVICE_TYPE = "incoming_device_type";
    Context mContext = this;
    PopupWindow popupInfo = null;
    ImageButton closeButton;
    private String incoming_device_type;
    private TabLayout tabs;

    /////SmartBand App/////
    private static String mac_address_smartBand;

    //Location
    private static LocationManager locationManager;

    /////MMRData App/////
    public final static String EXTRA_BT_DEVICE = "com.mbientlab.metawear_mmr.starter.DeviceSetupActivity.EXTRA_BT_DEVICE";
    public static String mac_address_mmr;

    SectionsTabsAdapter sectionsTabsAdapter;
    public static ViewPager viewPager;

    //Refresh Tabs//
    private static boolean refreshingTabs;
    private static int currentRefreshingTab;
    //Refresh Tabs//

    private BluetoothDevice btDevice_mmr;
    private MetaWearBoard metawear_mmr;
    /////MMRData App/////
    public final static String EXTRA_BT_DEVICE2 = "com.mbientlab.metawear_mmr.starter.DeviceSetupActivity.EXTRA_BT_DEVICE2";
    private BluetoothDevice btDevice_mmr2;
    private MetaWearBoard metawear_mmr2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_wearables);

        //////Toolbar Settings//////
        Toolbar toolbar = (Toolbar) findViewById(R.id.myToolbar_wearables);
        toolbar.setTitle(getString(R.string.app_name)); //setting the title
/*
        setSupportActionBar(toolbar);   //placing toolbar in place of actionbar

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(
                        "CLickkkk"
                );
                onBackPressed();
            }
        });
*/

        setSupportActionBar(toolbar);   //placing toolbar in place of actionbar
        //////Toolbar Settings//////

        sectionsTabsAdapter = new SectionsTabsAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsTabsAdapter);
        tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        incoming_device_type = getIntent().getStringExtra(DEVICE_TYPE);
        //checkFavourites();

        /////SmartBand App/////
        if (incoming_device_type.equals("SmartBand")) {
            //if(MainActivity.isSmartbandConnected()) {
            mac_address_smartBand = getIntent().getStringExtra("deviceaddress");
            System.out.println("ADDRESS: " + mac_address_smartBand);
            //}
        }
        /////SmartBand App/////

        /////MMRData App/////
        if (incoming_device_type.equals("MMR")) {
            //if(MainActivity.isMmrConnected()) {
            btDevice_mmr = getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
            getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);
            //}
            //Sets the second tab on the UI
            viewPager.setCurrentItem(1);
        }
        /////MMRData App/////

        /////MMR2Data App/////
        if (incoming_device_type.equals("MMR2")) {
            //if(MainActivity.isMmrConnected()) {
            btDevice_mmr2 = getIntent().getParcelableExtra(EXTRA_BT_DEVICE2);
            getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);
            //}
            //Sets the second tab on the UI
            viewPager.setCurrentItem(2);
        }
        /////MMRData App/////

        //TODO add more kind of wearables

        //Load device's Location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    /////MENU/////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_wearables, menu);
        return true;
    }

    @Override
    /**
     * This function has the menu options from the activity. This menu works different depending of the current tab.
     * It shows help information about the device, the possibility to scan a new device or the disconnection of the curretn device.
     * */
    public boolean onOptionsItemSelected(MenuItem item) {

        int selectedTab = tabs.getSelectedTabPosition();
        switch (item.getItemId()) {
            case R.id.menuBackToScan:
                Intent intent = new Intent(this, ChooseDeviceToScanActivity.class);
                startActivity(intent);
                break;

            case R.id.menuDisconnectDevice:
                //Code to disconnect current device
                switch (selectedTab) {
                    case 0: //SmartBand
                        if (MainActivity.isSmartbandConnected()) {
                            //Case for disconnection of smartband device
                            SmartBandSetupActivityFragment.deviceDisconnected();
                            Toast.makeText(this, "SmartBand disconnected", Toast.LENGTH_SHORT).show();
                            //Change the view of that tab to device disconnected, if there is other device still connected
                            refreshTabs(0);
                        } else {
                            Toast.makeText(this, "No SmartBand is connected", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1: //MMR
                        if (MainActivity.isMmrConnected()) {
                            //When SmartBand is connected first, then metawear_mmr is null, solved
                            if (metawear_mmr != null) {
                                metawear_mmr.disconnectAsync();
                            } else {
                                MMRSetupActivityFragment.disconnection();
                            }

                            Toast.makeText(this, "MMR device disconnected", Toast.LENGTH_SHORT).show();
                            MainActivity.setMmrConnected(false);
                            MainActivity.setMmr_device_global(null);   //Set device's MAC
                            //Change the view of that tab to device disconnected, if there is other device still connected
                            refreshTabs(1);

                        } else {
                            Toast.makeText(this, "No MMR device is connected", Toast.LENGTH_SHORT).show();
                        }
                        break;
                   case 2: //MMR2
                        if (MainActivity.isMmr2Connected()) {
                            //When SmartBand is connected first, then metawear_mmr2 is null, solved
                            if (metawear_mmr2 != null) {
                                metawear_mmr2.disconnectAsync();
                            } else {
                                MMR2SetupActivityFragment.disconnection();
                            }

                            Toast.makeText(this, "MMR2 device disconnected", Toast.LENGTH_SHORT).show();
                            MainActivity.setMmr2Connected(false);
                            MainActivity.setMmr2_device_global(null);   //Set device's MAC
                            //Change the view of that tab to device disconnected, if there is other device still connected
                            refreshTabs(2);

                        } else {
                            Toast.makeText(this, "No MMR2 device is connected", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case 3: //Others
                        //TODO case for disconnection of 3rd device
                        break;
                }

                //If there is no wearable connected... go back
                if ((!MainActivity.isSmartbandConnected()) && (!MainActivity.isMmrConnected()) && (!MainActivity.isMmr2Connected())) {
                    //Toast.makeText(this, "Nodeviceconnecteeeeed", Toast.LENGTH_SHORT).show();
                    if (popupInfo != null) {
                        popupInfo.dismiss();
                    }
                    //android.view.WindowLeaked jumps but it is not affecting behavior
                    Intent mainActivityIntent = new Intent(this, ChooseDeviceToScanActivity.class);
                    mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(mainActivityIntent);

                }
                break;

            case R.id.menuInfo:
                //Inflates a PopUpWindow with the information about the device's tab
                inflateInfoPopup(selectedTab);
                break;
        }
        return true;
    }

    /**
     * This method inflates the PopUp Window of the Help button on the main screen
     * It shows different information for each device type
     */
    private void inflateInfoPopup(int selectedTab) {
        //How to inflate a Popup Window -> https://android--code.blogspot.com/2016/01/android-popup-window-example.html
        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = null;
        //Show different info for each device type
        switch (selectedTab) {
            case 0: //SmartBand
                //Inflate the smartband layout/view
                customView = inflater.inflate(R.layout.activity_smartband_info_popup, null);
                break;
            case 1: //MMR
                //Inflate the mmr layout/view
                customView = inflater.inflate(R.layout.activity_mmr_info_popup, null);
                break;
            case 2: //MMR2
                //Inflate the mmr layout/view
                customView = inflater.inflate(R.layout.activity_mmr2_info_popup, null);
                break;
            default:
                //TODO case 2 for 3rd device type (tab 3)
                //Inflate the custom layout/view
                customView = inflater.inflate(R.layout.activity_main_info_popup, null);
                break;
        }

        // Initialize a new instance of popup window
        popupInfo = new PopupWindow(
                customView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // Get a reference for the custom view close button
        closeButton = (ImageButton) customView.findViewById(R.id.ib_close);
        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(closeButton_listener);
        //popupInfo.setFocusable(true);         //do not set to true if we want the onbackpressed button to close the popup window
        // Finally, show the popup window at the center location of root relative layout
        popupInfo.showAtLocation(customView, Gravity.CENTER, 0, 0);
    }

    private View.OnClickListener closeButton_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Dismiss the popup window
            popupInfo.dismiss();
        }
    };
    /////MENU/////

    @Override
    public void onBackPressed() {
        if (popupInfo != null && popupInfo.isShowing()) {
            popupInfo.dismiss();
            popupInfo = null;
        }
    }

    /////MMRData App/////
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        /////SmartBand App/////
        //if(incoming_device_type.equals("SmartBand")) {
        if (MainActivity.isSmartbandConnected()) {

        }
        //}
        /////SmartBand App/////

        /////MMRData App/////
        if (MainActivity.isMmrConnected()) {

        }
        /////MMRData App/////
        if (MainActivity.isMmr2Connected()) {

        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        /////SmartBand App/////
        if (!MainActivity.isSmartbandConnected()) {

        }
        /////SmartBand App/////

        /////MMRData App/////
        if (!MainActivity.isMmrConnected()) {

        }
        /////MMRData App/////
        if (!MainActivity.isMmr2Connected()) {

        }
    }

    @Override
    public BluetoothDevice getBtDevice_mmr() {
        return btDevice_mmr;
    }

    /////MMRData App/////
 //   @Override
    public BluetoothDevice getBtDevice_mmr2() {
        return btDevice_mmr2;
    }


    public static String getMac_address_smartBand() {
        return mac_address_smartBand;
    }

    public static LocationManager getLocationManager() {
        return locationManager;
    }

    /**
     * This method is used to refresh//update the UI when a second device is connected, Smartband is reconnected or devices are disconnected.
     * Currently it is working with devices are connected/disconnected on the first two tabs. It needs to refresh the Layout when a new fragment is attached.
     * This function helps by moving the tabs without user's interaction to let the fragment view be created.
     * <p>
     * Example: Having 4 Tabs/fragments the layout would be as follows   ->    | 0 | 1 | 2 | 3 |
     * When the user's view is on the Tab1, the layout is created also for the adjacent tabs (0 and 3).
     * If the user moves to Tab2, then the onDestroyView method for Tab0 is called, and also the onCreateView method for Tab4 is called.
     * So when connecting a second device in Tab1, it would be necessary to move to a Tab that is more than one tab away from Tab1 (go to Tab3) and then show Tab1 again.
     * <p>
     * To work with new devices in Tab2 or Tab3 it is not necessary to add more Tabs, but it wuld be necessary to reproduce the same behaviour (moving two tabs to the left (-2)instead of to the right (+2))
     */
    public static void refreshTabs(int tabToRefresh) {
        int currentTab = viewPager.getCurrentItem();
        //System.out.println("Current tab: " + currentTab);
        //TODO the difference of the tab number should be in absolute value for future releases, i.e. when implementing a new device in Tab3, it just have to return to Tab1 to renew the view
        int diff = currentTab - tabToRefresh;

        if (diff < 2) {
            //if the tab to refresh is not created, it is because the currentTab is 2 or more tabs away.
            setRefreshingTabs(true);
            currentRefreshingTab = tabToRefresh;
            //System.out.println("Tab to be refreshed: " + currentRefreshingTab);
            if (tabToRefresh < 2) {
                viewPager.setCurrentItem(currentRefreshingTab + 2);
            }
            if (tabToRefresh >= 2) {
                viewPager.setCurrentItem(currentRefreshingTab - 2);
            }
        } else {
            //Tab to refresh is not currently created, so we just move to that tab
            Handler uiHandler = new Handler();
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Handler");
                    TabWearablesActivity.viewPager.setCurrentItem(currentRefreshingTab, true);
                }
            });
        }
    }

    //Getters and Setters//
    public static boolean isRefreshingTabs() {
        return refreshingTabs;
    }

    public static void setRefreshingTabs(boolean value) {
        refreshingTabs = value;
    }

    public static int getCurrentRefreshingTab() {
        return currentRefreshingTab;
    }
    //Getters and Setters//

}