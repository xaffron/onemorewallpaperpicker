package com.sunnykwong.omwpp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OMWPPAddPathActivity extends Activity {
	public ProgressBar pb;
	public LinearLayout ll;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
		setResult(Activity.RESULT_CANCELED);
		setContentView(R.layout.pathpicker);
		
		ll = (LinearLayout)findViewById(R.id.pathpicker);
		
		pb = new ProgressBar(this,null,android.R.attr.progressBarStyleLarge);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.CENTER;
		lp.topMargin=50;
		pb.setLayoutParams(lp);
		ll.addView(pb);
		ll.invalidate();
		
		AsyncTask<String, String, String> at = new AsyncTask<String, String, String>() {
			
			ArrayList<File> wpDirsList;

			FileFilter wpOnlyFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (!pathname.getName().endsWith(".png") && !pathname.getName().endsWith(".jpg")) return false;
					BitmapFactory.decodeFile(pathname.getAbsolutePath(),OMWPP.BMPQUERYOPTIONS);
        			float wpWidth = OMWPP.BMPQUERYOPTIONS.outWidth;
        			float wpHeight = OMWPP.BMPQUERYOPTIONS.outHeight;
        			if (wpWidth>=400 && wpHeight>=400) return true;
        			else return false;
				}
			};
			
			FileFilter dirsOnlyFilter = new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if (pathname.isDirectory()) return true;
					return false;
				}
			};
			
			@Override
			protected String doInBackground(String... urls) {
				
				wpDirsList = new ArrayList<File>();
				countFiles(Environment.getExternalStorageDirectory());
				return "";
			}
			
			public void countFiles(File fileList) {
				File[] wpList = fileList.listFiles();
				int filecount = 0;
				if (wpList==null) return;

				// Respect hidden and .nomedia folders
				if (fileList.getName().startsWith(".")) return;
				if (new File(fileList.getAbsolutePath()+"/.nomedia").exists()) return;

				for (File file:wpList) {
					if (wpOnlyFilter.accept(file)) {
						System.out.println(file.getAbsolutePath() + " is a wallpaper.");
						filecount++;
					}
					if (filecount>2) {
						System.out.println(fileList.getAbsolutePath() + " added to wallpaper paths.");
						wpDirsList.add(fileList);
						break;
					}
				}

				
				File[] dirList = fileList.listFiles(dirsOnlyFilter);
				if (dirList==null) return;
				for (File dir: dirList)
					countFiles(dir);
			}
			
			@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				ll.removeView(pb);
				JSONArray jsonExistingPaths;
				try {
					jsonExistingPaths = OMWPP.CONFIGJSON.getJSONArray("localpaths");
				} catch (JSONException e) {
					jsonExistingPaths = new JSONArray();
				}
				for (File dir: wpDirsList) {
					final JSONArray jsonPaths = jsonExistingPaths;
					final CheckBox cb = new CheckBox(OMWPPAddPathActivity.this);
					cb.setText(dir.getAbsolutePath());
					cb.setChecked(false);
					for (int i=0;i<jsonPaths.length();i++) {
						if (jsonPaths.optString(i,"").equals(dir.getAbsolutePath())) {
							cb.setChecked(true);
						}
					}
					cb.setTextSize(14);
					cb.setPadding(70, 10, 10, 10);
					cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						JSONArray jsonNewPaths= new JSONArray();
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (jsonPaths.length()>0) {
								// First, we remove any existing trace of the entry
								for (int i=0;i<jsonPaths.length();i++) {
									if (cb.getText().equals(jsonPaths.optString(i,""))) {
										// do nothing - we are removing the entry
									} else {
										jsonNewPaths.put(jsonPaths.optString(i,""));
									} 
								}
								if (isChecked) jsonNewPaths.put(cb.getText());
							}  else {
								if (isChecked) jsonNewPaths.put(cb.getText());
							}
							try {
								OMWPP.CONFIGJSON.put("localpaths", jsonNewPaths);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						    OMWPP.commitJSONChanges();
						}
					});
					ll.addView(cb);
				}
				ll.invalidate();
			}
		};
		at.execute("");
		
	}
}
