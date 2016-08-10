/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status.runnable;

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
				local_pin.setValue(on);				
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
