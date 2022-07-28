package com.upm.jgp.healthywear.ui.main.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.DataModule.FavouriteObject;
import com.upm.jgp.healthywear.ui.main.adapter.FavDevAdapter;

import java.util.ArrayList;
import java.util.Map;

/**
 * Activity that shows the favourite devices stored on the mobile
 *
 * Currently they can be of two different types: SmartBand or MMR
 * The connection option from this screen is to be implemented
 *
 * @author Jorge Garcia Paredes (yoryidan)
 *  Modified by Raquel Prous 2022
 *  @version 210
 *  @since 2020
 */
public class FavouriteDevices extends AppCompatActivity{

    ListView lv_favDev;
    private static ArrayAdapter adapter;
    private int selectedPosition = -1;
    private View prevView = null;
    private View currentView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_favourite_device);

        //////Toolbar Settings//////
        Toolbar toolbar = (Toolbar) findViewById(R.id.favDev_myToolbar);
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

        lv_favDev = findViewById(R.id.lv_favouriteDevices);
        adapter = new FavDevAdapter(this, R.layout.adapter_item_fav_devices, new ArrayList(MainActivity.getFavouritesMap().entrySet()));
        lv_favDev.setAdapter(adapter);
        lv_favDev.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
            }
        });

    }

    public void onClick(View view) {
        int id = view.getId();
        Map.Entry<String, FavouriteObject> selectedDevice = (Map.Entry<String, FavouriteObject>) adapter.getItem(selectedPosition);
        switch (id) {
            case R.id.favDev_delete:
                adapter.remove(selectedDevice);
                adapter.notifyDataSetChanged();
                if(adapter.getCount()<1) {
                    onBackPressed();
                }
                break;
            case R.id.favDev_connect:
                switch(selectedDevice.getValue().getmWearableType()){
                    case 1: //SmartBand
                        //TODO connect to SmartBand devices
                        Snackbar.make(view, "Connecting SmartBand device... (TBD)", Snackbar.LENGTH_LONG)
                                .show();
                        break;
                    case 2:
                        //TODO connect to MMR devices
                        Snackbar.make(view, "Connecting MMR device... (TBD)", Snackbar.LENGTH_LONG)
                                .show();
                        break;
                    case 3:
                        //TODO connect to MMR devices
                        Snackbar.make(view, "Connecting MMR2 device... (TBD)", Snackbar.LENGTH_LONG)
                                .show();
                        break;
                }
                break;
        }
    }
}
