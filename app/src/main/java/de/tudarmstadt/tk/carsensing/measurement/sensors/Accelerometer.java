package de.tudarmstadt.tk.carsensing.measurement.sensors;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * @author Julien Gedeon
 *
 */

public class Accelerometer implements JSONPrinter{

	private static final String TAG = "Accelerometer";
	
	private Handler serviceHandler;
	private double valueX;
	private double valueY;
	private double valueZ;
	
	public Accelerometer(Context context, Handler serviceHandler) {
		this.serviceHandler = serviceHandler;
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(accelerometerEventListener, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
	}
	
    SensorEventListener accelerometerEventListener = new SensorEventListener(){
	  @Override
	  public void onAccuracyChanged(Sensor sensor, int accuracy) {
	   // TODO Auto-generated method stub
	  }
	 
	  @Override
	  public void onSensorChanged(SensorEvent event) {
		  valueX = event.values[0];
		  valueY = event.values[1];
		  valueZ = event.values[2];
		  /*
		  Message msg = new Message();
		  Bundle bundle = new Bundle();
		  bundle.putInt("messageType",MeasurementService.SENSOR_ACCELEROMETER);
		  bundle.putDouble("x", event.values[0]);
		  bundle.putDouble("y", event.values[1]);
		  bundle.putDouble("z", event.values[2]);
		  msg.setData(bundle);	
		  serviceHandler.sendMessage(msg);
		  */
	  }     
    };

	public List<Double> getValue() {
		Log.d(TAG, "getValue:" + valueX + "," + valueY + "," +  valueZ);
		return Arrays.asList(valueX, valueY, valueZ);
	}

	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generating JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.ACCELEROMETER);
			json.put("x",valueX);
			json.put("y", valueY);
			json.put("z", valueZ);
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}

	
}
