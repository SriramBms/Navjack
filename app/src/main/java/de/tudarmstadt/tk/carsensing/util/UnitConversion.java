package de.tudarmstadt.tk.carsensing.util;

/**
 * Class to compute unit conversions (celsius/fahrenheit, kph/mph)
 * @author Julien Gedeon
 */

public class UnitConversion {

	/**
	 * Converts from Fahrenheit to Celsius
	 * @param fahrenheit
	 * @return Value in Celsius
	 */
	public static float FahrenheitToCelsius(final float fahrenheit) {
		return  ((fahrenheit-32) / 1.8f);
	}
	
	/**
	 * Converts from Celsius to Fahrenheit
	 * @param celsius
	 * @return Value in Fahrenheit
	 */
	public static float CelsiusToFahreheit(final float celsius) {
		return ((celsius * 1.8f) + 32);
	}
	
	/**
	 * Converts from kph to mph
	 * @param kph
	 * @return Value in mph
	 */
	public static float kphToMph(final float kph) {
		return (kph * 1.609f);
	}
	
	/**
	 * Converts from mph to kph
	 * @param mph
	 * @return Value in kph
	 */
	public static float mphToKph(final float mph) {
		return (mph *  0.621f);
	}
	
}
