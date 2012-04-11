package com.sunnykwong.omwpp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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
		pb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		
		
		ll.addView(pb);
		
		ll.invalidate();
		
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
			
			@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				ll.invalidate();
			}
		};
		//at.execute("");
		
	}
}
