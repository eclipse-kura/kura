package org.eclipse.kura.raspberrypi.sensehat;

import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.osgi.service.component.ComponentContext;

public interface SenseHat {
	
	public FrameBuffer getFrameBuffer(ComponentContext ctx);
	
	public Joystick getJoystick();
	
	public HTS221 getHumiditySensor(int bus, int address, int addressSize, int frequency);
	
	public LPS25H getPressureSensor(int bus, int address, int addressSize, int frequency);
	
	public LSM9DS1 getIMUSensor(int bus, int accAddress, int magAddress, int addressSize, int frequency);

}
