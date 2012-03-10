package com.sunnykwong.omwpp;

import java.io.BufferedInputStream;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;

import com.sunnykwong.omwpp.OMWPP.OMWPPThumb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.WallpaperManager;

public class OneMoreWallpaperPickerActivity extends Activity {

	
	public Gallery gallery;
	public Button btnApply, btnHelp;
	public TextView tvDebConsole, tvFileConsole;
	public WPPickerAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	if (OMWPP.DEBUG) Log.i("OMWPP","Starting Activity");
        getWindow().setWindowAnimations(android.R.style.Animation_Toast);
        
		setResult(Activity.RESULT_CANCELED);
 
		boolean bNeedRefresh=false;;
		
		// If the config file is less than 6 months old, let it be
		System.out.println(System.currentTimeMillis());
		System.out.println(OMWPP.CONFIGJSON.optLong("lastupdateepoch",0l));
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
        
        gallery = (Gallery)findViewById(R.id.wpgallery);
        adapter = new WPPickerAdapter();
        
        //Load all wallpapers in dir into picker.

        tvDebConsole = (TextView)findViewById(R.id.debconsole);
        tvFileConsole = (TextView)findViewById(R.id.fileconsole);
        
        btnApply = (Button)findViewById(R.id.btnapply);
        btnHelp = (Button)findViewById(R.id.btnhelp);
        
        gallery.setAdapter(adapter);

        new PopulateGalleryTask().execute();
        new GenerateThumbnailTask().execute();

        gallery.setSelection(0);
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
			Toast.makeText(OMWPP.CONTEXT, "ForceDownload", Toast.LENGTH_SHORT).show();
			new DownloadDebsTask().execute("");
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
					if (iDebFile!=1)continue;

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
					BufferedInputStream bis = new BufferedInputStream(is);
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

					fos.close();
					Log.i("OMWPPdeb",
							"download ready in"
									+ ((System.currentTimeMillis() - startTime) / 1000)
									+ " sec");
					OMWPP.unpack(localFile, OMWPP.SDROOT);
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
		@Override
		protected String doInBackground(String... dummy) {
			OMWPPThumb thumb;
			while (true) {
    	    	if (OMWPP.DEBUG) Log.i("OMWPPAdapter","Polling queue.");
				thumb = OMWPP.THUMBNAILQUEUE.poll();
				if (thumb==null) {
					try {
						Thread.sleep(3000l);
					} catch (InterruptedException e) {
						break;
					}
				} else {
					// If we get the end_marker, we're done
					if (thumb==OMWPP.END_MARKER) {
						
						return "";
					}
					try {
						thumb.thumb.compress(CompressFormat.JPEG, 85, new FileOutputStream(thumb.file));
		    	    	if (OMWPP.DEBUG) Log.i("OMWPPAdapter","Thumbnail saved: " + thumb.file.getName()+".");
		    	    	publishProgress("Thumbnail " + thumb.file.getName()+" saved.");
	        		} catch (Exception e) {
		    	    	if (OMWPP.DEBUG) Log.w("OMWPPAdapter","Thumbnail could not be created: " + thumb.file.getName()+".");
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
            adapter.notifyDataSetChanged();
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(String result) {
			tvFileConsole.setText("All Thumbnails saved to SD cache.");
			super.onPostExecute(result);
		}
	}
	
	public class PopulateGalleryTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... dummy) {
	        for (File f : OMWPP.SDROOT.listFiles()) {
	        	final Bitmap bmp, thumbnail;
	        	// Spot check the file to see if it is a supported bitmap.
	        	// If it isn't, don't bother - move on.
	        	bmp = BitmapFactory.decodeFile(f.getAbsolutePath(),OMWPP.BMPVALIDOPTIONS);
	        	if (bmp==null) {
	    	    	if (OMWPP.DEBUG) Log.w("OMWPPAdapter",f.getName()+" is not a valid image.");
	        		continue;
	        	}
	        	// If it is, create a thumbnail if it doesn't already exist.
	        	final File tnfile = new File(OMWPP.THUMBNAILROOT+"/XFTN_" + f.getName());
	        	if (!tnfile.exists()) {
	    	    	if (OMWPP.DEBUG) Log.i("OMWPPAdapter","Generating preview for " + f.getName());
	        		thumbnail =  Bitmap.createScaledBitmap(bmp,320,200,true);
        			OMWPPThumb thumb = ((OMWPP)getApplication()).new OMWPPThumb();
        			thumb.thumb=thumbnail;
        			thumb.file=tnfile;

        			OMWPP.THUMBNAILQUEUE.offer(thumb);

        			// add it to the gallery. 
			    	if (OMWPP.DEBUG) Log.w("OMWPPAdapter",tnfile.getName()+" generated.");
			    	publishProgress(f.getName()+" added.");
		        	adapter.addItem(thumbnail, f);
	        	} else {
	        		try {
	        			thumbnail = BitmapFactory.decodeStream(new FileInputStream(tnfile));
	        			// add it to the gallery.
				    	if (OMWPP.DEBUG) Log.i("OMWPPAdapter",tnfile.getName()+" already cached.");
				    	publishProgress(f.getName()+" added from cache.");
			        	adapter.addItem(thumbnail, f);
	        		} catch (Exception e) {
				    	if (OMWPP.DEBUG) Log.w("OMWPPAdapter",tnfile.getName()+" cache corrupted! Deleting.");
				    	tnfile.delete();
	        			e.printStackTrace();
	        		}
	        	}
	        }
	        // Done looping through files; enqueue the end-marker.
	        OMWPP.THUMBNAILQUEUE.offer(OMWPP.END_MARKER);
	        
	        return "";
		}
		@Override
		protected void onProgressUpdate(String... values) {
			tvDebConsole.setText(values[0]);
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(String result) {
			tvDebConsole.setText("Previews loaded and ready.");
            adapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}
	}

	public class WPPickerAdapter extends BaseAdapter {

    	public ArrayList<File> mFiles = new ArrayList<File>();
    	public HashMap<String, Integer> mNames = new HashMap<String, Integer>();
    	public ArrayList<Bitmap> mPreviews = new ArrayList<Bitmap>();

        public WPPickerAdapter() {
        	
        }

        public int addItem(final Bitmap thumbnail, final File bitmap){
        	mPreviews.add(thumbnail);
        	mNames.put(bitmap.getName(), mFiles.size());
        	mFiles.add(bitmap);
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
            return position;
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