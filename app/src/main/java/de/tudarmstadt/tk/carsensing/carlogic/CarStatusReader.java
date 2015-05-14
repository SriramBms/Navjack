package de.tudarmstadt.tk.carsensing.carlogic;

/**
 * Interface used by the measurement service to determine if the car is moving/on
 * @author Julien Gedeon
 *
 */

public interface CarStatusReader {
	public boolean CarIsMoving();
}
