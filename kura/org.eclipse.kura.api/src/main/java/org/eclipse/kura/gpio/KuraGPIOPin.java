package org.eclipse.kura.gpio;

import java.io.IOException;

public interface KuraGPIOPin {

	public void changeValue(boolean active) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException;
	
	public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException;
	
	public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException;
	
	public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException;
	
	public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException;
	
	public void close() throws IOException;
	
	public KuraGPIODirection getDirection();
	
	public KuraGPIOMode getMode();
	
	public KuraGPIOTrigger getTrigger();
	
	public String getName();
	
	public int getIndex();
	
	public boolean isOpen();
}
