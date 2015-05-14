package de.tudarmstadt.tk.carsensing.obd;

/**
 * A (non-exhaustive) list of ELM237 commands to set up and configure the dongle.
 * @author Julien Gedeon
 *
 * The different Protocols supported are:
 * 0 - Automatic
 * 1 - SAE J1850 PWM (41.6 kbaud)
 * 2 - SAE J1850 VPW (10.4 kbaud)
 * 3 - ISO 9141-2 (5 baud init, 10.4 kbaud)
 * 4 - ISO 14230-4 KWP (5 baud init, 10.4 kbaud) 
 * 5 - ISO 14230-4 KWP (fast init, 10.4 kbaud)
 * 6 - ISO 15765-4 CAN (11 bit ID, 500 kbaud)
 * 7 - ISO 15765-4 CAN (29 bit ID, 500 kbaud)
 * 8 - ISO 15765-4 CAN (11 bit ID, 250 kbaud)
 * 9 - ISO 15765-4 CAN (29 bit ID, 250 kbaud)
 * A - SAE J1939 CAN (29 bit ID, 250* kbaud)
 * B - USER1 CAN (11* bit ID, 125* kbaud)
 * C - USER2 CAN (11* bit ID, 50* kbaud)
 *
 */

public class ELM237_Commands {
	
	/**
	 * Indicates the end of a command.
	 * If sent alone, repeats the last command.
	 */
	public final static String CR = "\r";
	
	
	/**
	 * Queries the activity monitor count, which determines how active the 
	 * inputs are.
	 * The counter is reset every time activity is detected and goes up
	 * while there is no activity every 0.655 seconds up to a value of
	 * 0xFF.
	 */
	public final static String ACTIVITY_MONITOR_COUNT = "AT AMC";

	
	/**
	 * Automatically sets the receive address.
	 */
	public final static String SET_RECEIVE_ADDRESS_AUTO = "AT AR";
	
	
	/**
	 * Disables adaptive timing. Timeout has to be set using the 
	 * AT ST command.
	 */
	public final static String DISABLE_ADAPTIVE_TIMING = "AT AT0";
	
	
	/**
	 * The default timing option and the recommended setting.
	 */
	public final static String ENABLE_ADAPTIVE_TIMING = "AT AT1";
	
	
	/**
	 * More aggressive version of AT1.
	 */
	public final static String ENABLE_ADAPTIVE_TIMING_AGGRESSIVE = "AT AT2";
	
	
	/**
	 * Performs a buffer dump, returning all messages in the device's buffer.
	 * First byte returned indicates the length (in bytes). Only bytes up to this
	 * length are valid.
	 */
	public final static String BUFFER_DUMP = "AT BD";
	
	
	/**
	 * Set all options to the default.
	 */
	public final static String DEFAULT = "AT D";
	
	
	/**
	 * Shows the current protocol, returns a textual description.
	 */
	public final static String DESCRIBE_CURRENT_PROTOCOL = "AT DP";
	
	
	/**
	 * Shows the current protocol, returns the protcol number.
	 */
	public final static String DESCRIBE_CURRENT_PROTOCOL_NUMBER = "AT DPN";
	
	
	/**
	 * Turns echo off. By default, all characters are echoed back.
	 */
	public final static String ECHO_OFF = "AT E0";
	
	
	/**
	 * Turns echo on.
	 */
	public final static String ECHO_ON = "AT E1";
	
	
	/**
	 * Prints the product ID string and version info of the dongle.
	 */
	public final static String ADAPTER_INFO = "AT I";
	
	
	/**
	 * Sets the ISO Baud rate to 10400.
	 * Used with the ISO 9141-2 and ISO 14230-4 protocol.
	 */
	public final static String SET_BAUD_RATE_10400 = "AT IB10";
	
	
	/**
	 * Sets the ISO Baud rate to 4800.
	 * Used with the ISO 9141-2 and ISO 14230-4 protocol.
	 */
	public final static String SET_BAUD_RATE_4800 = "AT IB48";
	
	
	/**
	 * Sets the ISO Baud rate to 9600.
	 * Used with the ISO 9141-2 and ISO 14230-4 protocol.
	 */
	public final static String SET_BAUD_RATE_9600  ="AT IB96";
	
	
	/**
	 * Reads the signal level at pin 15, which is related to the
	 * ignition voltage. Response is either 'ON' or 'OFF'.
	 */
	public final static String IGNITION_VOLTAGE = "AT IGN";
	
	
	/**
	 * Sets the linefeed after each CR to off.
	 */
	public final static String LINEFEED_OFF = "AT L0";
	
	
	/**
	 * Sets the linefeed after each CR to on.
	 */
	public final static String LINEFEED_ON = "AT L1";
	
	
	/**
	 * Causes the dongle to go to the low power mode. Dongle
	 * will respond with 'OK'. Dongle is brought back to normal
	 * operation by sending any character.
	 */
	public final static String ENABLE_LOW_POWER_MODE = "AT LP";
	
		
	/**
	 * Retrieves the value stored with the SD command.
	 */
	public final static String READ_USER_DATA = "AT RD";
	
	
	/**
	 * Stores one byte of data in a non-volatile memory location.
	 */
	public final static String STORE_USER_DATA = "AT SD";
	
	
	/**
	 * Reads the input voltage present at pin 2.
	 */
	public final static String READ_INPUT_VOLTAGE = "AT RV";
	
	
	/**
	 * Disables spaces in the responses.
	 */
	public final static String SPACES_OFF = "AT S0";
	
	
	/**
	 * Enables spaces in the responses.
	 */
	public final static String SPACES_ON = "AT S1";
	
	
	/**
	 * Perform a slow initialization (only protcols 3 and 4).
	 */
	public final static String SLOW_INITIALIZATION = "AT SI";
	
	
	/**
	 * Sets the protocol for operation.
	 */
	public final static String SET_PROTOCOL = "AT SP";
	
	
	/**
	 * Sets the protocol for operation, but automatically searches
	 * for a valid protocol as a fallback.
	 */
	public final static String SET_PROTOCOL_AUTO_FALLBACK = "AT SPA";
	
	
	/**
	 * Sets the timeout before a 'NO DATA' is returned.
	 */
	public final static String SET_TIMEOUT = "AT ST";
	
	
	/**
	 * Performs a complete reset as if the power was cut off
	 * and then back on.
	 */
	public final static String  RESET = "AT Z";
	
}
