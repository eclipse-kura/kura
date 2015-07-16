package org.eclipse.kura.linux.status.runnables;

import java.io.IOException;

import jdk.dio.gpio.GPIOPin;

public class BlinkStatusRunnable implements Runnable {

	private GPIOPin local_pin;
	private final int onTime;
	private final int offTime;
	
	public BlinkStatusRunnable(GPIOPin local_pin, int onTime, int offTime) {
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
			}catch(IOException ex){
				ex.printStackTrace();
				break;
			}
		}
	}

}
