package com.upm.jgp.healthywear.ui.main.fragments.smartband;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.orhanobut.logger.Logger;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;
import com.upm.jgp.healthywear.ui.main.activity.TabWearablesActivity;
import com.upm.jgp.healthywear.ui.main.fragments.common.MyService;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleNotifyResponse;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IBPDetectDataListener;
import com.veepoo.protocol.listener.data.ICustomSettingDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IPersonInfoDataListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.listener.data.ISleepDataListener;
import com.veepoo.protocol.listener.data.ISocialMsgDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.model.datas.BpData;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.PersonInfoData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.enums.EBPDetectModel;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.enums.EOprateStauts;
import com.veepoo.protocol.model.enums.ESex;
import com.veepoo.protocol.model.settings.CustomSettingData;
import com.veepoo.protocol.util.VPLogger;
import com.veepoo.protocol.util.VpBleByteUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

public class SmartBandSetupActivityFragment extends Fragment {
    private final static String TAG = SmartBandSetupActivityFragment.class.getSimpleName();
    static TextView tv_time, tv_hr, tv_bpl, tv_bph, tv_mac, tv_steps, tv_distance, tv_calories, tv_sleep, tv_location;
    static Context mContext = null;
    private static String smartbandAddress = null;
    private final int TYPESMARTBAND = 1;
    static Activity owner;

    static WriteResponse writeResponse = new WriteResponse();

    //timer task to measure HR and BP
    private static int FREQUENCY = 1; //Measure every n minutes
    static Timer timer;
    static TimerTask timerTask;
    Timer UItimer;
    TimerTask UIupdate;
    private static String heartrate = "measuring";
    private static String bphigh = "...";
    private static String bplow = "...";
    private static String time_now;
    private static String date_now;
    private static String location = "'lat':'-000.000000','lng':'-000.000000'";
    private static int steps_value = -1;
    private static final double STEPSTOKM = 0.0008; //Smartband uses aprox. 0.00082
    private static double distance_value = -1.1;
    private static final double STEPTOCALORIES = 0.05; //Smartband uses aprox. 0.053
    private static double calories_value = -1.1;
    private static String sleep_value = "unknown";
    //Handler handler =new Handler(); //updating UI
    private static String json_str=""; //"{'t':'0','hr':'0','bph':'0','bpl':'0'}";
    private static int Nrows=0;
    private static String  toplines="{'us':'USER','pass':'PASSWORDxx','db':'IoT','collection':'hrbp'}\n"+
            "{'mac':'"+ smartbandAddress + "'}\n";
    private static String folderPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp_hr" +  File.separator + "cache" + File.separator;
    static Lock lock= new ReentrantLock(true);
    static Timer gztimer;  //Timer for uploading data
    static TimerTask gztimerTask; //Timer task for uploading data


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        owner= getActivity();
        mContext = owner.getApplicationContext();

        smartbandAddress = getActivity().getIntent().getStringExtra("deviceaddress");;

        if(smartbandAddress==null){
            //Try to get it from global variable, probably when it is the second device connected, it was not taken from the Intent...
            smartbandAddress = MainActivity.getSmartband_mac_global();
        }

        updateTopLines();

