package org.eclipse.kura.gpio;

import java.util.Map;
/**
 * The GPIOService is used to access available GPIO resources on the system.<br/>
 * {@link KuraGPIOPin}s can be accessed by name or by terminal index.<br/>
 * <br/>
 * Operations on the pins can be done using the acquired {@link KuraGPIOPin} class.
 */
public interface GPIOService {
	
	public KuraGPIOPin getPinByName(String pinName);
	
	public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger);

	public KuraGPIOPin getPinByTerminal(int terminal);
	
	public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger);
	
	public Map<Integer, String> getAvailablePins();
	
}
