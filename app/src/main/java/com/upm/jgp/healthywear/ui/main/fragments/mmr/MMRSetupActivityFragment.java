package com.upm.jgp.healthywear.ui.main.fragments.mmr;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.AmbientLightLtr329.Gain;
import com.mbientlab.metawear.module.AmbientLightLtr329.IntegrationTime;
import com.mbientlab.metawear.module.AmbientLightLtr329.MeasurementRate;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi160.OutputDataRate;
import com.mbientlab.metawear.module.GyroBmi160.Range;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.AccRange;
import com.mbientlab.metawear.module.SensorFusionBosch.GyroRange;
import com.mbientlab.metawear.module.SensorFusionBosch.Mode;
import com.mbientlab.metawear.module.Temperature;
import com.upm.jgp.healthywear.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

import bolts.Continuation;
import bolts.Task;

//import com.mbientlab.metawear.data.Quaternion;
//import com.mbientlab.metawear.module.Timer;
//import com.mbientlab.metawear.module.HumidityBme280;
//import com.mbientlab.metawear.module.HumidityBme280.OversamplingMode;


/*
 * From Android API level 23, APP has to ask for permission to write or read from local folders.
 * This required update for the previous versions of the app which was developped for old phones lower than level 23.
 * This was done in the MainActivity class
 * https://stackoverflow.com/questions/44455887/permission-denied-on-writing-to-external-storage-despite-permission
 * */

/**
 * A placeholder fragment containing a simple view.
 */
public class MMRSetupActivityFragment extends Fragment implements ServiceConnection {
    public interface FragmentSettings {
        BluetoothDevice getBtDevice_mmr();
    }

    private MetaWearBoard metawear = null;
    private FragmentSettings settings;
    //private String MAC=metawear.getMacAddress();

