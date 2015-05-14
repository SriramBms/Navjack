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

public class BatteryTemperature implements JSONPrinter {

	private final static String TAG = "BatteryTemperature";
	private Context context;
	private int value;
	
	public BatteryTemperature(Context context) {
		this.context = context;
	}
	
	public int getBatteryTemp() {
		IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent battery = context.registerReceiver(null, batteryIntentFilter);
		value = (battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0))/10;
		return value;
	}

	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generate JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.BATTERY_TEMPERATURE);
			json.put("value", getBatteryTemp());
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}
	
}
