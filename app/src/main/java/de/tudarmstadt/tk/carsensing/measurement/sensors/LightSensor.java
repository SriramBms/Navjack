package de.tudarmstadt.tk.carsensing.measurement.sensors;

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

public class LightSensor implements JSONPrinter{

	private static final String TAG = "LightSensor";

	private Handler serviceHandler;
	private float value;
	
	public LightSensor(Context context, Handler serviceHandler) {
		this.serviceHandler = serviceHandler;
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(lightSensorEventListener, lightSensor,SensorManager.SENSOR_DELAY_NORMAL);
	}
	
    SensorEventListener lightSensorEventListener = new SensorEventListener(){
	  @Override
	  public void onAccuracyChanged(Sensor sensor, int accuracy) {
	   // TODO Auto-generated method stub
	  }
	 
	  @Override
	  public void onSensorChanged(SensorEvent event) {
		  value = event.values[0];
		  /*Message msg = new Message();
		  Bundle bundle = new Bundle();
		  bundle.putInt("messageType", MeasurementService.SENSOR_LIGHT);
		  bundle.putFloat("value", event.values[0]);
		  msg.setData(bundle);	
		  serviceHandler.sendMessage(msg);*/
	  }     
    };	
	
    public float getValue() {
		Log.d(TAG, "getValue:" + value);
    	return value;
    }

	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generate JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			json.put("measurementType", Measurements.LIGHT_SENSOR);
			json.put("value", value);
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}
    
}