    public MMRSetupActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        //getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_tab_mmr, container, false);
    }

    private SensorFusionBosch sensorFusion;
    private BarometerBosch baroBosch;
    //private HumidityBme280 humidity;
    private Temperature temperature;
    private Temperature.Sensor tempSensor;
    private AmbientLightLtr329 alsLtr329;
    private GyroBmi160 gyroBmi160;


    private Accelerometer accelerometer;
    //private Logging logging= metawear.getModule(Logging.class);
    private Logging logging;
    private static final String LOG_TAG = "Logging";

    private long time_now=0;
    private int n =0;
    //public static  String mac_address_mmr = "D2:01:2C:D9:BC:76"; //Defined in DeviceSetupActivity.java
    Lock lock= new ReentrantLock(true); //Define a lock to avoid the concurrency problem when writing data to txt file
    private String folderPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache" + File.separator;
    //private String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+ File.separator + "tmp" +  File.separator + "cache" + File.separator;
    //private String folderPath = getPublicDownloadStorageDir("cache").getPath();
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format of the .txt file name.
    //DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    Timer timer;  //Timer for uploading data
    TimerTask timerTask; //Timer task for uploading data
    //Timer timer_peb_app;
    //TimerTask timerTask_peb_app;
    //private static Boolean wificon=false;
    private static Boolean timerstatus=false;
    static String pass="1314";
    static String op="ADD";

    private String TEMP="0"; //TEMPETURE
    private String ILLUM="0"; //Illumnition
    private String ALT="0"; //ALTITUDE
    private String armangle ="0";
    private String location = "'lat':'000.000000','lng':'000.000000'";
    private String GYRO ="000";
    private Double arm;

    TextView tv_mac = null;
    TextView tv_temp = null;
    TextView tv_illm = null;
    TextView tv_alt = null;
    TextView tv_arm = null;
    TextView tv_acc = null;
    TextView tv_gps = null;


    // private Timer timer = metawear.getModule(Timer.class); //timer to download data from logging
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //textview to show data
        tv_mac = view.findViewById(R.id.mmr_value_mac);     //device MAC
        tv_temp = view.findViewById(R.id.mmr_value_temp);	//Temp data
        tv_illm = view.findViewById(R.id.mmr_value_illum);	//Illum data
        tv_alt =  view.findViewById(R.id.mmr_value_alt);	//Altitude data
        tv_arm =  view.findViewById(R.id.mmr_value_arm);    //arm angle
        tv_acc =  view.findViewById(R.id.mmr_value_acc);    //acceleration
        tv_gps =  view.findViewById(R.id.mmr_value_gps);    //location gps


        view.findViewById(R.id.mmr_acc_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer(); //start timer for zipping data
                StringBuffer sb = new StringBuffer();
                //READ TEMP DATA
                readTEMP();
                //Log.i("*****MAC****", DeviceSetupActivity.MAC);

                //change color to green if start on click
                if(metawear.isConnected()) {
                    tv_mac.setTextColor(Color.parseColor("#FF99CC00"));
                    tv_mac.setText(metawear.getMacAddress());
                }
                //accelerometer
                accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        //saveLog11();
                        //saveLog();//timer task to save the log data
                        //Method to calculate timestamp https://mbientlab.com/community/discussion/1934/metahub-timestamps#latest
                        source.stream(new Subscriber() {
                        //source.log(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                time_now = System.currentTimeMillis();
                                n++;
                                //Log.i("counter", String.valueOf(n));
                                //Log.i("acc_x", String.valueOf(data.value(Acceleration.class).x()) ); //print acc.x
                                String dataString = "{'x':" + String.format("%.3f",(data.value(Acceleration.class).x())) +
                                        ",'y':" + String.format("%.3f",(data.value(Acceleration.class).y())) +
                                        ",'z':" + String.format("%.3f",(data.value(Acceleration.class).z())) +
                                        ",'t':" + getUintAsTimestampGPS(time_now) +"}";  //timestamp when the data arrived Android
                                //Log.i("JSON:", dataString); //print complete acc vector
                                arm = 180-Math.acos(data.value(Acceleration.class).y())* 180f / Math.PI;
                                if (Double.isNaN(arm)){
                                    arm = 0.0;
                                }
                                armangle = String.format("%.2f", arm);
                                if (n%25==0){
                                    //Log.i("Arm Angle:", armangle);
                                    //https://stackoverflow.com/questions/47041396/only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-views
                                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                                        @Override
                                        public void run() {
                                            //if (Float.valueOf(armangle)>90){
                                            if (arm>90.0){
                                                //change color to red if angle >90
                                                tv_arm.setTextColor(Color.parseColor("#FFFF0000"));
                                            } else{
                                                tv_arm.setTextColor(Color.parseColor("#FF00DDFF")); //ANGLE <90 change back to blue
                                            }
                                            tv_arm.setText(armangle);
                                            tv_acc.setText(dataString);
                                            //tv_temp.setText(TEMP);
                                        }
                                    });

                                }

                                sb.append(dataString);
                                sb.append("\n");

                                if (n%750==0){

                                    //get gps location
                                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                                        @Override
                                        public void run() {
                                            location=getLocation(); //get location
                                            tv_gps.setText(location); //update UI
                                        }
                                    });
                                    String shortname=getUintAsTimestamp(time_now); //timestam
                                    //mongo --host 138.100.82.181:666 IoT -u IoT -p WISEST#2019
                                    String toplines="{'us':'IoT','pass':'WISEST#2019','db':'IoT','collection':'mmr_acc'}\n"+
                                            "{'mac':'"+metawear.getMacAddress()+"'," + location + ",'alt':'" + ALT + "','t_end':'" +shortname+ "','temp':'" + TEMP+ "','illum':'" + ILLUM+"'}\n";
                                    Log.i("Toplines:", toplines);

                                    String input=toplines+sb.toString();

                                    //***Create text file and write data into it***
                                    System.out.println("writting to text file");
                                    //System.out.println("count:"+count+"buffer:"+sb.toString());
                                    lock.lock(); //Lock begin
                                    FileWriter fw = null;
                                    //String shortname=getUintAsTimestamp(time_now);


                                    // 20190409
                                    String txtname=folderPath + shortname;
                                    //create folder if not exist
                                    File folderfile= new File(folderPath);
                                    if (!folderfile.exists()){
                                        folderfile.mkdirs();
                                    }

                                    File txtfile=new File(txtname+".txt");
                                    if (txtfile.exists()){                 //check if txt file already existed
                                        File ftxt=new File(folderPath);    //if exist count the number of files with this name
                                        int ntxt=0;
                                        for (File file : ftxt.listFiles()){
                                            if (file.isFile() && (file.getName().startsWith(shortname)) && (file.getName().endsWith(".txt"))) {
                                                ntxt++;
                                            }
                                        }
                                        txtname=txtname+Integer.toString(ntxt);
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
                                        System.out.println("Error writing to and closing file:"+e.getMessage());
                                        lock.unlock(); //Release lock
                                        return;
                                    }
                                    lock.unlock();  //Release lock
                                    sb.delete(0, sb.length());
                                }
                            }
                        });
                    }

                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        accelerometer.acceleration().start();
                        accelerometer.start();
                        //logging.start(true);  //start logging data to the flash memory
                        //saveLog();//timer task to save the log data
                        return null;
                    }
                });

