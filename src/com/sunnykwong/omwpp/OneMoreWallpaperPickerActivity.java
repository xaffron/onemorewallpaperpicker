package com.sunnykwong.omwpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
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

	static public final boolean DEBUG=true;
	static public File SDROOT;
	
	public Gallery gallery;
	public Button btnApply, btnHelp;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	if (DEBUG) Log.i("OMWPA","OnCreate");
        getWindow().setWindowAnimations(android.R.style.Animation_Toast);
        
        SDROOT = this.getExternalFilesDir(null);
        SDROOT.mkdirs();

		setResult(Activity.RESULT_CANCELED);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			finish();
        	return;
        }
		if (!SDROOT.canRead()) {
        	Toast.makeText(this, "SD Card missing or corrupt.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			finish();
        	return;
        }

        setContentView(R.layout.main);
        
        gallery = (Gallery)findViewById(R.id.wpgallery);
        WPPickerAdapter adapter = new WPPickerAdapter();
        //Load all wallpapers in dir into picker.
//        int counter=0;
        for (File f : SDROOT.listFiles()) {
        	System.out.println(f.getName());
        	adapter.addItem(f);
        }
        
        gallery.setAdapter(adapter);
        gallery.setSelection(0);

        btnApply = (Button)findViewById(R.id.btnapply);
        btnHelp = (Button)findViewById(R.id.btnhelp);
    }        

    public boolean isConnected() {
    	ConnectivityManager conMgr =  (ConnectivityManager)(this.getSystemService(Context.CONNECTIVITY_SERVICE));
    	boolean result = false;
    	for (NetworkInfo ni: conMgr.getAllNetworkInfo()) {
    		if (ni.isConnected()) {
    			result = true;
    			break;
    		}
    	}
    	return result;
    }
        
    public class WPPickerAdapter extends BaseAdapter {

    	public ArrayList<File> mFiles = new ArrayList<File>();
    	public HashMap<String, Integer> mNames = new HashMap<String, Integer>();
    	public ArrayList<Bitmap> mPreviews = new ArrayList<Bitmap>();

        public WPPickerAdapter() {
        	
        }

        public int addItem(final File f){
        	mNames.put(f.getName(), mFiles.size());
        	mPreviews.add(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()),320,200,true));
        	mFiles.add(f);
        	return mFiles.size();
        }


        public void removeItem(int pos){
//        	if (mThemes.size()==0)return;
//        	String sTheme = mThemes.get(pos);
//        	if (sTheme.equals(DEFAULTTHEME)) {
//        		Toast.makeText(OMCThemePickerActivity.this, "The default theme is not removable!", Toast.LENGTH_LONG).show();
//        		return;
//        	}
//        	mThemes.remove(pos);
//        	//mBitmaps.remove(sTheme);
//        	mCreds.remove(sTheme);
//        	mTweaked.remove(sTheme);
//        	File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme);
//        	THEMEMAP.clear();
//        	removeDirectory(f);
//        	purgeBitmapCache();
//        	purgeTypefaceCache();
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