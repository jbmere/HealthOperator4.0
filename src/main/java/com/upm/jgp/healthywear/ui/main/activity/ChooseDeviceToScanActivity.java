package com.upm.jgp.healthywear.ui.main.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.upm.jgp.healthywear.R;

public class ChooseDeviceToScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device_to_scan);

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

    //TODO activate these options when the specific devices are not connected yet
    //TODO change cardview color if the device is already connected
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.card_selection_1: //go to smartband scan screen
                Intent intent = new Intent(ChooseDeviceToScanActivity.this, ScanSmartBandActivity.class);
                startActivity(intent);
                break;
            case R.id.card_selection_2: //go to smartband scan screen
                //Toast.makeText(this, "MMR", Toast.LENGTH_SHORT).show();
                Intent intent2 = new Intent(ChooseDeviceToScanActivity.this, ScanMMRActivity.class);
                startActivity(intent2);
            break;
            case R.id.card_selection_3: //go to smartband scan screen
                //TODO code to connect to other devices
                Toast.makeText(this, "Other devices are not yet implemented.", Toast.LENGTH_SHORT).show();
            break;
        }
    }
}
