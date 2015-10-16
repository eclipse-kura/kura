package org.eclipse.kura.gpio;

/**
 * This interface is used to notify status change on the Input pins 
 *
 */
public interface PinStatusListener {

	/**
	 * Invoked when the status of the attached input pin changes
	 * @param value The new value of the pin.
	 */
	public void pinStatusChange(boolean value);
}
