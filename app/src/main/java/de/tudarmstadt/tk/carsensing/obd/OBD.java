package de.tudarmstadt.tk.carsensing.obd;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.bluetooth.BluetoothService;
import de.tudarmstadt.tk.carsensing.carlogic.CarStatusReader;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class to handle OBD communication
 * @author Julien Gedeon
 *
 */

public class OBD implements JSONPrinter, CarStatusReader {

	public final static String TAG = "OBD";
    public final static String BROADCAST_OBD = "de.tudarmstadt.tk.carsensing.broadcast.OBD";
    
	private Context context;
	private BluetoothService btService;
	private HashMap<String,Float> values = new HashMap<String, Float>();
	private boolean carMoving = false;
	private boolean dongleReady = false;
	
	
	public OBD(final String device, final Context context) {
		this.context = context;
		btService = new BluetoothService(this.context,obdHandler, device);
		connectDongle();
		initDongle();
	}
	
	public OBD(final Context context) {
		this.context = context;
		btService = new BluetoothService(this.context,obdHandler);
		connectDongle();
		initDongle();
	}
	
	public boolean dongleConnected() {
		return btService.isConnected();
	}
	
	public void disconnectDongle() {
		if (btService != null && dongleConnected()) {
			btService.disconnect();
		}
	}
	
	public void connectDongle() {
		if (btService != null && !(dongleConnected()) ) {
			btService.connect();
		}
	}
	
	public String getDeviceName(){
		return btService.getDeviceName();
	}
	
	public void initDongle() {
		Log.d(TAG, "OBD Query: initDongle called");
		try {
			dongleReady = false;
			Thread.sleep(400);
			btService.send(ELM237_Commands.RESET + "\r");
			Thread.sleep(800);
            btService.send(ELM237_Commands.SET_PROTOCOL_AUTO_FALLBACK + "3"+ "\r");
            Thread.sleep(800);
			btService.send(ELM237_Commands.ECHO_OFF + "\r");
			Thread.sleep(800);
			btService.send(ELM237_Commands.LINEFEED_OFF + "\r");
			Thread.sleep(800);
			btService.send(ELM237_Commands.SPACES_OFF + "\r");
			Thread.sleep(800);

		    dongleReady = true;
		    Thread.sleep(1500);
		} catch (Exception e) {
			Log.d(TAG, "Exception in initDongle");
			e.printStackTrace();
		}
	}
	
