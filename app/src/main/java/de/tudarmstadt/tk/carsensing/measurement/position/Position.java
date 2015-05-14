package de.tudarmstadt.tk.carsensing.measurement.position;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class Position implements JSONPrinter{

	private static final String TAG = "Position";
	
	public Float accuracy;
	public Double latitude;
	public Double longitude;
	public Double altitude;
	public Long time = 0L;
	public String provider;

	public Position() {
		super();
	}

	public Position(Message msg) {
		super();
		parseMessage(msg);
	}

	public Message getMessage() {
		Message msg = new Message();
		Bundle bundle = new Bundle(7);
		bundle.putInt("messageType", MeasurementService.POSITION);
		bundle.putFloat("accuracy", accuracy);
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putDouble("altitude", altitude);
		bundle.putLong("time", time);
		bundle.putString("provider", provider);
		msg.setData(bundle);
		return msg;
	}

	private void parseMessage(Message msg) {
		Bundle data = msg.getData();
		if (data.containsKey("messageType") &&
				((data.getInt("messageType") == MeasurementService.POSITION) ||
						(data.getInt("messageType") == MeasurementService.NETWORK_POSITION) ||
						(data.getInt("messageType") == MeasurementService.GPS_POSITION))) {
			accuracy = data.getFloat("accuracy");
			latitude = data.getDouble("latitude");
			longitude = data.getDouble("longitude");
			altitude = data.getDouble("altitude");
			time = data.getLong("time");
			provider = data.getString("provider");
		}
	}

	
	@Override
	public String toString() {
		return (this.latitude + "," + this.longitude + ",provider:" + this.provider);
	}

	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.LOCATION);
			json.put("description", description);
			json.put("latitude", latitude);
			json.put("longitude",longitude);
			json.put("altitude", altitude);
			json.put("accuracy", accuracy);
			json.put("provider", provider);
		} catch (JSONException e) {
		     	Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
		}
		return json;
	}
	
}
