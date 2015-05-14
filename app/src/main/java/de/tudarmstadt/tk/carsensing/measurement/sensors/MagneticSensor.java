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

public class MagneticSensor implements JSONPrinter{
	
	private static final String TAG = "MagneticSensor";
	
	private Handler serviceHandler;
	private double value;
	
	public MagneticSensor(Context context, Handler serviceHandler) {
		this.serviceHandler = serviceHandler;
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(magnetometerEventListener, magnetometer,SensorManager.SENSOR_DELAY_NORMAL);
	}
	
    SensorEventListener magnetometerEventListener = new SensorEventListener(){
  	  @Override
  	  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  	   // TODO Auto-generated method stub
  	  }
  	 
  	  @Override
  	  public void onSensorChanged(SensorEvent event) {
  		  double magneticField = Math.sqrt(
  				  				 Math.pow(event.values[0], 2) + 
  				  				 Math.pow(event.values[1], 2) +
  				  			     Math.pow(event.values[2], 2) 
  		  						 );
  		  value = magneticField;
  		  /*Message msg = new Message();
  		  Bundle bundle = new Bundle();
  		  bundle.putInt("messageType", MeasurementService.SENSOR_MAGNETIC);
  		  bundle.putDouble("value", magneticField);
  		  msg.setData(bundle);	
  		  serviceHandler.sendMessage(msg);*/
  	  }     
    };	
      
    public double getValue() {
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
			json.put("measurementType", Measurements.MAGNETIC_SENSOR);
			json.put("value", value);
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}
    
}
