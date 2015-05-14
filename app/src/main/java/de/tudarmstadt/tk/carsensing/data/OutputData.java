package de.tudarmstadt.tk.carsensing.data;

import android.util.Log;

/**
 * Provides a static function to output data to the logcat
 * @author Julien Gedeon
 *
 */

public class OutputData {
	
	public static final String TAG = "CarsenseDataOutput";
	
	public static void output(final String data) {
		if (!data.equals("")) Log.i(TAG,data);
	}
	
}
