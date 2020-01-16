package com.upm.jgp.healthywear.ui.main.fragments.mmr;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.upm.jgp.healthywear.ui.main.activity.ScanMMRActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
	
	public static final String TAG = "MyService";
	Timer timer_up;// = new Timer();
	TimerTask timerTask_up;
	private static Boolean timerstatus=false;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format of the .txt file name.
	private long lastdata_time=0;
	private long current_time=0;

    //final Context context=getApplicationContext();
	@Override
	public void onCreate() {
		super.onCreate();
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        //***Create foreground notification to keep the service active and avoid been killed***
/*		Intent notiIntent = new Intent(this, MainActivity.class);
	    PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
		Notification notifi = new NotificationCompat.Builder(this)
			         .setContentTitle("Uploading")
			         .setContentText("Uploading MMR data...")
			         .setSmallIcon(R.drawable.ic_launcher)
			         .setContentIntent(pendIntent)
			         //.setLargeIcon(R.drawable.ic_launcher)
			         .build(); // available from API level 4 and onwards
		startForeground(2, notifi);*/

		//Error for Android 8.1 and above:  Bad notification for startForeground: java.lang.RuntimeException: invalid channel for service notification
		//https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1?answertab=active#tab-top
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			//startMyOwnForeground();
			String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
			String channelName = "My Background Service";
			NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
			chan.setLightColor(Color.BLUE);
			chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			assert manager != null;
			manager.createNotificationChannel(chan);

			Intent notiIntent = new Intent(this, ScanMMRActivity.class);
			PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
			Notification notifi = new NotificationCompat.Builder(this)
					.setContentTitle("Uploading")
					.setContentText("Uploading MMR data...")
					//.setSmallIcon(R.drawable.ic_launcher)
					.setContentIntent(pendIntent)
					//.setLargeIcon(R.drawable.ic_launcher)
					.build(); // available from API level 4 and onwards
		}else{
			//startForeground(1, new Notification());
			Intent notiIntent = new Intent(this, ScanMMRActivity.class);
			PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
			Notification notifi = new NotificationCompat.Builder(this)
					.setContentTitle("Uploading")
					.setContentText("Uploading MMR data...")
					//.setSmallIcon(R.drawable.ic_launcher)
					.setContentIntent(pendIntent)
					//.setLargeIcon(R.drawable.ic_launcher)
					.build(); // available from API level 4 and onwards
			startForeground(2, notifi);
		}

		if (timerstatus==false){
			startTimer();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println(TAG + "onDestroy() executed!!!!!");
	    //timer_up.cancel(); //some android system will kill this service which will stop data uploading
	    //timer_peb_app.cancel();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


    //Timer for uploading data
	public void startTimer() {    
	    timerstatus=true;
	    timer_up = new Timer(); //set a new Timer
		System.out.println("Uploading timer begin..");
	    initializeTimerTask_up(); //initialize the TimerTask's job
	    //schedule the timer, after the first 5000ms the TimerTask will run every 60000ms
	    timer_up.schedule(timerTask_up, 5000, 60000);
	}

	public void initializeTimerTask_up() {
		timerTask_up = new TimerTask() {
			public void run() {
				//***Get file list in the folder // stackoverflow.com/questions/8646984/how-to-list-files-in-an-android-directory
				String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "backup";
				try {
					//File file[] = f.listFiles();
					File filegz[] = findergz(folderpath);   //get all the .gz file
					if (filegz!=null && filegz.length>0) {			// If there are .gz files, upload them
						for (int j = 0; j < filegz.length; j++) {
							String datapathgz = folderpath + File.separator + filegz[j].getName();
							new RetrieveFeedTask().execute(datapathgz);
							// prepare the UI information
							String Uistr = "Uploading file:" + filegz[j].getName();
							sendBroadcastMessage(Uistr);
						}
                        lastdata_time= System.currentTimeMillis();   //get the data coming time for restarting the pebble app
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d("Files", e.getLocalizedMessage() );
				}
			}
		};
	}

	//find .gz file
	public File[] findergz(String dirName){
	  	File dir = new File(dirName);
	   	return dir.listFiles(new FilenameFilter() {
	         public boolean accept(File dir, String filename)
	              { return filename.endsWith(".gz"); }
	   	} );
	}

	//Send UI information back to main activity
	public static final String ACTION_UI_BROADCAST="UI_TEXTVIEW_INFO",
			DATA_STRING="textview_string";

	private void sendBroadcastMessage(String UIstring) {
		if (UIstring != null) {
			Intent intent = new Intent(ACTION_UI_BROADCAST);
			intent.putExtra(DATA_STRING, UIstring);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}
	}


}

//创建同步线程 http://stackoverflow.com/questions/6343166/android-os-networkonmainthreadexception
class RetrieveFeedTask extends AsyncTask<String, Void, Void> {
	protected Void doInBackground(String... datapath) {

		String despath=datapath[0];
		if (despath!= null) {
			File gzfile = new File(despath);
			if (gzfile.exists()) {
				//http://stackoverflow.com/questions/2017414/post-multipart-request-with-android-sdk
				HttpClient client = new DefaultHttpClient(); //https://stackoverflow.com/questions/45940861/android-8-cleartext-http-traffic-not-permitted
				try {
					//HttpPost httpPost = new HttpPost("http://apiinas02.etsii.upm.es/pebble/carga_pebble.py"); //check the upload result here: http://138.100.82.184/tmp/
					//HttpPost httpPost = new HttpPost("http://pebble.etsii.upm.es/pebble/carga_pebble.py");
                    //mongo --host 138.100.82.181:666 IoT -u IoT -p WISEST#2019
					//HttpPost httpPost = new HttpPost("http://apii01.etsii.upm.es/mmr/carga_mmr.py"); //http://138.100.82.181/mmr/carga_mmr.py
                    HttpPost httpPost = new HttpPost("http://138.100.82.181/mmr/carga_mmr.py");
					//HttpPost httpPost = new HttpPost("http://138.4.110.150/mmr/carga_mmr.py");
					MultipartEntityBuilder builder = MultipartEntityBuilder
							.create();
					builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
					builder.addBinaryBody("file", gzfile,
							ContentType.create("application/x-gzip"),
							gzfile.getName());
					HttpEntity entity = builder.build();
					httpPost.setEntity(entity);
					System.out.println("executing request for file" + gzfile.getName() + gzfile.length()+httpPost.getRequestLine());
					HttpResponse response = client.execute(httpPost);
					HttpEntity resEntity = response.getEntity();
					String result = EntityUtils.toString(resEntity);
					//System.out.println(result + gzfile.getName());
					System.out.println(result);
					if (result.contains("OK")) {
						gzfile.delete();
						//gzfile.renameTo(bkpfile);
						System.out.println("Successful!!!!!");
					} else {
						System.out.println("upload failed!!  gzfile:"+ gzfile.getName() + " size:"+ gzfile.length());
                        System.out.println(result);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("upload exception!!!!  gzfile:"+ gzfile.getName() + " size:" + gzfile.length());
				} finally {
					client.getConnectionManager().shutdown();
				}
			} else {
				System.out.println("NO NEW FILE FOUND!");
			}
		}
		return null;
	}

}