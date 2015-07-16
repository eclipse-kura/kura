package org.eclipse.kura.linux.status.runnables;

import java.io.IOException;

import org.eclipse.kura.status.CloudConnectionStatusEnum;

import jdk.dio.gpio.GPIOPin;

public class HeartbeatStatusRunnable implements Runnable {

	private GPIOPin local_pin;
	
	public HeartbeatStatusRunnable(GPIOPin local_pin) {
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
			}catch(IOException ex){
				ex.printStackTrace();
				break;
			}
		}
	}

}
