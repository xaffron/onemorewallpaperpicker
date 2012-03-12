package com.sunnykwong.omwpp;

import java.io.BufferedInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.WallpaperManager;
import android.app.WallpaperManager;
import com.mobclix.android.sdk.MobclixMMABannerXLAdView;

public class OneMoreWallpaperPickerActivity extends Activity {
	
	public Gallery gallery;
	public Button btnApply, btnHelp;
	public TextView tvDebConsole, tvFileConsole;
	public WPPickerAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        long startMillis = System.currentTimeMillis();
        OMWPP.THUMBNAILQUEUE.clear();
        OMWPP.UNZIPQUEUE.clear();
        OMWPP.DOWNLOADQUEUE.clear();
        
        
    	if (OMWPP.DEBUG) Log.i("OMWPP","Starting Activity");
        getWindow().setWindowAnimations(android.R.style.Animation_Toast);
        getWindow().setFormat(PixelFormat.RGB_565);
		setResult(Activity.RESULT_CANCELED);
 
		boolean bNeedRefresh=false;;

		// If the config file is less than 6 months old, let it be

		if (System.currentTimeMillis() - OMWPP.CONFIGJSON.optLong("lastupdateepoch",0l) < 262974l * 60000l) {
	    	if (OMWPP.DEBUG) Log.i("OMWPP","Config file less than 6 months old - no update.");
			bNeedRefresh = false;
		} else {
			// If last download was more than 6 months ago, then download
			if (System.currentTimeMillis() - OMWPP.LASTCONFIGREFRESH > 262974l * 60000l) {
		    	if (OMWPP.DEBUG) Log.i("OMWPP","Config file old, but last DL less than 6 months old - no update.");
				bNeedRefresh = true;
			} else {
		    	if (OMWPP.DEBUG) Log.i("OMWPP","Config file & last DL more than 6 months old - need update.");
				bNeedRefresh = false;
			} 
		}
		if (bNeedRefresh) {
	    	if (OMWPP.DEBUG) Log.w("OMWPP","WE SHOULD NEVER BE HERE");

			AsyncTask<String, Void, String> at = new AsyncTask<String, Void, String>() {
				@Override
				protected String doInBackground(String... urls) {

					String response = "";
					for (String url : urls) {
						DefaultHttpClient client = new DefaultHttpClient();
						HttpGet httpGet = new HttpGet(url);
						try {
							HttpResponse execute = client.execute(httpGet);
							JSONObject tempObj = OMWPP.streamToJSONObject(execute.getEntity().getContent());
							BufferedWriter out = new BufferedWriter(new FileWriter(new File(OMWPP.THUMBNAILROOT.getPath()+ "omwpp_config.json")),8192);
							out.write(tempObj.toString(5));
							out.close();
							OMWPP.PREFS.edit().putLong("lastupdateepoch", System.currentTimeMillis()).commit();
							OMWPP.LASTCONFIGREFRESH = System.currentTimeMillis();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return response;
				}
			};
			at.execute(new String[]{"http://www.yahoo.com"});
		}
	
        setContentView(R.layout.main);	
//        TextView adtitle = (TextView)findViewById(R.id.AdTitle);
//        adtitle.setEnabled(false);
//        adtitle.setVisibility(View.INVISIBLE);
//        MobclixMMABannerXLAdView adview = (MobclixMMABannerXLAdView)findViewById(R.id.advertising_banner_view);
//        adview.cancelAd();
//        adview.pause();
//        adview.setEnabled(false);
//        adview.setVisibility(View.INVISIBLE);
        gallery = (Gallery)findViewById(R.id.wpgallery);
        adapter = new WPPickerAdapter();
        
        //Load all wallpapers in dir into picker.

        tvDebConsole = (TextView)findViewById(R.id.debconsole);
        tvFileConsole = (TextView)findViewById(R.id.fileconsole);
        
        btnApply = (Button)findViewById(R.id.btnapply);
        btnHelp = (Button)findViewById(R.id.btnhelp);
        
        gallery.setAdapter(adapter);
        new PopulateGalleryTask().execute();
//        new GenerateThumbnailTask().execute();

        gallery.setSelection(0);
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
        			long arg3) {
        		try {
        			WallpaperManager.getInstance(OneMoreWallpaperPickerActivity.this).setBitmap((Bitmap)(gallery.getSelectedItem()));
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        	}
		});
        
