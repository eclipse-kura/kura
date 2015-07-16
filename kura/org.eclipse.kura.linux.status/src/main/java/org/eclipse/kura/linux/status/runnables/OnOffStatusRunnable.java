package org.eclipse.kura.linux.status.runnables;

import java.io.IOException;

import jdk.dio.gpio.GPIOPin;

import org.eclipse.kura.status.CloudConnectionStatusEnum;

public class OnOffStatusRunnable implements Runnable {

	private GPIOPin local_pin;
	private boolean on = false;
	
	public OnOffStatusRunnable(GPIOPin local_pin, boolean on) {
		this.local_pin = local_pin;
		this.on = on;
	}

	@Override
	public void run() {
		while(true){
			try {
				local_pin.setValue(on);				
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
