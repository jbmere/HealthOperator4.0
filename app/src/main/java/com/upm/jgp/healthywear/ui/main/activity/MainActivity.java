package com.upm.jgp.healthywear.ui.main.activity;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.DataModule.FavouriteObject;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MyService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import bolts.Task;

//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    Context mContext = MainActivity.this;
    TextView mAppVersion;
    FloatingActionButton helpFAB;
    PopupWindow popupInfo = null;
    ImageButton closeButton;
    private static Map<String, FavouriteObject> favouritesMap = new HashMap<>();    //TODO use the map to store the favourite devices
    private static String favFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp";;
    private static String favDevicesFileName = favFilePath + "favouriteWearables.txt";

    public static BtleService.LocalBinder serviceBinder;
    private static boolean reconnectingSmartband = false;

    // Variables for requesting permissions, API 25+
    final int requestCode_storage=1;
    final int requestCode_gps=2;
    //private int[] grantResults_stg = null;
    //private int[] grantResults_gps = null;

    //create more variables if more devices are implemented
    //TODO change the value of these variables to true when the devices are connected
    private static boolean smartbandConnected = false;
    private static boolean mmrConnected = false;
    //TODO change the value of these variables to false when the devices are disconnected (those buttons are not yet implemented)
    /////Control variables to know state of devices/////

    private static String smartband_mac_global = null;
    private static BluetoothDevice mmr_device_global = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

        //Appends the app version to the TextView in main_activity
        mAppVersion = (TextView) super.findViewById(R.id.activity_main_versionTextView);
        mAppVersion.append(getAppVersion(mContext));

        //Creates the Float Action Button with help information
        helpFAB = findViewById(R.id.activity_main_infoFAB);
        helpFAB.setOnClickListener(activity_main_infoFAB_listener);

        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

        //check if the app has write_storage and access_Location permissions
        checkPermissions();

        //TODO read favourite devices from list, if the file exists
        favouritesMap = readFavouriteDevices();

        //TODO initiate somewhere common to all devices (Main or choose scan)
        //start MyService **background service**
        Intent startIntent = new Intent(this, MyService.class);
        startService(startIntent);
        System.out.println("Starting Uploading Service...");
    }

    @Override
    public void onBackPressed() {
        if (popupInfo != null && popupInfo.isShowing()) {
            popupInfo.dismiss();
            popupInfo=null;
        } else {
            super.onBackPressed();
        }
    }


    //https://stackoverflow.com/questions/44455887/permission-denied-on-writing-to-external-storage-despite-permission
    @Override // android recommended class to handle permissions
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(grantResults != null) {
            switch (requestCode) {
                case 1: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("permission", "granted");
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.uujm
                        Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                        //app cannot function without this permission for now so close it...
                        //onDestroy();
                    }
                    break;
                }
                case 2: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("permission", "granted");
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.uujm
                        Toast.makeText(MainActivity.this, "Permission denied to get location", Toast.LENGTH_SHORT).show();
                        //app cannot function without this permission for now so close it...
                        //onDestroy();
                    }
                    break;
                }
                // other 'case' line to check for other
                // permissions this app might request
            }
        }
    }

    /**
     * Private method that initializes the activity's view
     * */
    private void initMainActivityView() {
        //TODO add here the onCreate-code fot this part
    }

    /**
     * Check APP's permissions
     * */
    private void checkPermissions() {
        //TODO check messages
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //Lo que tengas que hacer con el STORAGE
            Log.d("Storage Permission", "granted");
        }else{
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Write in external storage permission")
                        .setMessage("This app needs to be able to write data on storage when the phone does not have a proper connection to send the data to the DataBase.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode_storage);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        requestCode_storage);
            }
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Lo que tengas que hacer con el Loaction
            Log.d("Location Permission", "granted");
        }else{
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Access to device location permission")
                        .setMessage("This App needs to access to the location of the device for its proper function")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode_gps);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        requestCode_gps);
            }
        }

    }

    /*      OnClickListeners      */

    /**
     * PopUp Window of the Help button on the main screen
     */
    private View.OnClickListener activity_main_infoFAB_listener = new View.OnClickListener() {
        public void onClick(View view) {
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show();

            //How to inflate a Popup Window -> https://android--code.blogspot.com/2016/01/android-popup-window-example.html
            // Initialize a new instance of LayoutInflater service
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

            // Inflate the custom layout/view
            View customView = inflater.inflate(R.layout.activity_main_info_popup, null);

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
            popupInfo.showAtLocation(view, Gravity.CENTER,0,0);

        }
    };

    private View.OnClickListener closeButton_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Dismiss the popup window
            popupInfo.dismiss();
        }
    };

    /**
     * Returns the app version if it exists
    * */
    public static String getAppVersion(Context context) {
        String version = "";
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return version;
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.activity_main_scan_button:
                Intent intent = new Intent(MainActivity.this, ChooseDeviceToScanActivity.class);
                startActivity(intent);
                break;
            case R.id.activity_main_fav_button:
                //Favourite devices for test
                //FavouriteObject fav1 = new FavouriteObject("1234", "smart3", 1);
                //FavouriteObject fav2= new FavouriteObject("5678", "mmr5", 2);
                //If there is no mac address on the file
                if(favouritesMap != null) {
                    //if map is not null and is not empty, go to Favs Activity and show the list to connect
                    if (!favouritesMap.isEmpty()){
                        //deleteFavouriteDevice(fav1.getmWearableMac());
                        //deleteFavouriteDevice(fav2);
                        //putFavouriteDevice(fav1.getmWearableMac(), fav1.getmWearableType());

                        Intent intent2 = new Intent(MainActivity.this, FavouriteDevices.class);
                        startActivity(intent2);
                    }else{
                        //putFavouriteDevice(fav1.getmWearableMac(), fav1.getmWearableType());
                        //putFavouriteDevice(fav2);
                        Toast.makeText(MainActivity.this, "There are no Favourite devices stored", Toast.LENGTH_SHORT).show();
                    }
                }
                //Else show toast
                else {
                    Toast.makeText(MainActivity.this, "There is no favs file", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //TODO create when MyService is working
    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void finish(){
        //*********Stop MyService_mmr***
        Intent stopIntent = new Intent(this, MyService.class);
        stopService(stopIntent);
        //System.out.println("service is stopped!");
        System.exit(0);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync().continueWithTask(task -> task.isFaulted() ? reconnect(board) : task);
    }

    //sets a new fav object in the map and saves the map on file
    public static boolean putFavouriteDevice(String mac, int type){
        boolean correct;

        //System.out.println("put");
        FavouriteObject fav = new FavouriteObject(mac, type);

        if(favouritesMap.put(fav.getmWearableMac(), fav)==null) {
            //System.out.println(" saving");
            correct = saveFavouriteDevices();
        }else {
            correct = false;    //If the object couldnt be put on the map
        }
        return correct;
    }

    //deletes a fav object from the map and saves the map on file
    public static boolean deleteFavouriteDevice(String mac){
        boolean correct;

        //System.out.println("deleted");
        if(favouritesMap.remove(mac)==null) {
            correct = false;    //If the object couldnt be found on the map
        }else {
            //System.out.println(" saving");
            correct = saveFavouriteDevices();
        }
        return correct;
    }

    //Get the list of favourite devices, otherwise return null
    private Map<String, FavouriteObject> readFavouriteDevices() {
        Map<String, FavouriteObject> map = new HashMap<>();

        try {
            FileInputStream fis = new FileInputStream(new File(favDevicesFileName));
            ObjectInputStream ois = new ObjectInputStream(fis);

            // Read objects
            map = (HashMap) ois.readObject();

            System.out.println("readFavouriteDevices: Favourites MAP read from file:");
            System.out.println(map);

            ois.close();
            fis.close();

        } catch (FileNotFoundException e) {
            System.out.println("readFavouriteDevices: File not found");
        } catch (IOException e) {
            System.out.println("readFavouriteDevices: Error initializing stream");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return map;
    }

    public static boolean saveFavouriteDevices(){
        boolean correct = true;

        try {
            FileOutputStream fos = new FileOutputStream(new File(favDevicesFileName));
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            // Write objects to file
            oos.writeObject(favouritesMap);

            //System.out.println("saveFavouriteDevices: Favourites Map saved on File");

            oos.close();
            fos.close();

        } catch (FileNotFoundException e) {
            System.out.println("saveFavouriteDevices: File not found");
            correct = false;
        } catch (IOException e) {
            System.out.println("saveFavouriteDevices: Error initializing stream");
            correct = false;
        }
        return correct;
    }

    public static boolean checkFavouriteDevice(String mac){

        boolean deviceIsFav = favouritesMap.containsKey(mac);
        //System.out.println("Device: " + mac + " is fav:" + deviceIsFav);
        //System.out.println(favouritesMap);

        return deviceIsFav;
    }

    //////Getters and Setters//////
    public static Map<String, FavouriteObject> getFavouritesMap() {
        return favouritesMap;
    }

    /////Control variables to know state of devices/////
    public static boolean isSmartbandConnected() {
        return smartbandConnected;
    }

    public static void setSmartbandConnected(boolean smartbandConnected_input) {
        smartbandConnected = smartbandConnected_input;
    }

    public static boolean isMmrConnected() {
        return mmrConnected;
    }

    public static void setMmrConnected(boolean mmrConnected_input) {
        mmrConnected = mmrConnected_input;
    }

    public static String getSmartband_mac_global() {
        return smartband_mac_global;
    }

    public static void setSmartband_mac_global(String smartband_mac_global) {
        MainActivity.smartband_mac_global = smartband_mac_global;
    }

    public static BluetoothDevice getMmr_device_global() {
        return mmr_device_global;
    }

    public static void setMmr_device_global(BluetoothDevice mmr_device_global) {
        MainActivity.mmr_device_global = mmr_device_global;
    }

    public static boolean isReconnectingSmartband() {
        return reconnectingSmartband;
    }

    public static void setReconnectingSmartband(boolean reconnectingSmartband) {
        MainActivity.reconnectingSmartband = reconnectingSmartband;
    }
}