/*

                //Gyro:The gyro sensor on this device is represented by the GyroBmi160 interface and uses the Coriolis effect to measure angular velocity.
                gyroBmi160.angularVelocity().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                //Log.i("MainActivity", data.value(AngularVelocity.class).toString());
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        gyroBmi160.angularVelocity();
                        gyroBmi160.start();
                        return null;
                    }
                });
*/

                //barometer altitude value
                baroBosch.altitude().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                ALT = String.format("%.2f",(data.value(Float.class)));
                               //Log.i("MainActivity", "Altitude (m) = " + data.value(Float.class));
                                new Handler(Looper.getMainLooper()).post(new Runnable(){
                                    @Override
                                    public void run() {
                                        tv_alt.setText(ALT);
                                    }
                                });
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        baroBosch.altitude().start();
                        baroBosch.start();
                        return null;
                    }
                });
/*
                //MMR doesn't have HUMIDITY sensor, Uncomment this section when necessary for other models
                // Relative humidity data is a float value from 0 to 100 percent and is represented as a forced data producer.
                humidity.value().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                Log.i("MainActivity", "Humidity = " + data.value(Float.class));
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        humidity.value().read();
                        return null;
                    }
                });
*/
                // Temperature data
                //****THE METHOD IN THE API DOCUMENTATION DOESNOT WORK. NOT UPDATING.  Use the readTEMP() function instead****
                // Temperature data is reported in Celsius and interepreted as a float value. It is represented as a forced data producer.
                //temperature = metawear.getModule(Temperature.class);
                //Temperature.Sensor tempSensor = temperature.findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
/*
                tempSensor.addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                TEMP = String.valueOf(data.value(Float.class));
                                //Log.i("MainActivity", "Temperature (C) = " + data.value(Float.class));
                                new Handler(Looper.getMainLooper()).post(new Runnable(){
                                    @Override
                                    public void run() {
                                        tv_temp.setText(TEMP);
                                    }
                                });
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        //metawear.getModule(BarometerBosch.class).start();
                        tempSensor.read();
                        return null;
                    }
                });

*/



                // Illuminance Data
                // Illuminance data is categorized as an async data producer; data is interpreted as a float value and is in units of lux (lx).
                alsLtr329.illuminance().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                ILLUM = String.format(Locale.US, "%.2f", data.value(Float.class));
                                //Log.i("MainActivity", String.format(Locale.US, "illuminance = %.3f lx", data.value(Float.class)));
                                new Handler(Looper.getMainLooper()).post(new Runnable(){
                                    @Override
                                    public void run() {
                                        tv_illm.setText(ILLUM);
                                    }
                                });
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        alsLtr329.illuminance().start();
                        return null;
                    }
                });

