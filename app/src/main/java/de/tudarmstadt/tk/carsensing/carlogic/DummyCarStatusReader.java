package de.tudarmstadt.tk.carsensing.carlogic;

/**
 * Dummy implementation of the CarStatusReader interface
 * Switches back between true and false every 10 times it is called
 * @author Julien Gedeon
 *
 */

public class DummyCarStatusReader implements CarStatusReader {
	private int x = 0;

	@Override
	public boolean CarIsMoving() {
		if (x==20) x = 0;
		x++;
		return(x>10);
	}

}
