package de.tudarmstadt.tk.carsensing.obd;

/**
 * The PIDs that determine which data to query in Mode 1.
 * @author Julien Gedeon
 *
 */

public class OBD_PIDS {
	
	/**
	 * Engine RPM. Return two bytes. Value has to be divided by 4 to get the
	 * correct value.
	 */
	public final static String RPM = "0C";
	
	
	/**
	 * 
	 */
	public final static String SPEED = "0D";
	
	
	/**
	 * 
	 */
	public final static String CALCULATED_LOAD_VALUE = "04";
	
	
	/**
	 * 
	 */
	public final static String COOLANT_TEMPERATURE = "05";
	
	
	/**
	 * 
	 */
	public final static String INTAKE_AIR_TEMPERATURE = "0F";
	
	
	/**
	 * 
	 */
	public final static String MASS_AIR_FLOW = "10";
	
}