        System.out.println("Init DONE in " + (System.currentTimeMillis()-startMillis) + "ms");
    }        

	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.statusmenu, menu);
		return true; 
	}
	
	@Override 
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		
		if (item.getItemId()==R.id.menuforcedownload) {
			AlertDialog ad = new AlertDialog.Builder(this)
								.setCancelable(true)
								.setTitle("WARNING: Large Download")
								.setMessage("OMWPP is about to contact Ubuntu servers for its background files, which total up to 50-60MB in size.  It is highly recommended to proceed only when you are on WiFi.")
								.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										//Do nothing
										}
								})
								.setPositiveButton("Ready!", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										new DownloadDebsTask().execute("");
									}
								})
								.show();
		} else if (item.getItemId()==R.id.menudownloadstatus) {
			StringBuilder sb = new StringBuilder(1000);
			try {
				JSONArray archives = OMWPP.CONFIGJSON.getJSONArray("archives"); 
				for (int i = 0; i < archives.length(); i++) {
					JSONObject archive = archives.getJSONObject(i);
					sb.append(archive.getString("comment"))
						.append("(" + archive.getLong("size") + " bytes) is ")
						.append(archive.getBoolean("downloaded")?"downloaded.":"not downloaded.")
						.append("\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			AlertDialog ad = new AlertDialog.Builder(this)
								.setCancelable(true)
								.setTitle("Download Status")
								.setMessage(sb.toString())
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
									}
								}).show();
		} else {
			Toast.makeText(OMWPP.CONTEXT, "Sorry, no help.\nThis is alpha, remember?", Toast.LENGTH_SHORT).show();
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	public class DownloadDebsTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			int iDebFile, iMirror, iMaxMirrors;
			try {
				iMaxMirrors = OMWPP.CONFIGJSON.getJSONArray("mirrors").length();
				for (iDebFile = 0; iDebFile<OMWPP.CONFIGJSON.getJSONArray("archives").length(); iDebFile++) {
					JSONObject Debarchive = OMWPP.CONFIGJSON.getJSONArray("archives").getJSONObject(iDebFile);
					String sMirror = "http://"+OMWPP.CONFIGJSON.getJSONArray("mirrors").getString((int)(Math.random()*iMaxMirrors));
					//
					//	DEBUG ONLY
								if (iDebFile!=3)continue;
					//	
					URL url = new URL(sMirror + Debarchive.getString("url"));
					File localFile = new File(OMWPP.SDROOT + "/" + url.getFile().substring(url.getFile().lastIndexOf("/")+1));

					long startTime = System.currentTimeMillis();
					Log.i("OMWPPdeb", "download begining");
					Log.i("OMWPPdeb", "download url:" + url);
					Log.i("OMWPPdeb", "downloaded file name:" + localFile.getName());
					/* Open a connection to that URL. */

					URLConnection ucon = url.openConnection();

					/*
					 * Define InputStreams to read from the URLConnection.
					 */
					InputStream is = ucon.getInputStream();
					MessageDigest md5 = MessageDigest.getInstance("MD5");
					BufferedInputStream bis = new BufferedInputStream(new DigestInputStream(is,md5));
					
					FileOutputStream fos = new FileOutputStream(localFile);

					/*
					 * Read bytes to the Buffer until there is nothing more to read(-1).
					 */
					long targetByteCount = Debarchive.getLong("size");
					long bytecount=0;
				    byte[] buffer = new byte[8192];
				    int iBytesRead = 0;
				    while ((iBytesRead = bis.read(buffer))!= -1){
				    	bytecount+=8;
						publishProgress(localFile.getName() + " downloading: " + bytecount + " out of " +targetByteCount); 
				    	fos.write(buffer,0,iBytesRead);
				    }
				    bis.close();
				    byte[] digest = md5.digest();
					BigInteger bigInt = new BigInteger(1, digest);
					String output = bigInt.toString(16);
					System.out.println("MD5: " + output);
					fos.close();
					Log.i("OMWPPdeb",
							"download ready in"
									+ ((System.currentTimeMillis() - startTime) / 1000)
									+ " sec");
					OMWPP.unDeb(localFile, OMWPP.SDROOT);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			tvDebConsole.setText(values[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
	}

	public class GenerateThumbnailTask extends AsyncTask<String, String, String> {
		int count=0;
		@Override
		protected String doInBackground(String... dummy) {
			File fullBmpFile=OMWPP.ENDMARKER_FILE;
			while (true) {
//    	    	if (OMWPP.DEBUG) Log.i("OMWPPTNThread","Polling queue.");
				try {
					fullBmpFile = OMWPP.THUMBNAILQUEUE.take();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (fullBmpFile==null) {
					try {
						Thread.sleep(100l);
					} catch (InterruptedException e) {
						break;
					}
				} else {
					// If we get the end_marker, we're done
					if (fullBmpFile==OMWPP.ENDMARKER_FILE) {
						return "";
					}
					try { 
			        	final File tnfile = new File(OMWPP.THUMBNAILROOT+"/XFTN_" + fullBmpFile.getName());
			        	if (tnfile.exists()) {
					    	if (OMWPP.DEBUG) Log.i("OMWPPTNThread",tnfile.getName()+" already cached.");
					    	Bitmap bmp = BitmapFactory.decodeFile(tnfile.getAbsolutePath());
					    	adapter.setBitmap(fullBmpFile,bmp);
					    	publishProgress(tnfile.getName()+" added from cache.");
			        	} else {
			        		if (OMWPP.DEBUG) Log.i("OMWPPTNThread","Generating preview for " + fullBmpFile.getName());
			        		Bitmap bmpFull = BitmapFactory.decodeFile(fullBmpFile.getAbsolutePath());
			        		Bitmap bmp = Bitmap.createScaledBitmap(bmpFull, OMWPP.SCREENWIDTH/2,(int)(OMWPP.SCREENWIDTH*.3125),true);
			        		bmpFull.recycle(); 
					    	adapter.setBitmap(fullBmpFile,bmp);
			        		bmp.compress(CompressFormat.JPEG, 85, new FileOutputStream(tnfile));
			        		if (OMWPP.DEBUG) Log.i("OMWPPTNThread","Thumbnail saved: " + fullBmpFile.getName()+".");
			        		publishProgress("Thumbnail " + fullBmpFile.getName() +" saved.");
			        	}
	        		} catch (Exception e) {
		    	    	if (OMWPP.DEBUG) Log.w("OMWPPTNThread","Thumbnail could not be created: " + fullBmpFile.getName() +".");
	        			e.printStackTrace();
	        			continue;
	        		}
				}
			} 
			return "";
		}
		@Override
		protected void onProgressUpdate(String... values) {
			tvFileConsole.setText(values[0]);
			count++;
			if (count>3) {
				count=0;
	            adapter.notifyDataSetChanged();
			}
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(String result) {
			tvFileConsole.setText("All Thumbnails saved to SD cache.");
	        adapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}
	}
	
	public class PopulateGalleryTask extends AsyncTask<String, String, String> {
		int count=0;
		@Override
		protected String doInBackground(String... dummy) {
			//File[] files = OMWPP.SDROOT.listFiles();
			List<File> filelist = Arrays.asList(OMWPP.SDROOT.listFiles()); 
			Collections.sort(filelist);
			File[] files = (File[])filelist.toArray();
	        for (File f : files) {
	        	final Bitmap bmp, thumbnail;
	        	// Spot check the file to see if it is a supported bitmap.
	        	// If it isn't, don't bother - move on.
	        	if (!f.getName().endsWith(".png") && !f.getName().endsWith(".jpg")) continue;
//	        	bmp = BitmapFactory.decodeFile(f.getAbsolutePath(),OMWPP.BMPVALIDOPTIONS);
//	        	if (bmp==null) {
//	    	    	if (OMWPP.DEBUG) Log.w("OMWPPAdapter",f.getName()+" is not a valid image.");
//	        		continue;
//	        	}
	        	// If it is, create a thumbnail if it doesn't already exist.
    	    	if (OMWPP.DEBUG) Log.i("OMWPPreview",f.getName()+" is added to thumbnail queue.");
	        	OMWPP.THUMBNAILQUEUE.remove(OMWPP.ENDMARKER_FILE);
	        	{
	        		try {
	        			OMWPP.THUMBNAILQUEUE.put(f);
	        		} catch (Exception e) {
	        			e.printStackTrace();
	        		}
	        	}
	        	adapter.addItem(f);
	        	publishProgress("Found " + f.getName() + ".");
	        }
	        // Done looping through files; enqueue the end-marker.
	        try {
	        	OMWPP.THUMBNAILQUEUE.put(OMWPP.ENDMARKER_FILE);
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        return "";
		}
		@Override
		protected void onProgressUpdate(String... values) {
        	if (OMWPP.THUMBNAILTASK==null) OMWPP.THUMBNAILTASK = new GenerateThumbnailTask();
			if (OMWPP.THUMBNAILTASK.getStatus()==Status.FINISHED ||
					OMWPP.THUMBNAILTASK.getStatus()==Status.PENDING) 
				OMWPP.THUMBNAILTASK.execute("","","");

			tvDebConsole.setText(values[0]);
			count++;
			if (count>3) {
				count=0;
	            adapter.notifyDataSetChanged();
			}
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(String result) {
			tvDebConsole.setText("File discovery complete.");
            adapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}
	}

	public class WPPickerAdapter extends BaseAdapter {

    	public ArrayList<File> mFiles = new ArrayList<File>();
    	public HashMap<File, Integer> mNames = new HashMap<File, Integer>();
    	public ArrayList<Bitmap> mPreviews = new ArrayList<Bitmap>();

        public WPPickerAdapter() {
        	
        }
        
        public void setBitmap (final File bitmapFile, final Bitmap bitmap) {
        	int index = mNames.get(bitmapFile);
        	mPreviews.set(index, bitmap);
        }

        public int addItem(final File bitmapFile){
        	mPreviews.add(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        	mNames.put(bitmapFile, mFiles.size());
        	mFiles.add(bitmapFile);
        	return mFiles.size();
        }


        public void removeItem(int pos){
        }        

        @Override
		public int getCount() {
            return mFiles.size();
        }

        @Override
		public Object getItem(int position) {
            return BitmapFactory.decodeFile(mFiles.get(position).getAbsolutePath());
        }

        public int getPosition(File f) {
        	return mNames.get(f.getName());
        }

        @Override
		public long getItemId(int position) {
            return position;
        }

        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
        	File f = mFiles.get(position);
        	System.out.println("GETVIEW: POS " + position + " - " + f.getName());
        	
        	LinearLayout ll = (LinearLayout)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.preview, null);
        	((TextView)ll.findViewById(R.id.wpfilename)).setText(f.getName());

        	((ImageView)ll.findViewById(R.id.wppreview)).setImageBitmap(mPreviews.get(position));
        	ll.requestLayout();

//        	BitmapFactory.Options bo = new BitmapFactory.Options();
//        	bo.inDither=true;
//        	bo.inPreferredConfig = Bitmap.Config.ARGB_4444;
//    		((ImageView)ll.findViewById(getResources().getIdentifier("ThemePreview", "id", PKGNAME)))
//    				.setImageBitmap(BitmapFactory.decodeFile(
//    				OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + mThemes.get(position) +"/000preview.jpg",bo));
//        	((TextView)ll.findViewById(getResources().getIdentifier("ThemeCredits", "id", PKGNAME))).setText(mCreds.get(mThemes.get(position)));
            return ll;
        }

        public void dispose() {
        	mFiles.clear();
        	mNames.clear();
        	System.gc();
        }
    
    }
    
        
} 