        File folderfile= new File(folderPath);
        if (!folderfile.exists()){
            folderfile.mkdirs();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_tab_smartband, container, false);
    }

    //TODO Check stability
    public static void initializationOfTheConnectionWithSmartband(){
        //****Initializing****
        //Password verifying
        boolean is24Hourmodel = false;
        VPOperateManager.getMangerInstance(mContext).confirmDevicePwd(writeResponse, new IPwdDataListener() {
            @Override
            public void onPwdDataChange(PwdData pwdData) {
                String message = "PwdData:\n" + pwdData.toString();
                Logger.t(TAG).i(message);
                //sendMsg(message, 1);

                deviceNumber = pwdData.getDeviceNumber();
                deviceVersion = pwdData.getDeviceVersion();
                deviceTestVersion = pwdData.getDeviceTestVersion();

            }
        }, new IDeviceFuctionDataListener() {
            @Override
            public void onFunctionSupportDataChange(FunctionDeviceSupportData functionSupport) {
                String message = "FunctionDeviceSupportData:\n" + functionSupport.toString();
                Logger.t(TAG).i(message);
                //sendMsg(message, 2);
                EFunctionStatus newCalcSport = functionSupport.getNewCalcSport();
                if (newCalcSport != null && newCalcSport.equals(EFunctionStatus.SUPPORT)) {
                    isNewSportCalc = true;
                } else {
                    isNewSportCalc = false;
                }
                watchDataDay = functionSupport.getWathcDay();
                contactMsgLength = functionSupport.getContactMsgLength();
                allMsgLenght = functionSupport.getAllMsgLength();
                VPLogger.i("Data reading process，ORIGIN_DATA_DAY:" + watchDataDay);
            }
        }, new ISocialMsgDataListener() {
            @Override
            public void onSocialMsgSupportDataChange(FunctionSocailMsgData socailMsgData) {
                String message = "FunctionSocailMsgData:\n" + socailMsgData.toString();
                Logger.t(TAG).i(message);
                //sendMsg(message, 3);
            }
        }, new ICustomSettingDataListener() {
            @Override
            public void OnSettingDataChange(CustomSettingData customSettingData) {
                String message = "FunctionCustomSettingData:\n" + customSettingData.toString();
                Logger.t(TAG).i(message);
                //sendMsg(message, 4);
            }
        }, "0000", is24Hourmodel);


        // sync. personal info. 同步个人信息
        VPOperateManager.getMangerInstance(mContext).syncPersonInfo(writeResponse, new IPersonInfoDataListener() {
            @Override
            public void OnPersoninfoDataChange(EOprateStauts EOprateStauts) {
                String message = "Sync. personal info.:\n" + EOprateStauts.toString();
                Logger.t(TAG).i(message);
                //sendMsg(message, 1);
            }
        }, new PersonInfoData(ESex.MAN, 178, 70, 31, 8000));

        if(MainActivity.isReconnectingSmartband()){
            MainActivity.setReconnectingSmartband(false);
            owner.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Reconnected!!", Toast.LENGTH_SHORT).show();
                }
            });
            startTimer();
        }
    }

    FloatingActionButton fab = null;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //////from onCreate in VPBluetooth///////
        tv_time = (TextView) view.findViewById(R.id.smartband_value_time);
        tv_hr = (TextView) view.findViewById(R.id.smartband_value_hr);
        tv_bpl = (TextView) view.findViewById(R.id.smartband_value_bpl);
        tv_bph = (TextView) view.findViewById(R.id.smartband_value_bph);
        tv_mac = (TextView) view.findViewById(R.id.smartband_value_mac);

        tv_steps = (TextView) view.findViewById(R.id.smartband_value_steps);
        tv_distance = (TextView) view.findViewById(R.id.smartband_value_distance);
        tv_calories = (TextView) view.findViewById(R.id.smartband_value_calories);
        tv_sleep = (TextView) view.findViewById(R.id.smartband_value_sleep);
        tv_location = (TextView) view.findViewById(R.id.smartband_value_gps);

        //****Initializing****
        initializationOfTheConnectionWithSmartband();

        fab = view.findViewById(R.id.fabSmartBandfav);
        if (smartbandAddress == null) {
            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_dialog_alert));
        } else {
            if (MainActivity.checkFavouriteDevice(smartbandAddress)) {
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_on));
            } else {
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_off));
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (smartbandAddress != null) {
                    if (MainActivity.checkFavouriteDevice(smartbandAddress)) {
                        //TODO take device out from the list
                        if (MainActivity.deleteFavouriteDevice(smartbandAddress)) {
                            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.star_big_off));
                            Snackbar.make(view, "Device removed from favourites", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    } else {
                        //TODO add device to the list
                        if (MainActivity.putFavouriteDevice(smartbandAddress, TYPESMARTBAND)) {
                            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_on));
                            Snackbar.make(view, "Device added to favourites", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                }
            }
        });



        //button Start to start data collecting process
        view.findViewById(R.id.smartband_acc_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();   //start timer for zipping data
                //Toast.makeText(getContext(), "Start ...",Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.smartband_acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
                //Toast.makeText(getContext(), "Stoppp ...",Toast.LENGTH_SHORT).show();
            }
        });

    }

    //Timer
    private static boolean istimeron=false; //confirm if the timer is runing or not
    private static boolean isgztimeron=false; //
    public static void startTimer() {
        if(MainActivity.isSmartbandConnected()) {
            if (!istimeron) {
                timer = new Timer(); //set a new Timer
                initializeTimerTask(); //initialize the TimerTask's job
                istimeron = true;
                //schedule the timer, after the first 5000ms the TimerTask will run every 60000ms
                timer.schedule(timerTask, 5000, 1000 * 60 * FREQUENCY);
                time_now = getUintAsTimestamp(System.currentTimeMillis());
                date_now = getUintAsDatestamp(System.currentTimeMillis());
                tv_time.setText(time_now);
                tv_hr.setText(heartrate);
                tv_bpl.setText(bplow);
                tv_bph.setText(bphigh);
                tv_steps.setText(String.valueOf(steps_value));
                tv_distance.setText(String.format("%.2f", distance_value));
                tv_calories.setText(String.format("%.2f", calories_value));
                tv_sleep.setText(sleep_value);
                tv_location.setText(location);
                tv_mac.setText(smartbandAddress);
                tv_time.setTextColor(Color.parseColor("#FF99CC00"));
                tv_mac.setTextColor(Color.parseColor("#FF99CC00"));

                Log.i("Timer", "started!!!");
                Log.i("TimeNow", String.valueOf(time_now));
            }
            if (!isgztimeron) {
                //gizp file timer task
                gztimer = new Timer();
                initializegzTimerTask();
                isgztimeron = true;
                gztimer.schedule(gztimerTask, 1000 * 60 * 1, 1000 * 60 * 5);
            }
        }else{
            //You can show a reminder for the user, telling that the Device is disconnected
        }
    }

    public static void stopTimer(){
        //startTimer(); //start timer for zipping data
        if (istimeron){
            timer.cancel();
            istimeron=false;
            //stop HR measuring
            VPOperateManager.getMangerInstance(mContext).stopDetectHeart(writeResponse);
            //Stop BP measuring
            VPOperateManager.getMangerInstance(mContext).stopDetectBP(writeResponse, EBPDetectModel.DETECT_MODEL_PUBLIC);

            tv_time.setText("Stopped");
            tv_hr.setText("Stopped");
            tv_bpl.setText(".");
            tv_bph.setText(".");
            tv_steps.setText("Stopped");
            tv_distance.setText("Stopped");
            tv_calories.setText("Stopped");
            tv_sleep.setText("Stopped");
            tv_location.setText("Stopped");
            tv_time.setTextColor(Color.parseColor("#FFCC0000"));
            tv_mac.setTextColor(Color.parseColor("#FFCC0000"));

        }

        if (isgztimeron){
            gztimer.cancel();
            isgztimeron=false;
        }
    }
    //lo cambia cada minuto en la pantalla
    public static void initializeTimerTask() {

        if(MainActivity.isSmartbandConnected()) {
            timerTask = new TimerTask() {
                public void run() {

                    //get gps location
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            location= refresh_phone_Location(); //get location
                            tv_location.setText(location);      //update UI
                        }
                    });

                    //Steps
                    VPOperateManager.getMangerInstance(mContext).readSportStep(writeResponse, new ISportDataListener() {
                        @Override
                        public void onSportDataChange(SportData sportData) {
                            steps_value = sportData.getStep();
                            tv_steps.setText(String.valueOf(steps_value));
                            distance_value = steps_value * STEPSTOKM;
                            calories_value = steps_value * STEPTOCALORIES;
                            tv_distance.setText(String.format("%.2f", distance_value));
                            tv_calories.setText(String.format("%.2f", calories_value));
                            String message = "Current step count:\n" + sportData.getStep();
                            Logger.t(TAG).i(message);
                        }
                    });

                    VPOperateManager.getMangerInstance(mContext).readSleepData(writeResponse, new ISleepDataListener() {
                        @Override
                        public void onSleepDataChange(SleepData sleepData) {
                            String message = "";
                            String sleepUp = "";
                            //This compares the current day with the sleep WakeUp date to show the sleep hours only if they are from the current day
                            //For months values it is necessary to add a 0 for the months between 1 and 9
                            //It is necessary to confirm that for days of the month from 1 to 9 happens the same
                            if(sleepData.getSleepUp().month<10) {
                                if(sleepData.getSleepUp().day<10){
                                    sleepUp = sleepData.getSleepUp().year + "-0" + sleepData.getSleepUp().month + "-0" + sleepData.getSleepUp().day;    //Adds a 0 for the month and the day
                                }else {
                                    sleepUp = sleepData.getSleepUp().year + "-0" + sleepData.getSleepUp().month + "-" + sleepData.getSleepUp().day;
                                }
                            }else {
                                if(sleepData.getSleepUp().day<10){
                                    sleepUp = sleepData.getSleepUp().year + "-" + sleepData.getSleepUp().month + "-0" + sleepData.getSleepUp().day;
                                }else {
                                    sleepUp = sleepData.getSleepUp().year + "-" + sleepData.getSleepUp().month + "-" + sleepData.getSleepUp().day;
                                }
                            }
                            //sleep_value = sleepData.toString();   //All sleep Data, sleep time, deep sleep, etc

                            if (sleepUp.equals(date_now)) {
                                double sleep_hours = sleepData.getAllSleepTime() / 60.0;    //value from minutes to hours
                                sleep_value = String.format("%.2f", sleep_hours);
                                tv_sleep.setText(sleep_value);
                                message = "Sleep data-return:" + sleepData.toString();
                            } else {
                                message = "Sleep data-return: from sleepUP = " + sleepUp + " today is " + date_now + " " + sleepUp.equals(date_now);
                            }
                            Logger.t(TAG).i(message);
                        }

                        @Override
                        public void onSleepProgress(float progress) {

                            String message = "Sleep data-reading progress:" + "progress=" + progress;
                            Logger.t(TAG).i(message);
                        }

                        @Override
                        public void onSleepProgressDetail(String day, int packagenumber) {
                            String message = "Sleep data-reading progress detailed:" + "day=" + day + ",packagenumber=" + packagenumber;
                            Logger.t(TAG).i(message);
                        }

                        @Override
                        public void onReadSleepComplete() {
                            String message = "Sleep data-end of reading";
                            Logger.t(TAG).i(message);
                            if (sleep_value.equals("unknown")) {
                                sleep_value = "0,0";
                                tv_sleep.setText(sleep_value);
                            }
                        }
                    }, watchDataDay);

                    //Heart rate
                    VPOperateManager.getMangerInstance(mContext).startDetectHeart(writeResponse, new IHeartDataListener() {
                        int n = 0; //counter
                        long t0 = System.currentTimeMillis();
                        long t_dif = System.currentTimeMillis() - t0;

                        @Override
                        public void onDataChange(HeartData heart) {

                            //TODO when changing the Tab the Data on the fragment is deleted, so we write it again when new data is received...
                            tv_sleep.setText(sleep_value);
                            tv_mac.setText(smartbandAddress);
                            tv_mac.setTextColor(Color.parseColor("#FF99CC00"));

                            String strHR = heart.toString();
                            Logger.t(TAG).i(strHR);
                            t_dif = System.currentTimeMillis() - t0;
                            n++;
                            Log.i("n", Integer.toString(n));
                            Log.i("Time difference", Long.toString(t_dif));
                            if (heart.getData() != 0 || t_dif > 30 * 1000) {
                                heartrate = Integer.toString(heart.getData());
                                Log.i("heartrate", heartrate);
                                time_now = getUintAsTimestamp(System.currentTimeMillis());
                                date_now = getUintAsDatestamp(System.currentTimeMillis());
                                // tv_hr.setText(Integer.toString(heartrate));

                                // Stop HR measurement
                                VPOperateManager.getMangerInstance(mContext).stopDetectHeart(writeResponse);
                                Log.i("HR", "HR FINISHED!!");
                                //after HR measuring finished, start BP measuring

                                //Start BP measuring
                                VPOperateManager.getMangerInstance(mContext).startDetectBP(writeResponse, new IBPDetectDataListener() {
                                    @Override
                                    public void onDataChange(BpData bpData) {
                                        String strBP = bpData.toString();
                                        Logger.t(TAG).i(strBP);
                                        //sendMsg(message, 1);
                                        if (bpData.getProgress() == 100) {
                                            bphigh = Integer.toString(bpData.getHighPressure());
                                            bplow = Integer.toString(bpData.getLowPressure());
                                            //Log.i("*******BPhigh*******:", bphigh);
                                            tv_time.setText(time_now);
                                            tv_time.setTextColor(Color.parseColor("#FF99CC00"));
                                            tv_hr.setText(heartrate);
                                            tv_bpl.setText(bplow);
                                            tv_bph.setText(bphigh);

                                            //UpdateTopLines
                                            updateTopLines();
                                            //t is the time when the measurement started, not the real time of
                                            json_str = json_str + "{'t':'" + time_now + "','hr':'" + heartrate + "','bpl':'" + bplow + "','bph':'" + bphigh + "','steps':'" + steps_value + "','distance':'" + distance_value + "','calories':'" + calories_value + "','sleep':'" + sleep_value + "'}\n";
                                            Nrows++;
                                            Log.i("*******Nrows*******:", Integer.toString(Nrows));
                                            //define how many rows in one txt file
                                            if (Nrows == 5) {
                                                String input = toplines + json_str;
                                                //***Create text file and write data into it***
                                                System.out.println("writting to text file");
                                                lock.lock(); //Lock begin
                                                FileWriter fw = null;
                                                String txtname = folderPath + "hr" + time_now;
                                                File txtfile = new File(txtname + ".txt");
                                                if (txtfile.exists()) {                 //check if txt file already existed
                                                    File ftxt = new File(folderPath);    //if exist count the number of files with this name
                                                    int ntxt = 0;
                                                    for (File file : ftxt.listFiles()) {
                                                        if (file.isFile() && (file.getName().startsWith("hr" + time_now)) && (file.getName().endsWith(".txt"))) {
                                                            ntxt++;
                                                        }
                                                    }
                                                    txtname = txtname + Integer.toString(ntxt);
                                                }

                                                try {
                                                    fw = new FileWriter(txtname + ".txt", true);
                                                } catch (IOException e1) {
                                                    // TODO Auto-generated catch block
                                                    e1.printStackTrace();
                                                }
                                                BufferedWriter bufferWritter = new BufferedWriter(fw);
                                                try {
                                                    //bufferWritter.write(sb.toString());
                                                    bufferWritter.write(input);
                                                    bufferWritter.close();
                                                } catch (IOException e) {
                                                    System.out.println("Error writing to and closing file:" + e.getMessage());
                                                    lock.unlock(); //Release lock
                                                    return;
                                                }
                                                lock.unlock();  //Release lock
                                                json_str = "";
                                                Nrows = 0;
                                            }

                                            //Stop BP measuring
                                            VPOperateManager.getMangerInstance(mContext).stopDetectBP(writeResponse, EBPDetectModel.DETECT_MODEL_PUBLIC);
                                        }
                                    }
                                }, EBPDetectModel.DETECT_MODEL_PUBLIC);
                            }
                        }
                    });
                }

            };
        }else{
            //device is not connected yet, reconnection is made in the MainActivity (scan screen)
        }
    }

    //MongoDB
    public static void initializegzTimerTask() {
        gztimerTask = new TimerTask() {
            public void run() {
                //***Get file list in the folder // stackoverflow.com/questions/8646984/how-to-list-files-in-an-android-directory
                String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp_hr" +  File.separator + "cache";
                String bkpfolder = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp_hr" +  File.separator + "backup";

                File folderfilebkp= new File(bkpfolder);
                if (!folderfilebkp.exists()){
                    folderfilebkp.mkdirs();
                }

                try {
                    File filegz[] = findergz(folderpath);   //get all the .gz file
                    if (filegz.length>0) {			// If there are .gz files, upload them
                        for (int j = 0; j < filegz.length; j++) {
                            String datapathgz = bkpfolder + File.separator + filegz[j].getName();
                            File bkpfile = new File(datapathgz);
                            filegz[j].renameTo(bkpfile);
                        }
                    } else{
                        try {
                            File file[] = finder(folderpath);  //get all the .txt file
                            if (file.length > 0) {
                                for (int i = 0; i < file.length; i++) //Send all the files to the server one by one.
                                {
                                    boolean complete = isCompletelyWritten(file[i]); //Check if the file has completely written
                                    String srcpath = folderpath + File.separator + file[i].getName();
                                    String bkppath = bkpfolder + File.separator + file[i].getName();
                                    if (complete) {
                                        //compress the .txt file to .gz file
                                        String despath0 = srcpath.substring(0, srcpath.indexOf(".")) + ".gz";
                                        String gzfile = gzipFile(srcpath, despath0);
                                        File zip = new File(gzfile);
                                        String despath = bkpfolder + File.separator + zip.getName();
                                        File newzip = new File(despath);
                                        zip.renameTo(newzip);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            Log.d("Files", e.getLocalizedMessage() );
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.d("Files", e.getLocalizedMessage() );
                }
            }
        };
    }

    //Function to Get the GPS information from the phone. //Reference: http://blog.csdn.net/cjjky/article/details/6557561
    @SuppressLint("MissingPermission")
    private static String refresh_phone_Location(){
        double latitude=0.0;
        double longitude =0.0;
        double altitude =0.0;
        float accuracy=0;
        long t_gps=0;
        long utcTime = System.currentTimeMillis();
        //t_gps=utcTime+tmadrid.getOffset(utcTime);

        t_gps=utcTime;
        //TODO attach this fragment to Activity, when disconnecting smartband and connecting again, the getActivity method is retuning null object
        //solved by getting the LocationManager from TabWearablesActivity
        LocationManager locationManager = TabWearablesActivity.getLocationManager();
        LocationListener locationListener = new LocationListener() {

            //Provider's state triggers this function when the three states of available, temporarily unavailable and no service are directly switched
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            // This function is triggered when the Provider is enabled, such as GPS is turned on            @Override
            public void onProviderEnabled(String provider) {
            }

            //This function is triggered when the Provider is disabled, such as GPS is turned off
            @Override
            public void onProviderDisabled(String provider) {
            }

            //This function is triggered when the coordinates change, if the Provider passes the same coordinates, it will not be triggered
            @Override
            public void onLocationChanged(Location location) {
                //TODO uncomment?
    			/*if (location != null) {
    				Log.e("Map", "Location changed : Lat: "
    				+ location.getLatitude() + " Lng: "  + location.getLongitude()+ " Alt: "
    				+ location.getAltitude()+" Acc: " + location.getAccuracy()+" t_gps:"+location.getTime());
    			}	   */
                //t_gps=location.getTime(); //timestamp
            }
        };

        if (MainActivity.isLocationPermissionsGranted()) {  //Permissions are explicitely checked on MainActivity
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,300, 0,locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location != null){
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    altitude = location.getAltitude();
                    accuracy=location.getAccuracy();
                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,300, 0,locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location != null){
                    latitude = location.getLatitude(); //Latitude
                    longitude = location.getLongitude(); //Longitude
                    altitude=location.getAltitude(); //Altitude
                    accuracy=location.getAccuracy(); //Accuracy, in meters
                }
            }
        }

        //String location="'lat':'"+String.format("%.6f",(latitude))+"','lng':'"+String.format("%.6f",(longitude)) +"','t_gps':'"+getUintAsTimestampGPS(t_gps)+"'";
        location ="'lat':'"+String.format("%.6f",(latitude))+"','lng':'"+String.format("%.6f",(longitude)) +"'";
        //Toast.makeText(this, location, Toast.LENGTH_SHORT).show();
        return location;
    }


    /**
     * 密码验证获取以下信息
     * Password verification to get the following information
     */
    static int watchDataDay = 3;
    static int contactMsgLength = 0;
    static int allMsgLenght = 4;
    private static int deviceNumber = -1;
    private static String deviceVersion;
    private static String deviceTestVersion;
    boolean isOadModel = false;
    static boolean isNewSportCalc = false;


    //button Start to start data collecting process
    public void startTimerTask(View view) {
        startTimer(); //start timer for zipping data
    }
    public void stopTimerTask(View view) {
        stopTimer();
    }


    /**
     * Write status returned
     */
    public static class WriteResponse implements IBleWriteResponse {
        @Override
        public void onResponse(int code) {
            Logger.t(TAG).i("write cmd status:" + code);
            switch (code){
                case -1:
                    //deviceDisconnected();
                    //TODO reconnection code??
                    //made on MainActivity.java
                    //Here it would jump more than once without a timer...
                    break;
            }
        }
    }

    /**
     * Returns the data of the device
     */
    public void listenDeviceCallbackData() {
        VPOperateManager.getMangerInstance(mContext).listenDeviceCallbackData(new IBleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                Logger.t(TAG).i("Data returned by the device：" + Arrays.toString(VpBleByteUtil.byte2HexToStrArr(value)));
            }

        });
    }

    public static void deviceDisconnected(){
        tv_time.setTextColor(Color.parseColor("#FFCC0000"));
        tv_mac.setTextColor(Color.parseColor("#FFCC0000"));
        if(MainActivity.isSmartbandConnected()) {
            //Toast.makeText(mContext, "Device disconnected", Toast.LENGTH_SHORT).show();
            VPOperateManager.getMangerInstance(mContext).disconnectWatch(writeResponse);
            MainActivity.setSmartbandConnected(false);
        }
        stopTimer();
    }

    public static void drawingReconnectingLayout(){
        tv_time.setText("Reconnecting...");
        tv_time.setTextColor(Color.parseColor("#FF7E00"));
        tv_mac.setTextColor(Color.parseColor("#FF7E00"));
    }


    @Override
    public void onDestroy() {
        if (istimeron) {
            timer.cancel();
        }

        if (isgztimeron) {
            gztimer.cancel();
        }

        finish();
        super.onDestroy();
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format
    //Change the date format
    private static String getUintAsTimestamp(Long uint) {
        //return DATE_FORMAT.format(new Date(uint.longValue() * 1000L)).toString();
        //uint=uint+tmadrid.getOffset(uint);
        //DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT.format(new Date(uint)).toString();
    }

    //Date format to compare if the sleeping data coming from the smartband is from today or last days
    private static final DateFormat DATE_FORMAT2 = new SimpleDateFormat("yyyy-MM-dd"); //Set the format
    //Change the date format
    private static String getUintAsDatestamp(Long uint) {
        //return DATE_FORMAT.format(new Date(uint.longValue() * 1000L)).toString();
        //uint=uint+tmadrid.getOffset(uint);
        //DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT2.format(new Date(uint)).toString();
    }


    //Gzip a text file http://examples.javacodegeeks.com/core-java/io/fileinputstream/compress-a-file-in-gzip-format-in-java/
    public static String gzipFile(String source_filepath, String destinaton_zip_filepath) {
        System.out.println("Compressing "+ source_filepath+"...........");
        byte[] buffer = new byte[256];
        File textfile = new File(source_filepath);
        if (textfile.exists() && textfile.length()>100) {
            String gzfile = countgz(source_filepath);
            destinaton_zip_filepath = source_filepath.substring(0,
                    source_filepath.indexOf(".")) + "_" + gzfile + ".gz";
            //System.out.println("gzfile:"+gzfile);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(
                        destinaton_zip_filepath);
                GZIPOutputStream gzipOuputStream = new GZIPOutputStream(
                        fileOutputStream);
                FileInputStream fileInput = new FileInputStream(source_filepath);
                int bytes_read;
                while ((bytes_read = fileInput.read(buffer)) > 0) {
                    gzipOuputStream.write(buffer, 0, bytes_read);
                }
                try {
                    fileInput.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                try {
                    gzipOuputStream.finish();
                    gzipOuputStream.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                System.out.println("The file was compressed successfully!");
                File gzf = new File(destinaton_zip_filepath);//check if the generated gzfile is larger than 1kb.
                if (gzf.length() > 10) {
                    textfile.delete();
                    System.out.println("The file was compressed successfully!");
                    return destinaton_zip_filepath;
                } else {
                    gzf.delete();
                    return null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    public static String countgz(String filepath){
        File txtfile = new File (filepath);
        String fullname = txtfile.getName();
        String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp_hr" +  File.separator + "cache"+File.separator;
        String firstname=fullname.substring(0, fullname.indexOf("."));
        //System.out.println("fullname:"+fullname+"folderpath:"+folderpath+"firstname:"+firstname);
        File gzf=new File(folderpath);
        int count=0;
        for (File file : gzf.listFiles()){
            if (file.isFile() && (file.getName().startsWith(firstname)) && (file.getName().endsWith(".gz"))) {
                count++;
            }
        }
        return Integer.toString(count);
    }

    //find all the .txt files in a folder. http://stackoverflow.com/questions/1384947/java-find-txt-files-in-specified-folder
    public static File[] finder( String dirName){
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".txt"); }
        } );
    }

    //find .gz file
    public static File[] findergz( String dirName){
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".gz"); }
        } );
    }

    //Check if a file is been written.10 seconds since last modification.
    private static boolean isCompletelyWritten(File file) {
        long currenttime=System.currentTimeMillis();
        long lastmodify=file.lastModified();
        if (currenttime-lastmodify>(10000)){
            return true;
        }else{
            return false;
        }
    }


    //***************************************************************************
    //****** code from uploadMMRData app ***
    @Override
    public void onResume() {
        super.onResume();
        //System.out.println("onResume...");

        //***Receive textview information from MyService***
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
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
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
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
        PendingIntent intent = PendingIntent.getActivity(owner.getBaseContext(), 0, new Intent(owner.getIntent()), owner.getIntent().getFlags());
        AlarmManager manager = (AlarmManager) owner.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
        //System.exit(0);
        finish();
    }

    public void finish(){
        //*********Stop MyService***
        Intent stopIntent = new Intent(owner, MyService.class);
        owner.stopService(stopIntent);
        //System.out.println("service is stopped!");
        //System.exit(0);
    }

    private static void updateTopLines() {
        toplines = "{'us':'USER','pass':'PASSWORDxx','db':'IoT','collection':'hrbp'}\n" +
                "{'mac':'" + smartbandAddress + "','appversion':'" + MainActivity.getStringAppVersion() + "'," + location +"}\n";
    }

}
