package com.sunnykwong.omwpp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DownloadService extends Service {
	// First, a few flags for the service.
	static boolean RUNNING=false;	// Am I (already) running?
	static boolean STOPNOW4x4=false;		// Should I stop now? from 4x4 widgets
	static boolean STOPNOW4x2=false;		// Should I stop now? from 4x2 widgets
	static boolean STOPNOW4x1=false;		// Should I stop now? from 4x1 widgets
	static boolean STOPNOW3x3=false;		// Should I stop now? from 3x3 widgets
	static boolean STOPNOW3x1=false;		// Should I stop now? from 3x1 widgets
	static boolean STOPNOW2x2=false;		// Should I stop now? from 2x2 widgets
	static boolean STOPNOW2x1=false;		// Should I stop now? from 2x1 widgets
	static boolean STOPNOW1x3=false;		// Should I stop now? from 1x3 widgets
    static Method mStartForeground, mStopForeground, mSetForeground;
    static Object[] mStartForegroundArgs = new Object[2];
    static Object[] mStopForegroundArgs = new Object[1];
    static Object[] mSetForegroundArgs = new Object[1];
    NotificationManager ntfMgr;
    Notification ntf;

	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {
		ntfMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Set the info for the views that show in the notification panel.
		// Icon and PI required for all notifications...
		ntf = new Notification();
		ntf.icon = R.drawable.ic_launcher;
        ntf.flags|=Notification.FLAG_ONGOING_EVENT;
        ntf.flags|=Notification.FLAG_NO_CLEAR;
        Intent it = new Intent(this, DUMMY.class);
        ntf.contentIntent = PendingIntent.getActivity(this, 0, it, 0);

		// Custom remoteview for progress bar.
		RemoteViews contentView = new RemoteViews(this.getPackageName(),R.layout.downloadnotification);

        ntf.contentView = contentView;
	}
	
	@Override
    public void onDestroy() {
		// Make sure our notification is gone.
//		stopForegroundCompat(OMC.SVCNOTIFICATIONID);
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
    	handleCommand(intent);
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		// We want intents redelivered and onStartCommand re-executed if the service is killed.
		return 1;  // Service.START_STICKY ; have to use literal because Donut is unaware of the constant
	}
	
	void handleCommand (Intent intent) {
		// If we come back from a low memory state, all sorts of screwy stuff might happen.
		// If the Intent itself is null, let's create one.
		if (intent == null) {
//			OMC.FG=true;
//			OMC.SCREENON=true;
		} else if (intent.getAction()==null) {
		// the Intent action might be blank.
		// In that case, we take an educated guess and say it's a foreground situation.
//			OMC.FG=true;
//			OMC.SCREENON=true;
		}
		

        // Throw the notification up.
        ntfMgr.notify(0, ntf);
		new DownloadDebsTask().execute("");
	}

	// Sad, but I don't know what this is used for.  Someday.
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class DownloadDebsTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			int iDebFile, iMirror, iMaxMirrors;
				// First of all, are we online?  If not, don't even try.
				if (!OMWPP.isConnected()) {
					return "abort";
				}
				// We're going to try a maximum of 5 times.
				int tries = 1;
				try {
					iMaxMirrors = OMWPP.CONFIGJSON.getJSONArray("mirrors").length();
					for (iDebFile = 0; iDebFile<OMWPP.CONFIGJSON.getJSONArray("archives").length(); iDebFile++) {
						//
						//	DEBUG ONLY
						//if (iDebFile!=3)continue;
						//	
						// Set the remote and local filenames
	
						JSONObject Debarchive = OMWPP.CONFIGJSON.getJSONArray("archives").getJSONObject(iDebFile);
						String friendlyName = Debarchive.getString("comment");
						boolean downloaded = Debarchive.getBoolean("downloaded");

						if (downloaded) {
							if (OMWPP.DEBUG) Log.i("OMWPPDLTask", friendlyName + " already downloaded.");
							continue;
						}
						
						File localFile = new File(OMWPP.SDROOT + "/" + Debarchive.getString("filename"));
						String md5sum = Debarchive.getString("md5sum");
						
						boolean success=false;
						URLConnection ucon = null;
						InputStream is = null;
						long startTime =0l;
						
						while (tries < 5) {
							if ( isCancelled()) {
								if (OMWPP.DEBUG) Log.i("OMWPPDLTask", "Task interrupted. Ending.");
								return "";
							}
							String sMirror = "http://"+OMWPP.CONFIGJSON.getJSONArray("mirrors").getString((int)(Math.random()*iMaxMirrors));
							URL url = new URL(sMirror + Debarchive.getString("url") + Debarchive.getString("filename"));
							startTime = System.currentTimeMillis();
							if (OMWPP.DEBUG) Log.i("OMWPPDLTask", "download url:" + url + " Attempt #" + tries);
							/* Open a connection to that URL. */
							try {
								/*
								 * Define InputStreams to read from the URLConnection.
								 */
								ucon = url.openConnection();
								is = ucon.getInputStream();
								
								// OK, we're properly connected.  Open a digest so we can compute MD5.
								MessageDigest md5;
								try {
									md5 = MessageDigest.getInstance("MD5");
								} catch (NoSuchAlgorithmException e) {
									e.printStackTrace();
									is.close();
									tries++;
									continue;
								}
								BufferedInputStream bis = new BufferedInputStream(new DigestInputStream(is,md5));
								
								FileOutputStream fos = new FileOutputStream(localFile);
	
								/*
								 * Read bytes to the Buffer until there is nothing more to read(-1).
								 */
								long targetByteCount = Debarchive.getLong("size");
								long bytecount=0;
							    byte[] buffer = new byte[8192];
							    int iBytesRead = 0;
							    int count = 0;
							    while ((iBytesRead = bis.read(buffer))!= -1){
									if ( isCancelled()) {
										if (OMWPP.DEBUG) Log.i("OMWPPDLTask", "Task interrupted. Ending.");
										bis.close();
										fos.close();
										return "";
									}
							    	bytecount+=iBytesRead;
							    	if (count++>50) {
							    		count=0;
										publishProgress(friendlyName + ": ("+ bytecount + "/" + targetByteCount + " bytes)", String.valueOf(bytecount*1f/targetByteCount)); 
							    	}
							    	fos.write(buffer,0,iBytesRead);
							    }
							    bis.close();
								fos.close();
							    
							    // Compute checksum.
							    byte[] digest = md5.digest();
								BigInteger bigInt = new BigInteger(1, digest);
								String thissum = bigInt.toString(16);
								
								// If the md5 sum matches, we're done; otherwise, we retry the download.
								if (thissum.equals(md5sum)) {
									publishProgress(friendlyName + " extracting...","1"); 
									if (OMWPP.DEBUG) Log.i("OMWPPdeb", "download ready in "
													+ ((System.currentTimeMillis() - startTime) / 1000)
													+ " sec");
									Debarchive.put("downloaded", true);
									OMWPP.commitJSONChanges();
									success=true;
									
									try {
										OMWPP.unDeb(localFile, OMWPP.SDROOT);
									} catch (Exception e) {
										e.printStackTrace();
									}
									break;
								} else {
									Log.i("OMWPPDLTask", "MD5 sum mismatch! Retry...");
									publishProgress(friendlyName + ": Downloaded file is corrupt!  Retrying...","0"); 
									tries++;
									continue;
								}
							} catch (java.io.FileNotFoundException e) {
								Log.i("OMWPPDLTask", "File not found on server. Retry...");
								tries++;
								continue;
							} catch (java.net.UnknownHostException e) {
								Log.i("OMWPPDLTask", "Unknown Host. Retry...");
								tries++;
								continue;
							} catch (java.io.IOException e) {
								Log.i("OMWPPDLTask", "General IO Error! Retry...");
								e.printStackTrace();
								tries++;
								continue;
							}
						}
						if (!success) {
							return "abort";
						}
	
					}
				} catch (JSONException e) {
					e.printStackTrace();
					return "";
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return "";
				}
				return "success";
		}
		@Override
		protected void onProgressUpdate(String... values) {
			ntf.contentView.setTextViewText(R.id.ntfconsole2, values[0]);
			ntf.contentView.setProgressBar(R.id.ntfProgress, 100, (int)(Double.parseDouble(values[1])*100d), false);
			ntfMgr.notify(0, ntf);
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (result.equals("abort")) {
				ntfMgr.cancel(0);
			} else if (result.equals("success")) {
				Toast.makeText(DownloadService.this, "All wallpapers downloaded.  Please restart OMWPP!", Toast.LENGTH_LONG);
			}
			ntfMgr.cancel(0);
		}
	}

    
}
