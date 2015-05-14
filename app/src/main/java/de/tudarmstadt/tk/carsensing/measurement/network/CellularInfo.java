package de.tudarmstadt.tk.carsensing.measurement.network;

import org.json.JSONException;
import org.json.JSONObject;

import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Class to get information about the cellular network (e.g. signal strength)
 * @author Julien Gedeon
 *
 */

public class CellularInfo implements JSONPrinter{

	public final static String TAG = "CellularInfo";
	private TelephonyManager telephonyManager;
	private int signalStrength;
	
	public CellularInfo(final Context context) {
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		
	}
	
	public int getValue() {
		return signalStrength;
	}
	
	private PhoneStateListener phoneStateListener = new PhoneStateListener() {
		public void onSignalStrengthsChanged(SignalStrength newSignalStrength) {
			Log.d(TAG, "onSignalStrengthsChanged");
			// Determine whether phone is GSM or CDMA
			if (newSignalStrength.isGsm()) {
				signalStrength = newSignalStrength.getGsmSignalStrength();
			} else {
				signalStrength = newSignalStrength.getCdmaDbm();
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
			json.put("measurementType", Measurements.CELLULAR_SIGNAL_STRENGTH);
			json.put("signalStrength", signalStrength);
			json.put("description", description);
			} catch (JSONException e) {
				Log.e(TAG, "Error generating JSON");
				e.printStackTrace();
			} 
		return json;
	}
	
	
}
