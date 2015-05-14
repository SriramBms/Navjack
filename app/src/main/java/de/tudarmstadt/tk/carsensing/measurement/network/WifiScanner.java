package de.tudarmstadt.tk.carsensing.measurement.network;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;


/**
 * Class to perform Wifi Scans
 * Note that there is no guarantee when they are available (usually within very short delay).
 * For the use of this class, it is best to initiate a continuous scan and then periodically call the getScanResults() function.
 * @author Julien Gedeon
 *
 */

public class WifiScanner implements JSONPrinter{
	
	private static final String TAG = "WifiScanner";
	
	private final WifiManager wifiManager;
	
	private boolean continuous = false;
	private List<ScanResult> scanResults;
	Context mContext;
	
	public WifiScanner(final Context context) {
		Log.d(TAG, "WifiScanner created");
		wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		checkAvailability();
		context.registerReceiver(wifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mContext = context;
	}
	
	/**
	 * Initiates a Wifi Scan
	 * @param continuous If true, the Wifi scan is repeated continuously each time the broadcast receiver gets a result
	 */
	public void scanWifi(final boolean continuous) {
		Log.d(TAG, "scanWifi called with continuous = " + continuous);
		this.continuous = continuous;
		wifiManager.startScan();
		
	}
	
	/**
	 * Stops continuous scans
	 */
	public void stopScan() {
		Log.d(TAG, "stopScan called");
		continuous = false;
		mContext.unregisterReceiver(wifiScanReceiver);
	}
	
	/**
	 * Returns the wifi scan results. 
	 * @return the list of all scan results
	 */
	public List<ScanResult> getScanResults() {
		return scanResults;
	}
	
	
	/**
	 * Checks if Wifi is enabled
	 */
	private void checkAvailability() {
		if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);
	}
	
	/**
	 * Receiver for the scan results
	 */
	private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(final Context c, final Intent intent) {
	        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
	        	scanResults = wifiManager.getScanResults();
	        	if (continuous) wifiManager.startScan();
	        }
	    }     
	};


	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generating JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.WIFI_INFO);
			json.put("description", description);
			JSONArray wifiNetworks = new JSONArray();
			for (ScanResult res : scanResults) {
				JSONObject nw = new JSONObject();
				nw.put("BSSID", res.BSSID);
				nw.put("SSID", res.SSID);
				nw.put("frequency", res.frequency);
				nw.put("signal", res.level);
				nw.put("capabilities", res.capabilities);
				wifiNetworks.put(nw);
			}
			json.put("networks", wifiNetworks);
	    } catch (JSONException e) {
			Log.e(TAG, "Error generating JSON");
			e.printStackTrace();
		} catch (Exception e){
			Log.e(TAG, "NULL pointer exception. wifi object is NULL");
			e.printStackTrace();
		}
		return json;
	}

}
