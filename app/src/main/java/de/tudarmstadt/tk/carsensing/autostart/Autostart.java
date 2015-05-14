package de.tudarmstadt.tk.carsensing.autostart;

import de.tudarmstadt.tk.carsensing.activity.KioskActivity;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The Broadcast Receiver that waits for the BOOT_COMPLETED message
 * @author Julien Gedeon
 *
 */
public class Autostart extends BroadcastReceiver {
	
	private static final String TAG = "Autostart";
	
    public void onReceive(Context context, Intent intent) 
    {
    	Log.d(TAG, "Autostart Broadcast Receiver receive");

    	// Show Kiosk Activity
    	Log.d(TAG, "Starting the Kiosk Activity");
    	Intent activityIntent = new Intent(context, KioskActivity.class);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(activityIntent);
		
    	// Start the service
    	Log.d(TAG, "Starting the Measurement Service");
		Intent serviceIntent = new Intent(context, MeasurementService.class);
    	context.startService(serviceIntent);

    }
}