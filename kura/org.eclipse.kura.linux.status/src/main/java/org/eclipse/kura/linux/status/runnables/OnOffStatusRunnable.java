package org.eclipse.kura.linux.status.runnables;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.status.CloudConnectionStatusEnum;

public class OnOffStatusRunnable implements Runnable {

	private KuraGPIOPin local_pin;
	private boolean on = false;
	
	public OnOffStatusRunnable(KuraGPIOPin local_pin, boolean on) {
		this.local_pin = local_pin;
		this.on = on;
	}

	@Override
	public void run() {
		while(true){
			try {
				local_pin.changeValue(on);				
			}catch(KuraUnavailableDeviceException ex){
				ex.printStackTrace();
				break;				
			}catch(KuraClosedDeviceException ex){
				ex.printStackTrace();
				break;				
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(CloudConnectionStatusEnum.PERIODIC_STATUS_CHECK_DELAY);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

}
