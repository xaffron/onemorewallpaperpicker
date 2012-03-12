package com.sunnykwong.omwpp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarUtils;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.security.MessageDigest;

public class OMWPP extends Application {
	
	static public final boolean DEBUG=true;
	static public JSONObject CONFIGJSON;
	static public File SDROOT,THUMBNAILROOT;
	static public long LASTCONFIGREFRESH;
	static public BitmapFactory.Options BITMAPOPTIONS, BMPQUERYOPTIONS, BMPVALIDOPTIONS;
	static public ArrayBlockingQueue<File> THUMBNAILQUEUE;
	static public ArrayBlockingQueue<URL> DOWNLOADQUEUE;
	static public ArrayBlockingQueue<File> UNZIPQUEUE;
	static public OneMoreWallpaperPickerActivity.GenerateThumbnailTask THUMBNAILTASK=null;
	static public OneMoreWallpaperPickerActivity.PopulateGalleryTask PREVIEWTASK=null;
//	static public OneMoreWallpaperPickerActivity.GenerateThumbnailTask THUMBNAILTASK=null;
//	static public OneMoreWallpaperPickerActivity.GenerateThumbnailTask THUMBNAILTASK=null;
	static public Context CONTEXT;
	static public AssetManager AM;
	static public SharedPreferences PREFS;
	static public int SCREENWIDTH, SCREENHEIGHT;
	
//	public class OMWPPThumb {
//		public Bitmap thumb;
//		public File file;
//		public OMWPPThumb() {
//			// TODO Auto-generated constructor stub 
//		}
//	} 
//
//	static public OMWPPThumb END_MARKER;
	static public final File ENDMARKER_FILE = new File("");
	static public URL ENDMARKER_URL;

	

	@Override 
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		CONTEXT = this.getApplicationContext();
		AM = this.getAssets();
		PREFS = PreferenceManager.getDefaultSharedPreferences(OMWPP.CONTEXT);
		
		BMPQUERYOPTIONS = new BitmapFactory.Options();
		BMPQUERYOPTIONS.inJustDecodeBounds=true;

		BMPVALIDOPTIONS = new BitmapFactory.Options();
		BMPVALIDOPTIONS.inSampleSize=4;
		
		// Initialize the four queues.
		THUMBNAILQUEUE = new ArrayBlockingQueue<File>(100,false);
		DOWNLOADQUEUE = new ArrayBlockingQueue<URL>(20,false);
		UNZIPQUEUE = new ArrayBlockingQueue<File>(20,false);

		SCREENWIDTH = getResources().getDisplayMetrics().widthPixels;
        SCREENHEIGHT = getResources().getDisplayMetrics().heightPixels;

		try {
			ENDMARKER_URL=new URL("http://localhost");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Check and/or create the wallpapers directory.
        SDROOT = new File(Environment.getExternalStorageDirectory().getPath()+"/ubuntuwps/");
        if (OMWPP.DEBUG) Log.i("OMWPPApp","Checking/Creating " + SDROOT.getAbsoluteFile());
        SDROOT.mkdirs();
        THUMBNAILROOT = getExternalFilesDir(null);
        if (OMWPP.DEBUG) Log.i("OMWPPApp","Checking/Creating " + THUMBNAILROOT.getAbsoluteFile());
        THUMBNAILROOT.mkdirs();
		
		//First of all, let's load up the latest configuration JSON file.
		try {
			if (isSDPresent()) {
		        if (OMWPP.DEBUG) Log.i("OMWPPApp","Loading config file from TN folder");
				CONFIGJSON = streamToJSONObject(new FileInputStream(new File(THUMBNAILROOT.getPath()+ "/omwpp_config.json")));
			} else {
		        if (OMWPP.DEBUG) Log.i("OMWPPApp","No config file in TN folder");
				throw new Exception();
			}
		} catch (Exception e) {
			try {
		        if (OMWPP.DEBUG) Log.i("OMWPPApp","Loading fallback config file");
				CONFIGJSON = streamToJSONObject(OMWPP.AM.open("omwpp_config.json"));
		        if (OMWPP.DEBUG) Log.i("OMWPPApp","Copying fallback config file to TN folder");
				copyAssetToFile("omwpp_config.json", THUMBNAILROOT.getPath()+ "/omwpp_config.json");
			} catch (Exception ee) { e.printStackTrace(); }
		}

		// Figure out when we last downloaded a new config file.
		LASTCONFIGREFRESH = OMWPP.PREFS.getLong("LASTCONFIGREFRESH", 0l);
		
	}
	
