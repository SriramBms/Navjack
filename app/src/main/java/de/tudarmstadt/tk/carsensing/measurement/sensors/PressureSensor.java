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
 * Activity to display live data from the OBD dongle
 * @author Sriram Shantharam
 *
 */
public class PressureSensor implements JSONPrinter{
	private static final String TAG = "Pressure";
	private Handler serviceHandler;
	private float value;

	public PressureSensor(Context context, Handler handler) {
			this.serviceHandler = handler;
			SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
			Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			sensorManager.registerListener(pressureSensorEventListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
			
	}
	SensorEventListener pressureSensorEventListener = new SensorEventListener(){
		  @Override
		  public void onAccuracyChanged(Sensor sensor, int accuracy) {
		   // TODO Auto-generated method stub
		  }
		 
		  @Override
		  public void onSensorChanged(SensorEvent event) {
			  value = event.values[0];
			  /*Message msg = new Message();
			  Bundle bundle = new Bundle();
			  bundle.putInt("messageType", MeasurementService.SENSOR_PRESSURE);
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

			JSONObject json = new JSONObject();
			try {
				json.put("deviceID", deviceID);
				json.put("timestamp", time);
				json.put("measurementType", Measurements.PRESSURE_SENSOR);
				json.put("value", value);
				json.put("description", description);
				} catch (JSONException e) {
					Log.e(TAG, "Error generating JSON");
					e.printStackTrace();
				} 
			return json;
		}

}
