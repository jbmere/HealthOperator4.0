package com.upm.jgp.healthywear.ui.main.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.ScanMMRActivity;
import com.upm.jgp.healthywear.ui.main.fragments.smartband.ScanSmartBandActivity;

public class ChooseDeviceToScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device_to_scan);

        CardView smartband_CV = findViewById(R.id.CV_card_selection_1);
        CardView mmr_CV = findViewById(R.id.CV_card_selection_2);

        //Set CardView color to grey if the device is already connected, white otherwise
        if(MainActivity.isSmartbandConnected()){
            smartband_CV.setCardBackgroundColor(getResources().getColor(R.color.grey));
        }else{
            smartband_CV.setCardBackgroundColor(getResources().getColor(R.color.white));
        }
        if(MainActivity.isMmrConnected()){
            mmr_CV.setCardBackgroundColor(getResources().getColor(R.color.grey));
        }else{
            mmr_CV.setCardBackgroundColor(getResources().getColor(R.color.white));
        }

        //////Toolbar Settings//////
        Toolbar toolbar = (Toolbar) findViewById(R.id.myToolbar_choose_scan);
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

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.LL_card_selection_1: //go to smartBand scan screen
                if(MainActivity.isSmartbandConnected()){
                    Toast.makeText(this, "SmartBand already connected", Toast.LENGTH_SHORT).show();
                }else {
                    Intent intent = new Intent(ChooseDeviceToScanActivity.this, ScanSmartBandActivity.class);
                    startActivity(intent);
                }
                break;

            case R.id.LL_card_selection_2: //go to smartband scan screen
                if(MainActivity.isMmrConnected()){
                    Toast.makeText(this, "MMR device already connected", Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent2 = new Intent(ChooseDeviceToScanActivity.this, ScanMMRActivity.class);
                    startActivity(intent2);
                }
                break;

            case R.id.LL_card_selection_3: //go to smartband scan screen
                //TODO code to connect to other devices
                Toast.makeText(this, "Other devices are not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