	public static boolean copyAssetToFile(String src, String tgt) {
		try {
			FileOutputStream oTGT = new FileOutputStream(tgt);
			InputStream oSRC = OMWPP.AM.open(src);
			
		    byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	static public boolean isSDPresent() {
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(OMWPP.CONTEXT, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			return false;
        }

		if (!Environment.getExternalStorageDirectory().canRead()) {
        	Toast.makeText(OMWPP.CONTEXT, "SD Card missing or corrupt.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	return false;
        }
		return true;
    }
	
    static public boolean isConnected() {
    	ConnectivityManager conMgr =  (ConnectivityManager)OMWPP.CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE);
    	boolean result = false;
    	for (NetworkInfo ni: conMgr.getAllNetworkInfo()) {
    		if (ni.isConnected()) {
    			result = true;
    			break;
    		}
    	}
    	return result;
    }
    
    static public JSONObject streamToJSONObject(InputStream is) throws IOException {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr,8192);
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null){
			sb.append(line+"\n");
		}
		isr.close();
		br.close();
		try {
			return new JSONObject(sb.toString());
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
    }

   public static List<File> unZip(final File inputZip, final File outputDir) throws IOException {
    	final List<File> unpackedFiles = new ArrayList<File>();
    	if (OMWPP.DEBUG) {
    		Log.i("OMWPPzip",String.format("Unzipping zip/apk file %s.", inputZip.getAbsoluteFile()));
    	}

        final InputStream is = new FileInputStream(inputZip); 
        final ZipInputStream zipInputStream = (ZipInputStream) new ZipInputStream(is);
        ZipEntry entry = null; 
        while ((entry = (ZipEntry)zipInputStream.getNextEntry()) != null) {
        	if (OMWPP.DEBUG) Log.i("OMWPPzip","Read entry: " + entry.getName());
        	if (entry.getName().toLowerCase().endsWith(".png") || entry.getName().toLowerCase().endsWith(".jpg")) {
            	if (OMWPP.DEBUG) Log.i("OMWPPzip","Is background, extracting.");
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(zipInputStream, outputFileStream);
                outputFileStream.close();
                unpackedFiles.add(outputFile);
        		
        	} else if (entry.getName().toLowerCase().endsWith(".gz")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(zipInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, SDROOT);
                outputFile.delete();
        	} else if (entry.getName().toLowerCase().endsWith(".tar")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(zipInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, SDROOT);
        	} else {
            	if (OMWPP.DEBUG) Log.i("OMWPPzip","Is not background, skipping.");
        	}
        }
        zipInputStream.close(); 
        return unpackedFiles;
    }

    public static List<File> unDeb(final File inputDeb, final File outputDir) throws IOException, ArchiveException {
    	final List<File> unpackedFiles = new ArrayList<File>();
    	if (OMWPP.DEBUG) {
    		Log.i("OMWPPdeb",String.format("Unzipping deb file %s.", inputDeb.getAbsoluteFile()));
    	}

        final InputStream is = new FileInputStream(inputDeb); 
        final ArArchiveInputStream debInputStream = (ArArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("ar", is);
        ArArchiveEntry entry = null; 
        while ((entry = (ArArchiveEntry)debInputStream.getNextEntry()) != null) {
        	if (OMWPP.DEBUG) Log.i("OMWPPdeb","Read entry: " + entry.getName());
        	if (entry.getName().toLowerCase().endsWith(".png") || entry.getName().toLowerCase().endsWith(".jpg")) {
            	if (OMWPP.DEBUG) Log.i("OMWPPdeb","Is background, extracting.");
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                unpackedFiles.add(outputFile);
        		
        	} else if (entry.getName().toLowerCase().endsWith(".gz")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, SDROOT);
                outputFile.delete();
        	} else if (entry.getName().toLowerCase().endsWith(".tar")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, entry.getName());
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, SDROOT);
        	} else {
            	if (OMWPP.DEBUG) Log.i("OMWPPdeb","Is not background, skipping.");
        	}
        }
        debInputStream.close(); 
        return unpackedFiles;
    }

    public static List<File> untar(final File inputtar, final File outputDir) throws IOException, ArchiveException {
    	final List<File> unpackedFiles = new ArrayList<File>();
    	if (OMWPP.DEBUG) {
    		Log.i("OMWPPtar",String.format("Unzipping tar file %s.", inputtar.getAbsoluteFile()));
    	}

        final InputStream is = new FileInputStream(inputtar); 
        final TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new TarArchiveInputStream(is);
        TarArchiveEntry entry = null; 
        while ((entry = (TarArchiveEntry)tarInputStream.getNextEntry()) != null) {
        	if (OMWPP.DEBUG) Log.i("OMWPPtar","Read entry: " + entry.getName());
        	String filename = entry.getName().substring(entry.getName().lastIndexOf("/")+1);
        	if (filename.toLowerCase().endsWith(".png") || filename.toLowerCase().endsWith(".jpg")) {
            	if (OMWPP.DEBUG) Log.i("OMWPPtar","Is background, extracting.");
                final File outputFile = new File(outputDir, filename);
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                unpackedFiles.add(outputFile);
        		
        	} else if (filename.toLowerCase().endsWith(".gz")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, filename);
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                gunzip(outputFile, SDROOT);
                outputFile.delete();
        	} else if (filename.toLowerCase().endsWith(".tar")) {
                //RECURSIVE CALL
                final File outputFile = new File(outputDir, filename);
                final OutputStream outputFileStream = new FileOutputStream(outputFile); 
                IOUtils.copy(tarInputStream, outputFileStream);
                outputFileStream.close();
                untar(outputFile, SDROOT);
                outputFile.delete();
        	} else {
            	if (OMWPP.DEBUG) Log.i("OMWPPtar","Is not background, skipping.");
        	}
        }
        tarInputStream.close(); 
        return unpackedFiles;
    }

	public static boolean gunzip(File src, File tgtPath) {
    	if (OMWPP.DEBUG) {
    		Log.i("OMWPPgz",String.format("UnGzipping gz file %s.", src.getAbsoluteFile()));
    	}
		try {
			GzipCompressorInputStream oSRC = new GzipCompressorInputStream(new FileInputStream(src));
			File tgt = new File(tgtPath.getAbsolutePath()+"/"+src.getName().substring(0,src.getName().lastIndexOf(".")));
			FileOutputStream oTGT = new FileOutputStream(tgt);
		    byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
		    
		    if (tgt.getName().endsWith(".tar")) {
		    	untar(tgt,SDROOT);
		    	tgt.delete();
		    }
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	

}
