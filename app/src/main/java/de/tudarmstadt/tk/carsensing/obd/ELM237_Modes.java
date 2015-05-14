package de.tudarmstadt.tk.carsensing.obd;

/**
 * The different modes for requests and their responses.
 * These are always the first bytes sent and received when
 * talking to the dongle outside of AT commands.
 * @author Julien Gedeon
 *
 */

public class ELM237_Modes {

	/**
	 * Show current data.
	 */
	public final static String CURRENT_DATA = "01";
	
	
	/**
	 * Response for current data.
	 */
	public final static String RESPONSE_CURRENT_DATA ="41";
	
	
	/**
	 * Show vehicle information.
	 */
	public final static String VEHICLE_INFORMATION = "09";
	
	
	/**
	 * Response for vehicle information.
	 */
	public final static String RESPONSE_VEHICLE_INFORMATION = "49";
	
}
