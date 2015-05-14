package de.tudarmstadt.tk.carsensing.data;

import org.json.JSONObject;

/**
 * Interface for measurements and sensors that output JSON data
 * @author Julien Gedeon
 *
 */

public interface JSONPrinter {
	public JSONObject outputJSON(String deviceID, long time, String description);
}
