package com.upm.jgp.healthywear.ui.main.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MMRSetupActivityFragment;
import com.upm.jgp.healthywear.ui.main.fragments.tabs.SectionsPagerAdapter;

import bolts.Continuation;

import static android.content.DialogInterface.BUTTON_NEGATIVE;

public class TabWearablesActivity extends AppCompatActivity implements ServiceConnection, MMRSetupActivityFragment.FragmentSettings {
    public static final String DEVICE_TYPE = "incoming_device_type";
    Context mContext = this;
    PopupWindow popupInfo = null;
    ImageButton closeButton;
    private String incoming_device_type;

    /////MMRData App/////
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.metawear_mmr.starter.DeviceSetupActivity.EXTRA_BT_DEVICE";
    public static String mac_address_mmr;
    public static class ReconnectDialogFragment extends DialogFragment implements  ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE = "com.mbientlab.metawear_mmr.starter.DeviceSetupActivity.ReconnectDialogFragment.KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice = null;
        private MetaWearBoard currentMwBoard = null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args = new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment = new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice = getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialogInterface, i) -> {
                currentMwBoard.disconnectAsync();
                getActivity().finish();
            });

            return reconnectDialog;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard= ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }

    private BluetoothDevice btDevice_mmr;
    private MetaWearBoard metawear_mmr;

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    /////MMRData App/////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_wearables);

        //////Toolbar Settings//////
        Toolbar toolbar = (Toolbar) findViewById(R.id.myToolbar_wearables);
        toolbar.setTitle(getString(R.string.app_name)); //setting the title

        setSupportActionBar(toolbar);   //placing toolbar in place of actionbar
        //////Toolbar Settings//////

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        String incoming_device_type = getIntent().getStringExtra(DEVICE_TYPE);
        /////SmartBand App/////
        if(incoming_device_type.equals("SmartBand")) {
            //if(MainActivity.isSmartbandConnected()) {

            //}
        }

        /////MMRData App/////
        if(incoming_device_type.equals("MMR")) {
            //if(MainActivity.isMmrConnected()) {
            btDevice_mmr = getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
            getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);
            //}
        }
        /////MMRData App/////
    }


    /////MENU/////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_wearables, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case R.id.menuBackToScan:
                Intent intent = new Intent(this, ChooseDeviceToScanActivity.class);
                startActivity(intent);
                break;

            case R.id.menuDisconnectDevice:
                //TODO code to disconnect current device

                //TAB2
                //metawear_mmr.disconnectAsync();
                //finish();
                //TAB2
                Toast.makeText(this, "Disconnecting device", Toast.LENGTH_SHORT).show();
                break;

            case R.id.menuInfo:
                inflateInfoPopup();
                break;

        }
        return true;
    }

    /**
     * PopUp Window of the Help button on the main screen
     */
    private void inflateInfoPopup(){
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
            popupInfo.showAtLocation(customView, Gravity.CENTER,0,0);

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
            popupInfo=null;
        }
    }


    /////MMRData App/////
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
            metawear_mmr = ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice_mmr);
            mac_address_mmr = metawear_mmr.getMacAddress();
            metawear_mmr.onUnexpectedDisconnect(status -> {
                ReconnectDialogFragment dialogFragment = ReconnectDialogFragment.newInstance(btDevice_mmr);
                dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);

                metawear_mmr.connectAsync().continueWithTask(task -> task.isCancelled() || !task.isFaulted() ? task : MainActivity.reconnect(metawear_mmr))
                        .continueWith((Continuation<Void, Void>) task -> {
                            if (!task.isCancelled()) {
                                runOnUiThread(() -> {
                                    ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();
                                    ((MMRSetupActivityFragment) getSupportFragmentManager().findFragmentById(R.id.mmr_setup_fragment)).reconnected();
                                });
                            } else {
                                finish();
                            }

                            return null;
                        });
            });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public BluetoothDevice getBtDevice_mmr() {
        return btDevice_mmr;
    }
    /////MMRData App/////
}