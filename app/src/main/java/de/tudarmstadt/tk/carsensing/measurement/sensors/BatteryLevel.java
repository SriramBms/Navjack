package de.tudarmstadt.tk.carsensing.measurement.sensors;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryLevel implements JSONPrinter{
	
	private final static String TAG = "BatteryLevel";
	private Context context;
	private int value;
	
	public BatteryLevel(Context context) {
		this.context = context;
	}

	public int getBatteryLevel() {
		IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent battery = context.registerReceiver(null, batteryIntentFilter);
		value = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, Integer.MIN_VALUE);
		return value;
	}

	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generating JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.BATTERY_LEVEL);
			json.put("value", getBatteryLevel());
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}
	
}
