package com.upm.jgp.healthywear.ui.main.fragments.common;


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

import com.upm.jgp.healthywear.ui.main.activity.MainActivity;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MMR2SetupActivityFragment;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.MMRSetupActivityFragment;

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
import java.util.concurrent.TimeUnit;

/**
 * This Activity contains the service which periodically sends the POST messages to the selected DataBase (MongoDB)
 *
 * Currently it contains two RetrievedFeedTask, one for each device type: SmartBand or MMR
 *
 * @author modified by Jorge Garcia Paredes (yoryidan)
 * Modified by Raquel Prous 2022
 * @version 210
 */
public class MyService extends Service {
	
	public static final String TAG = "MyService";
	Timer timer_up;// = new Timer();
	TimerTask timerTask_up;
	private static Boolean timerstatus=false;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format of the .txt file name.
	private long lastdata_time=0;
	private long lastmmrfiles_time=0;
	private long lastmmr2files_time=0;
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
			String NOTIFICATION_CHANNEL_ID = "com.upm.jgp.healthywear.fragments.mmr";
			String channelName = "My Background Service";
			NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
			chan.setLightColor(Color.BLUE);
			chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			assert manager != null;
			manager.createNotificationChannel(chan);

			Intent notiIntent = new Intent(this, MainActivity.class);
			PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
			NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
			Notification notifi = notificationBuilder.setOngoing(true)	//Important Line, it was crashing when using (new NotificationCompat.Builder(this)
					.setContentTitle("Uploading")
					.setContentText("Uploading Wearables data...")
					//.setSmallIcon(R.drawable.ic_launcher)
					.setContentIntent(pendIntent)
					//.setLargeIcon(R.drawable.ic_launcher)
					.setCategory(Notification.CATEGORY_SERVICE)
					.build(); // available from API level 4 and onwards
			startForeground(2, notifi);

		}else{
			//startForeground(1, new Notification());
			Intent notiIntent = new Intent(this, MainActivity.class);
			PendingIntent pendIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
			Notification notifi = new NotificationCompat.Builder(this)
					.setContentTitle("Uploading")
					.setContentText("Uploading Wearables data...")
					//.setSmallIcon(R.drawable.ic_launcher)
					.setContentIntent(pendIntent)
					//.setLargeIcon(R.drawable.ic_launcher)
					.build(); // available from API level 4 and onwards
			startForeground(1, notifi);
		}

		if (timerstatus==false){
			startTimer();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//System.out.println(TAG + "onDestroy() executed!!!!!");
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
		//System.out.println("Uploading timer begin..");
	    initializeTimerTask_up(); //initialize the TimerTask's job
	    //schedule the timer, after the first 5000ms the TimerTask will run every 60000ms
	    timer_up.schedule(timerTask_up, 5000, 60000);
	}

	public void initializeTimerTask_up() {
		timerTask_up = new TimerTask() {
			public void run() {
				//***Get file list in the folder // stackoverflow.com/questions/8646984/how-to-list-files-in-an-android-directory
				String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "backup";
				String folderpath2 = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp2" +  File.separator + "backup";
				//SmartBand file list
				String folderpath_SmartBand = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp_hr" +  File.separator + "backup";
				try {
					//MMR
					if(MainActivity.isMmrConnected()) {
						//File file[] = f.listFiles();
						System.out.println("Looking for MMR Files");
						File filegz[] = findergz(folderpath);   //get all the .gz file
						if (filegz != null && filegz.length > 0) {            // If there are .gz files, upload them
							for (int j = 0; j < filegz.length; j++) {
								String datapathgz = folderpath + File.separator + filegz[j].getName();
								new RetrieveFeedTask_mmr().execute(datapathgz);
								//prepare the UI information
								String Uistr = "Uploading file:" + filegz[j].getName();
								sendBroadcastMessage(Uistr);
							}
							lastdata_time = System.currentTimeMillis();   //get the data coming time for restarting the pebble app
							lastmmrfiles_time = 0;
						}else{
							//save last time that were new files to upload
							if(lastmmrfiles_time==0) {
								lastmmrfiles_time = System.currentTimeMillis();
							}else{
								long timeSinceNoNewMMRFiles = System.currentTimeMillis() - lastmmrfiles_time;
								//If timeSinceNoNewMMRFiles bigger than 3min, then reconnect MMR
								System.out.println("No MMR Files since: " + TimeUnit.MILLISECONDS.toMinutes(timeSinceNoNewMMRFiles));
								if(TimeUnit.MILLISECONDS.toMinutes(timeSinceNoNewMMRFiles)>3){
									MMRSetupActivityFragment.reconnection();
								}
							}
						}
					}
					//MMR2
					if(MainActivity.isMmr2Connected()) {
						//File file[] = f.listFiles();
						System.out.println("Looking for MMR2 Files");
						File filegz[] = findergz(folderpath2);   //get all the .gz file
						if (filegz != null && filegz.length > 0) {            // If there are .gz files, upload them
							for (int j = 0; j < filegz.length; j++) {
								String datapathgz = folderpath2 + File.separator + filegz[j].getName();
								new RetrieveFeedTask_mmr().execute(datapathgz);
								//prepare the UI information
								String Uistr = "Uploading file:" + filegz[j].getName();
								sendBroadcastMessage(Uistr);
							}
							lastdata_time = System.currentTimeMillis();   //get the data coming time for restarting the pebble app
							lastmmr2files_time = 0;
						}else{
							//save last time that were new files to upload
							if(lastmmr2files_time==0) {
								lastmmr2files_time = System.currentTimeMillis();
							}else{
								long timeSinceNoNewMMRFiles = System.currentTimeMillis() - lastmmr2files_time;
								//If timeSinceNoNewMMRFiles bigger than 3min, then reconnect MMR
								System.out.println("No MMR2 Files since: " + TimeUnit.MILLISECONDS.toMinutes(timeSinceNoNewMMRFiles));
								if(TimeUnit.MILLISECONDS.toMinutes(timeSinceNoNewMMRFiles)>3){
									MMR2SetupActivityFragment.reconnection();
								}
							}
						}
					}
					//SmartBand
					if(MainActivity.isSmartbandConnected()) {
						System.out.println("Looking for SmartBand Files");
						File filegz[] = findergz(folderpath_SmartBand);   //get all the .gz file
						if (filegz != null && filegz.length > 0) {            // If there are .gz files, upload them
							for (int j = 0; j < filegz.length; j++) {
								String datapathgz = folderpath_SmartBand + File.separator + filegz[j].getName();
								new RetrieveFeedTask_SmartBand().execute(datapathgz);
								// prepare the UI information
								String Uistr = "Uploading file:" + filegz[j].getName();
								sendBroadcastMessage(Uistr);
							}
							lastdata_time = System.currentTimeMillis();   //get the data coming time for restarting the pebble app
						}
					}
				} catch (Exception e) {
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

/**
 * This class creates a synchronization Thread for the MMR device.
 * It is an asynchronous task to Post into the MMR MongoDB
 * */
//Create a synchronization  Thread http://stackoverflow.com/questions/6343166/android-os-networkonmainthreadexception
class RetrieveFeedTask_mmr extends AsyncTask<String, Void, Void> {
	protected Void doInBackground(String... datapath) {

		String despath=datapath[0];
		if (despath!= null) {
			File gzfile = new File(despath);
			if (gzfile.exists()) {
				//http://stackoverflow.com/questions/2017414/post-multipart-request-with-android-sdk
				HttpClient client = new DefaultHttpClient(); //https://stackoverflow.com/questions/45940861/android-8-cleartext-http-traffic-not-permitted
				try {
					//TODO modify with your DataBase address
                    HttpPost httpPost = new HttpPost("http://apii01.etsii.upm.es/mmr/carga_mmr_inf.py");
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

/**
 * This class creates a synchronization Thread for the SmartBand.
 * It is an asynchronous task to Post into the SmartBand MongoDB
 * */
//Create a syncronization Thread http://stackoverflow.com/questions/6343166/android-os-networkonmainthreadexception
class RetrieveFeedTask_SmartBand extends AsyncTask<String, Void, Void> {
	protected Void doInBackground(String... datapath) {

		String despath=datapath[0];
		if (despath!= null) {
			File gzfile = new File(despath);
			if (gzfile.exists()) {
				//http://stackoverflow.com/questions/2017414/post-multipart-request-with-android-sdk
				HttpClient client = new DefaultHttpClient(); //https://stackoverflow.com/questions/45940861/android-8-cleartext-http-traffic-not-permitted
				try {
					//TODO modify with your DataBase address
					HttpPost httpPost = new HttpPost("http://apii01.etsii.upm.es/smbnd/carga_heart_inf.py");
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