package org.eclipse.kura.gpio;

import java.util.Map;

public interface GPIOService {
	
	public KuraGPIOPin getPinByName(String pinName);
	
	public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger);

	public KuraGPIOPin getPinByTerminal(int terminal);
	
	public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger);
	
	public Map<Integer, String> getAvailablePins();
	
}
