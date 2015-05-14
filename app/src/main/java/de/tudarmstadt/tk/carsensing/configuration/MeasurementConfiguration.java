package de.tudarmstadt.tk.carsensing.configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import de.tudarmstadt.tk.carsensing.http.StringDownloader;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;


/**
 * Class to retrieve and parse configuration files for the measurement service
 * @author Julien Gedeon
 *
 */

public class MeasurementConfiguration {

	private final static String TAG = "MeasurementConfiguration";
	
	/**
	 * Retrievels a configuration file from an URL
	 * @param url
	 * @return
	 */
	
	public static Bundle retrieveConfigurationFromURL(final String url) {
		Log.d(TAG, "Retrieve Configuration from URL");
		StringDownloader downloader = new StringDownloader();
		String[] params = {url};
		downloader.execute(params);
		try {
			String downloaded  = downloader.get();
			Log.d(TAG, "Downloaded config:" + downloaded);
			return JSONToBundle(downloaded);
		} catch (InterruptedException e) {
			Log.d(TAG, "Interrupted Exception");
			e.printStackTrace();
		} catch (ExecutionException e) {
			Log.d(TAG, "Execution Exception");
			e.printStackTrace();
		} 
		return getDefaultConfiguration();
	}
	
	/**
	 * Retrieves a configartion file locally
	 * @param filename
	 * @return
	 */
	public static Bundle retrieveConfigurationFromFile(final String filename) {
		Log.d(TAG, "Retrieve Configuration from file");
		StringBuffer sb = new StringBuffer("");
		try {
		    BufferedReader in = new BufferedReader(new FileReader(filename));
		    String str;
		    while ((str = in.readLine()) != null) {
		    	sb.append(str);
		    }
		    in.close();
			return JSONToBundle(sb.toString());
		} catch (Exception e) {
			Log.e(TAG, "Error while retrieving configuration from file");
			e.printStackTrace();
			return getDefaultConfiguration();
		}
	}
	
	/**
	 * Generates a default configuration
	 * @return
	 */
	public static Bundle getDefaultConfiguration() {
		Log.d(TAG, "Creating Default Configuration");
		Bundle bundle = new Bundle();
		bundle.putString(IntentMessages.DESCRIPTION, DateFormat.format("yyyy-MM-dd   hh:mm:ss", new Date().getTime()).toString());
		bundle.putFloat(IntentMessages.INTERVAL, 30);
		bundle.putBoolean(IntentMessages.COLLECT_LIGHT, true);
		bundle.putBoolean(IntentMessages.COLLECT_ACCEL, false);
		bundle.putBoolean(IntentMessages.COLLECT_HEADING, false);
		bundle.putBoolean(IntentMessages.COLLECT_MAGNETIC, false);
		bundle.putBoolean(IntentMessages.COLLECT_BATTERY_TEMP, true);
		bundle.putBoolean(IntentMessages.COLLECT_BATTERY_LEVEL, true);
		bundle.putBoolean(IntentMessages.COLLECT_LOCATION, true);
		bundle.putBoolean(IntentMessages.COLLECT_WIFI, true);
		bundle.putBoolean(IntentMessages.COLLECT_CELLULAR, true);
		bundle.putBoolean(IntentMessages.COLLECT_LOUDNESS, true);
		bundle.putBoolean(IntentMessages.COLLECT_OBD, false);
		bundle.putBoolean(IntentMessages.COLLECT_SPEED, false);
		bundle.putBoolean(IntentMessages.COLLECT_RPM, false);
		bundle.putBoolean(IntentMessages.COLLECT_ENGINE_LOAD, false);
		bundle.putBoolean(IntentMessages.COLLECT_MAF, false);
		bundle.putBoolean(IntentMessages.COLLECT_COOLANT_TEMP, false);
		bundle.putBoolean(IntentMessages.COLLECT_INTAKE_TEMP, false);
		return bundle;
	}
	
	/**
	 * Convert the JSON configuration to a bundle
	 * @param json
	 * @return
	 */
	private static Bundle JSONToBundle(final String json) {
		Log.d(TAG, "JSON to Bundle");
		JSONObject obj;
		Bundle bundle = new Bundle();
		try {
			obj = new JSONObject(json.replaceAll("\\s+",""));
			bundle.putString(IntentMessages.DESCRIPTION, obj.getString(IntentMessages.DESCRIPTION));
			bundle.putFloat(IntentMessages.INTERVAL, (float) obj.getDouble(IntentMessages.INTERVAL));
			bundle.putBoolean(IntentMessages.COLLECT_LIGHT, obj.getBoolean(IntentMessages.COLLECT_LIGHT));
			bundle.putBoolean(IntentMessages.COLLECT_ACCEL, obj.getBoolean(IntentMessages.COLLECT_ACCEL));
			bundle.putBoolean(IntentMessages.COLLECT_HEADING, obj.getBoolean(IntentMessages.COLLECT_HEADING));
			bundle.putBoolean(IntentMessages.COLLECT_MAGNETIC, obj.getBoolean(IntentMessages.COLLECT_MAGNETIC));
			bundle.putBoolean(IntentMessages.COLLECT_BATTERY_TEMP, obj.getBoolean(IntentMessages.COLLECT_BATTERY_TEMP));
			bundle.putBoolean(IntentMessages.COLLECT_BATTERY_LEVEL, obj.getBoolean(IntentMessages.COLLECT_BATTERY_LEVEL));
			bundle.putBoolean(IntentMessages.COLLECT_LOCATION, obj.getBoolean(IntentMessages.COLLECT_LOCATION));
			bundle.putBoolean(IntentMessages.COLLECT_WIFI, obj.getBoolean(IntentMessages.COLLECT_WIFI));
			bundle.putBoolean(IntentMessages.COLLECT_CELLULAR, obj.getBoolean(IntentMessages.COLLECT_CELLULAR));
			bundle.putBoolean(IntentMessages.COLLECT_LOUDNESS, obj.getBoolean(IntentMessages.COLLECT_LOUDNESS));
			bundle.putBoolean(IntentMessages.COLLECT_OBD, obj.getBoolean(IntentMessages.COLLECT_OBD));
			bundle.putBoolean(IntentMessages.COLLECT_SPEED, obj.getBoolean(IntentMessages.COLLECT_SPEED));
			bundle.putBoolean(IntentMessages.COLLECT_RPM, obj.getBoolean(IntentMessages.COLLECT_RPM));
			bundle.putBoolean(IntentMessages.COLLECT_ENGINE_LOAD, obj.getBoolean(IntentMessages.COLLECT_ENGINE_LOAD));
			bundle.putBoolean(IntentMessages.COLLECT_MAF, obj.getBoolean(IntentMessages.COLLECT_MAF));
			bundle.putBoolean(IntentMessages.COLLECT_COOLANT_TEMP, obj.getBoolean(IntentMessages.COLLECT_COOLANT_TEMP));
			bundle.putBoolean(IntentMessages.COLLECT_INTAKE_TEMP, obj.getBoolean(IntentMessages.COLLECT_INTAKE_TEMP));
		} catch (JSONException e) {
			Log.e(TAG, "Error creating JSON configuration from string");
			e.printStackTrace();
			return getDefaultConfiguration();
		}
		return bundle;
	}
	
}
