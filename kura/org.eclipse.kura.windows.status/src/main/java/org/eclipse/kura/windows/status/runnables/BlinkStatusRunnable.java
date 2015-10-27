package org.eclipse.kura.windows.status.runnables;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;

public class BlinkStatusRunnable implements Runnable {

	private KuraGPIOPin local_pin;
	private final int onTime;
	private final int offTime;
	
	public BlinkStatusRunnable(KuraGPIOPin local_pin, int onTime, int offTime) {
		this.local_pin = local_pin;
		this.onTime = onTime;
		this.offTime = offTime;
	}

	@Override
	public void run() {
		while(true){
			try{				
				local_pin.setValue(true);
				Thread.sleep(onTime);
				local_pin.setValue(false);
				Thread.sleep(offTime);
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
