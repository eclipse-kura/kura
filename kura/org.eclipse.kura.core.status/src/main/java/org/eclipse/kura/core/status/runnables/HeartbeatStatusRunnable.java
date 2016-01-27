package org.eclipse.kura.core.status.runnables;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.status.CloudConnectionStatusEnum;

public class HeartbeatStatusRunnable implements Runnable {

	private KuraGPIOPin local_pin;
	
	public HeartbeatStatusRunnable(KuraGPIOPin local_pin) {
		this.local_pin = local_pin;
	}

	@Override
	public void run() {
		while(true){
			try{				
				local_pin.setValue(true);
				Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_SYSTOLE_DURATION);
				local_pin.setValue(false);
				Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_SYSTOLE_DURATION);
				local_pin.setValue(true);
				Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_DIASTOLE_DURATION);
				local_pin.setValue(false);
				Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_PAUSE_DURATION);
			}catch(InterruptedException ex){
				break;
			}catch(KuraUnavailableDeviceException ex){
				ex.printStackTrace();
				break;				
			}catch(KuraClosedDeviceException ex){
				ex.printStackTrace();
				break;				
			}catch(IOException ex){
				ex.printStackTrace();
				break;
			}
		}
	}

}