/*
                //SENSOR FUSION
                //THIS CAN BE USED FOR ACCURATE ARM ANGLE CALCULATION
                final SensorFusionBosch sensorFusion = metawear.getModule(SensorFusionBosch.class);
                sensorFusion.quaternion().addRouteAsync(new RouteBuilder() {
                //sensorFusion.eulerAngles().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                //Log.i("DataFusion", "Quaternion = " + data.value(Quaternion.class));
                                float halfAngle = (float) Math.acos(data.value(Quaternion.class).w());
                                float angle =(float) (halfAngle * 360f / Math.PI);

                                //Log.i("Rotation Angle", "Angle = " + String.valueOf(angle));
                                //Log.i("DataFusion", "eulerAngles = " + data.value(EulerAngles.class));
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        //sensorFusion.eulerAngles().start();
                        sensorFusion.quaternion().start();
                        sensorFusion.start();
                        return null;
                    }
                });
                */

            }
        });
        view.findViewById(R.id.mmr_acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelerometer.stop();
                accelerometer.acceleration().stop();
                //sensorFusion.quaternion().stop();
                //sensorFusion.stop();
                //logging.stop(); //stop logging
                metawear.tearDown();
                if(timer!=null)
                    timer.cancel();

                baroBosch.altitude().stop();
                baroBosch.stop();
                alsLtr329.illuminance().stop();

                //change color to red if start on click
                tv_mac.setTextColor(Color.parseColor("#FFCC0000"));
                if(metawear.isConnected()) {
                    tv_mac.setText("Stopped");
                }else{
                    tv_mac.setText("Disconnected");
                }
            }
        });
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        metawear = ((BtleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice_mmr());

        accelerometer= metawear.getModule(Accelerometer.class);
        accelerometer.configure()
                .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
                .range(4f)      // Set data range to +/-4g, or closet valid range
                .commit();
        Log.i("MainActivity", "Actual Odr = " + accelerometer.getOdr());

        logging=metawear.getModule(Logging.class);

        // use ndof mode with +/-16g acc range and 2000dps gyro range
        sensorFusion = metawear.getModule(SensorFusionBosch.class);
        sensorFusion.configure()
                .mode(Mode.IMU_PLUS)
                .accRange(AccRange.AR_16G)
                .gyroRange(GyroRange.GR_2000DPS)
                .commit();

        baroBosch = metawear.getModule(BarometerBosch.class);
        // configure the barometer with suggested values for indoor navigation
        baroBosch.configure()
                .filterCoeff(BarometerBosch.FilterCoeff.AVG_16)
                .pressureOversampling(BarometerBosch.OversamplingMode.ULTRA_HIGH)
                .standbyTime(0.5f)
                .commit();

        //humidity = metawear.getModule(HumidityBme280.class);
        // set oversampling to 16x
        //humidity.setOversampling(OversamplingMode.SETTING_16X);

        //https://mbientlab.com/androiddocs/3/temperature.html
        //temerature not updating
        temperature = metawear.getModule(Temperature.class);
        //tempSensor = temperature.findSensors(Temperature.SensorType.NRF_SOC)[0];
        //tempSensor = temperature.findSensors(Temperature.SensorType.BOSCH_ENV)[1];
        //Temperature.Sensor tempSensor = temperature.findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
        //tempSensor = temperature.findSensors(Temperature.SensorType.EXT_THERMISTOR)[0];
        timerModule= metawear.getModule(com.mbientlab.metawear.module.Timer.class);




        //Light
        alsLtr329 = metawear.getModule(AmbientLightLtr329.class);
        // Set the gain to 8x
        // Set integration time to 250ms
        // Set measurement rate to 50ms
        alsLtr329.configure()
                .gain(Gain.LTR329_8X)
                .integrationTime(IntegrationTime.LTR329_TIME_250MS)
                .measurementRate(MeasurementRate.LTR329_RATE_500MS)
                .commit();

        //Gyro
        gyroBmi160 = metawear.getModule(GyroBmi160.class);
        // set the data rat to 50Hz and the
        // data range to +/- 2000 degrees/s
        gyroBmi160.configure()
                .odr(OutputDataRate.ODR_25_HZ)
                .range(Range.FSR_2000)
                .commit();

        //check if folder exist, create them if not
        File cachefolder = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache");
        if (!cachefolder.exists())
            cachefolder.mkdirs();
        File backfolder = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "backup");
        if (!backfolder.exists())
            backfolder.mkdirs();

        /*boolean writable = isExternalStorageWritable();
        if (writable){
            Log.i("ExternalStorageWritable", "********YES!!!!!");
        }else {
            Log.i("ExternalStorageWritable", "********NO!!!!!");
        }
*/


    }
    /* Checks if external storage is available for read and write */
  /*  public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getPublicDownloadStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), albumName);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }*/



    //setup temp sensor  // https://github.com/mbientlab/MetaWear-SampleApp-Android/blob/master/app/src/main/java/com/mbientlab/metawear/app/TemperatureFragment.java#L228
    private com.mbientlab.metawear.module.Timer timerModule;
    private com.mbientlab.metawear.module.Timer.ScheduledTask scheduledTask;
    protected void readTEMP() {
        byte gpioDataPin= 0, gpioPulldownPin= 1;
        boolean activeHigh= false;
        int TEMP_SAMPLE_PERIOD= 1000;  //UPDATE FREQUENCY
        Temperature.Sensor tempSensor = temperature.sensors()[0];
        if (tempSensor.type() == Temperature.SensorType.EXT_THERMISTOR) {
            ((Temperature.ExternalThermistor) temperature.sensors()[0]).configure(gpioDataPin, gpioPulldownPin, activeHigh);
        }
        tempSensor.addRouteAsync(source -> source.stream((data, env) -> {
            TEMP = String.valueOf(data.value(Float.class));

            //Update UI
            new Handler(Looper.getMainLooper()).post(new Runnable(){
                @Override
                public void run() {
                    tv_temp.setText(TEMP);
                }
            });

        })).continueWithTask(task -> {
            task.getResult();
            return timerModule.scheduleAsync(TEMP_SAMPLE_PERIOD, false, tempSensor::read);
        }).continueWithTask(task -> {
            scheduledTask = task.getResult();
            scheduledTask.start();
            return null;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        timer.cancel();
    }




    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }



    //Timer for zipping data
    public void startTimer() {
        timerstatus=true;
        timer = new Timer(); //set a new Timer
        initializeTimerTask(); //initialize the TimerTask's job
        //schedule the timer, after the first 5000ms the TimerTask will run every 60000ms
        timer.schedule(timerTask, 5000, 60000);
    }


    //task to zip data files
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                //***Get file list in the folder // stackoverflow.com/questions/8646984/how-to-list-files-in-an-android-directory
                String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache";
                String bkpfolder = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "backup";

                try {
                    //File file[] = f.listFiles();
                    File filegz[] = findergz(folderpath);   //get all the .gz file
                    //if (filegz.length>0) {			// If there are .gz files, upload them
                    if (filegz!=null && filegz.length>0){
                        for (int j = 0; j < filegz.length; j++) {
                            String datapathgz = bkpfolder + File.separator + filegz[j].getName();
                            File bkpfile = new File(datapathgz);
                            //new RetrieveFeedTask().execute(datapathgz);
                            filegz[j].renameTo(bkpfile);
                        }
                    } else{
                        try {
                            File file[] = finder(folderpath);  //get all the .txt file
                            //if (file.length > 0) {
                            if (file!=null && file.length > 0) {
                                for (int i = 0; i < file.length; i++) //Send all the files to the server one by one.
                                {
                                    Log.d("Files", "FileName:" + file[i].getName());
                                    boolean complete = isCompletelyWritten(file[i]); //Check if the file has completely written
                                    String srcpath = folderpath + File.separator + file[i].getName();
                                    String bkppath = bkpfolder + File.separator + file[i].getName();
                                    if (complete) {
                                        //Log.d("Files", "path" + datapath);
                                        //new RetrieveFeedTask().execute(datapath); //execute new thread 执行同步线程
                                        //Log.d("Files", "i:" + i);
                                        //compress the .txt file to .gz file
                                        String despath0 = srcpath.substring(0, srcpath.indexOf(".")) + ".gz";
                                        //String despath=datapath[0]+".gz";
                                        String gzfile = gzipFile(srcpath, despath0);
                                       //Log.d("GZFILE", gzfile);
                                        if (!isNullOrEmpty(gzfile)){   //in case that gzfile is null
                                            File zip = new File(gzfile);
                                            String despath = bkpfolder + File.separator + zip.getName();
                                            File newzip = new File(despath);
                                            zip.renameTo(newzip);
                                        }
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

    //Function to check a string is Null or Empty
    public static boolean isNullOrEmpty(String str) {
        if(str != null && !str.trim().isEmpty())
            return false;
        return true;
    }


    //Gzip a text file http://examples.javacodegeeks.com/core-java/io/fileinputstream/compress-a-file-in-gzip-format-in-java/
    public String gzipFile(String source_filepath, String destinaton_zip_filepath) {
        System.out.println("Compressing "+ source_filepath+"...........");
        byte[] buffer = new byte[1024];
        File textfile = new File(source_filepath);
        if (textfile.exists() && textfile.length()>1000) {
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
                //System.out.println("The file was compressed successfully!");
                File gzf = new File(destinaton_zip_filepath);//check if the generated gzfile is larger than 1kb.
                if (gzf.length() > 1000) {
                    textfile.delete();
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
            if (textfile.exists()){
                textfile.delete();
            }
            return null;
        }
    }

    public String countgz(String filepath){
        File txtfile = new File (filepath);
        String fullname = txtfile.getName();
        String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache"+File.separator;
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
    public File[] finder( String dirName){
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".txt"); }
        } );
    }

    //find .gz file
    public File[] findergz( String dirName){
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".gz"); }
        } );
    }

    //Check if a file is been written.10 seconds since last modification.
    private boolean isCompletelyWritten(File file) {
        long currenttime=System.currentTimeMillis();
        long lastmodify=file.lastModified();
        if (currenttime-lastmodify>(10000)){
            return true;
        }else{
            return false;
        }
    }

    //Change the date format
    private String getUintAsTimestamp(Long uint) {
        //return DATE_FORMAT.format(new Date(uint.longValue() * 1000L)).toString();
        //uint=uint+tmadrid.getOffset(uint);
        //DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        return DATE_FORMAT.format(new Date(uint)).toString();
    }

    //Function to Get the GPS information from the phone. //Reference: http://blog.csdn.net/cjjky/article/details/6557561
    public String getLocation(){
        double latitude=0.0;
        double longitude =0.0;
        double altitude =0.0;
        float accuracy=0;
        long t_gps=0;
        long utcTime = System.currentTimeMillis();
        //t_gps=utcTime+tmadrid.getOffset(utcTime);

        t_gps=utcTime;
        LocationManager locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            // Provider被enable时触发此函数，比如GPS被打开
            @Override
            public void onProviderEnabled(String provider) {
            }
            // Provider被disable时触发此函数，比如GPS被关闭
            @Override
            public void onProviderDisabled(String provider) {
            }
            //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
            @Override
            public void onLocationChanged(Location location) {
    			/*if (location != null) {
    				Log.e("Map", "Location changed : Lat: "
    				+ location.getLatitude() + " Lng: "  + location.getLongitude()+ " Alt: "
    				+ location.getAltitude()+" Acc: " + location.getAccuracy()+" t_gps:"+location.getTime());
    			}	   */
                //t_gps=location.getTime(); //timestamp
            }
        };

        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
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
                    latitude = location.getLatitude(); //经度
                    longitude = location.getLongitude(); //纬度
                    altitude=location.getAltitude(); //海拔
                    accuracy=location.getAccuracy(); //精度, in meters
                }
            }
        }

        //String location="'lat':'"+String.format("%.6f",(latitude))+"','lng':'"+String.format("%.6f",(longitude)) +"','t_gps':'"+getUintAsTimestampGPS(t_gps)+"'";
        String location="'lat':'"+String.format("%.6f",(latitude))+"','lng':'"+String.format("%.6f",(longitude)) +"'";
        return location;
    }

    private static final DateFormat DATE_FORMAT_GPS = new SimpleDateFormat("yyyyMMddHHmmssSSS"); //Set the format of the .txt file name.
    private String getUintAsTimestampGPS(Long uint) {
        //return DATE_FORMAT.format(new Date(uint.longValue() * 1000L)).toString();
        //DATE_FORMAT_GPS.setTimeZone(TimeZone.getTimeZone("GMT+0")); //set timezone*******?
        //uint=uint+tmadrid.getOffset(uint); //added in the function
        return DATE_FORMAT_GPS.format(new Date(uint)).toString();
    }



}