	public void requestRPM() {
		Log.v(TAG, "OBD Query: RPM");
		values.remove(Measurements.OBD_RPM);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.RPM);
	}
	
	public void requestSpeed() {
		Log.v(TAG, "OBD Query: Speed");
		values.remove(Measurements.OBD_SPEED);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.SPEED);
	}
	
	public void requestCLV() {
		Log.v(TAG, "OBD Query: CLV");
		values.remove(Measurements.OBD_ENGINE_LOAD);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.CALCULATED_LOAD_VALUE);
	}
	
	public void requestMAF() {
		Log.v(TAG, "OBD Query: Maf");
		values.remove(Measurements.OBD_MAF);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.MASS_AIR_FLOW);
	}
	
	public void requestCoolantTemp() {
		Log.v(TAG, "OBD Query: Cooltemp");
		values.remove(Measurements.OBD_COOLANT_TEMP);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.COOLANT_TEMPERATURE);
	}
	
	public void requestIntakeTemp() {
		Log.v(TAG, "OBD Query: Intemp");
		values.remove(Measurements.OBD_INTAKE_TEMP);
		sendMessage(ELM237_Modes.CURRENT_DATA + OBD_PIDS.INTAKE_AIR_TEMPERATURE);
	}
	
	private void sendMessage(String message) {
		if (!dongleConnected()) {
			Log.d(TAG, "OBD Query: Dongle not connected - will connect now");
			connectDongle();
			initDongle();
			carMoving = false;
			return;
		}

		try {
			if (dongleReady) btService.send(message + "\r");
		} catch (Exception e) {
			Log.d(TAG, "Exception in sendMessage");
			e.printStackTrace();
		}
		
	}
	
	
	private void parseResponse(String message) {
		Log.d(TAG, "Parse Response, Input:" + message);
		message = message.replaceAll("\\s+","");
		
		if (message.contains("STOPPED")) {
			Log.d(TAG, "Received STOPPED from Dongle");
			dongleReady = false;
			values.clear();
		}
		
		if (message.contains("UNABLETOCONNECT")) {
			Log.d(TAG, "UNABLETOCONNECT from Dongle");
			
			values.clear();
			initDongle();
		}
		
		if ((message.contains("SEARCHING"))) {
			try {
				Log.d("CarStatus", "Status: Connected (1)");
				broadcastStatusUpdate(OBD_Status.CONNECTED);
				Thread.sleep(1000);
				values.clear();
			} catch (Exception e) {}
		}
		
		if ((message.contains("NO")) || (message.contains("STOPPED")) || (message.contains("SEARCHING"))) {
			Log.d(TAG, "carMoving set to false");
			Log.d("CarStatus", "Status: Connected (2)");
			broadcastStatusUpdate(OBD_Status.CONNECTED);
			values.clear();
			carMoving = false;
		}
		
		if ((message.contains("NODATA"))) {
			Log.d("OBD Query: NODATA-CarStatus", "Status: Connected (3)");

			broadcastStatusUpdate(OBD_Status.CONNECTED);
			values.clear();
			//initDongle();   /* do not initdongle. NODATA is sent when a particular PID is unsupported or there is no data available. */
							
		}
		
		if ((message.length() > 2) && message.substring(0, 2).equals(ELM237_Modes.RESPONSE_CURRENT_DATA)) {
			Log.v(TAG, "OBD message received: No extra headers");
			broadcastStatusUpdate(OBD_Status.DATA);
			
		
			
			if (message.substring(2, 4).equals(OBD_PIDS.RPM)) {
				int result = (Integer.parseInt(message.substring(4, 8), 16))/4;
				values.put(Measurements.OBD_RPM, (float)result);
				Log.d(TAG, "RPM parse:" + result);
				broadcastOBDData(Measurements.OBD_RPM, (float) result);
				if (result > 0) {
					carMoving=true;
				} else {
					carMoving = false;
				}
			}
			
			
			
			if (message.substring(2, 4).equals(OBD_PIDS.SPEED)) {
				int result = Integer.parseInt(message.substring(4, 6), 16); 
				values.put(Measurements.OBD_SPEED, (float)result);
				Log.d(TAG,"Speed parse:" + result);
				broadcastOBDData(Measurements.OBD_SPEED, (float) result);
			}
			
			if (message.substring(2, 4).equals(OBD_PIDS.CALCULATED_LOAD_VALUE)) {
				float result = Integer.parseInt(message.substring(4, 6), 16) * (float)(100.0/255.0) ; 
				values.put(Measurements.OBD_ENGINE_LOAD, result);
				Log.d(TAG,"Load parse:" + result);
				broadcastOBDData(Measurements.OBD_ENGINE_LOAD, result);
			}
			
			if (message.substring(2, 4).equals(OBD_PIDS.MASS_AIR_FLOW)) {	
				float result = Integer.parseInt(message.substring(4, 8), 16) /(float)100.0; 
				values.put(Measurements.OBD_MAF, result);
				Log.d(TAG,"MAF parse:" + result);
				broadcastOBDData(Measurements.OBD_MAF, result);
			}
		
			if (message.substring(2, 4).equals(OBD_PIDS.COOLANT_TEMPERATURE)) {
				float result = (float) (Integer.parseInt(message.substring(4, 6), 16))-40; 
				values.put(Measurements.OBD_COOLANT_TEMP, result);
				Log.d(TAG,"Cool Temp parse:" + result);
				broadcastOBDData(Measurements.OBD_COOLANT_TEMP, result);
			}
			
			if (message.substring(2, 4).equals(OBD_PIDS.INTAKE_AIR_TEMPERATURE)) {
				float result = (float) Integer.parseInt(message.substring(4, 6), 16)-40; 
				values.put(Measurements.OBD_INTAKE_TEMP, result);
				Log.d(TAG,"Intake Temp Parse:" + result);
				broadcastOBDData(Measurements.OBD_INTAKE_TEMP, result);
			}
		}
		
		//fix for extra header info
		
		if ((message.length() > 2) && message.substring(4, 6).equals(ELM237_Modes.RESPONSE_CURRENT_DATA)) {
			Log.v(TAG, "OBD message received with extra headers");
			broadcastStatusUpdate(OBD_Status.DATA);
			
			
			if (message.substring(6, 8).equals(OBD_PIDS.RPM)) {
				int result = (Integer.parseInt(message.substring(8, 12), 16))/4;
				values.put(Measurements.OBD_RPM, (float)result);
				Log.d(TAG, "RPM parse:" + result);
				broadcastOBDData(Measurements.OBD_RPM, (float) result);
				if (result > 0) {
					carMoving=true;
				} else {
					carMoving = false;
				}
			}
			
			if (message.substring(6, 8).equals(OBD_PIDS.SPEED)) {
				int result = Integer.parseInt(message.substring(8, 10), 16); 
				values.put(Measurements.OBD_SPEED, (float)result);
				Log.d(TAG,"Speed parse:" + result);
				broadcastOBDData(Measurements.OBD_SPEED, (float) result);
			}
			
			if (message.substring(6, 8).equals(OBD_PIDS.CALCULATED_LOAD_VALUE)) {
				float result = Integer.parseInt(message.substring(8, 10), 16) * (float)(100.0/255.0) ; 
				values.put(Measurements.OBD_ENGINE_LOAD, result);
				Log.d(TAG,"Load parse:" + result);
				broadcastOBDData(Measurements.OBD_ENGINE_LOAD, result);
			}
			
			if (message.substring(6, 8).equals(OBD_PIDS.MASS_AIR_FLOW)) {	
				float result = Integer.parseInt(message.substring(8, 12), 16) /(float)100.0; 
				values.put(Measurements.OBD_MAF, result);
				Log.d(TAG,"MAF parse:" + result);
				broadcastOBDData(Measurements.OBD_MAF, result);
			}
		
			if (message.substring(6, 8).equals(OBD_PIDS.COOLANT_TEMPERATURE)) {
				float result = (float) (Integer.parseInt(message.substring(8, 10), 16))-40; 
				values.put(Measurements.OBD_COOLANT_TEMP, result);
				Log.d(TAG,"Cool Temp parse:" + result);
				broadcastOBDData(Measurements.OBD_COOLANT_TEMP, result);
			}
			
			if (message.substring(6, 8).equals(OBD_PIDS.INTAKE_AIR_TEMPERATURE)) {
				float result = (float) Integer.parseInt(message.substring(8, 10), 16)-40; 
				values.put(Measurements.OBD_INTAKE_TEMP, result);
				Log.d(TAG,"Intake Temp Parse:" + result);
				broadcastOBDData(Measurements.OBD_INTAKE_TEMP, result);
			}
		}
	}
	

	private final Handler obdHandler = new Handler() {
	 @Override
	 	public void handleMessage(Message msg) {
		 switch (msg.what) {
		 	case IntentMessages.BLUETOOTH_SEND:
		 		byte[] buffer = (byte[]) msg.obj;
	            String message = new String(buffer);
	            Log.d(TAG, "OBD message sent:" + message);
	            break;
	        case IntentMessages.BLUETOOTH_RECEIVE:
	            	Log.d(TAG, "OBD message received" +  msg.obj.toString());     
	            	parseResponse(msg.obj.toString());
	                break;
	        default:
	            	 Log.d("OBD", "Unrecognized OBD message");
	            }
	        }
	 };


	@Override
	public JSONObject outputJSON(String deviceID, long time, String description) {
		Log.d(TAG, "Generate JSON");
		JSONObject json = new JSONObject();
		try {
			json.put("deviceID", deviceID);
			json.put("timestamp", time);
			JSONArray obdValues = new JSONArray();
			for (HashMap.Entry<String, Float> val : values.entrySet()) {
				JSONObject obdVal = new JSONObject();
				obdVal.put("measurementType", val.getKey());
				obdVal.put("value", val.getValue());
				obdValues.put(obdVal);
			}
			json.put("obdValues", obdValues);
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}

	
	@Override
	public boolean CarIsMoving() {
		if (!dongleConnected()) return false;
		return carMoving;
	}

	
	public void checkDongleConnection() {
		if (!dongleConnected()) {
			broadcastStatusUpdate(OBD_Status.NO_CONNECTION);
			connectDongle();
		}
	}
	
	
	public void broadcastStatusUpdate(final String newStatus) {
		Log.v(TAG, "Sending OBD broadcast (status)"+newStatus);
	    Intent broadcast = new Intent();
	    broadcast.setAction(BROADCAST_OBD);
	    broadcast.putExtra(OBD_Status.DATA_TYPE, OBD_Status.OBD_STATUS);
	    broadcast.putExtra(OBD_Status.OBD_STATUS, newStatus);
	    context.sendBroadcast(broadcast);
	}
	
	
	public void broadcastOBDData(final String type, final float data) {
		Log.v(TAG, "Sending OBD broadcast"+type);
	    Intent broadcast = new Intent();
	    broadcast.setAction(BROADCAST_OBD);
	    broadcast.putExtra(OBD_Status.DATA_TYPE, type);
	    broadcast.putExtra(type, data);
	    context.sendBroadcast(broadcast);
	}
	
	
}
