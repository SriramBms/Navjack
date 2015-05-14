package de.tudarmstadt.tk.carsensing.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;


/**
 * Downloads the content of an URL as a String
 * @author Julien Gedeon
 *
 */

public class StringDownloader extends AsyncTask <String, Void, String>{

	private final static String TAG = "StringDownloader";
	
	@Override
	protected String doInBackground(String... url) {
		StringBuffer sb = new StringBuffer("");
		try {
		    URL remote = new URL(url[0]);
		    BufferedReader in = new BufferedReader(new InputStreamReader(remote.openStream()));
		    String str;
		    while ((str = in.readLine()) != null) {
		    	sb.append(str);
		    }
		    in.close();
		} catch (ConnectException ce) {
			Log.e(TAG, "Connect Exception");
		} catch (Exception e) {
			Log.e(TAG, "Error while downloading");
			e.printStackTrace();
		}
		return sb.toString();
	}

}
