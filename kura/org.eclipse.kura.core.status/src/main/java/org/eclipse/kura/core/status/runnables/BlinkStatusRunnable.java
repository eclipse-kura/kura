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
package org.eclipse.kura.core.status.runnables;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;

public class BlinkStatusRunnable implements Runnable {

    private final KuraGPIOPin local_pin;
    private final int onTime;
    private final int offTime;

    public BlinkStatusRunnable(KuraGPIOPin local_pin, int onTime, int offTime) {
        this.local_pin = local_pin;
        this.onTime = onTime;
        this.offTime = offTime;
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.local_pin.setValue(true);
                Thread.sleep(this.onTime);
                this.local_pin.setValue(false);
                Thread.sleep(this.offTime);
            } catch (InterruptedException ex) {
                break;
            } catch (KuraUnavailableDeviceException ex) {
                ex.printStackTrace();
                break;
            } catch (KuraClosedDeviceException ex) {
                ex.printStackTrace();
                break;
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
        }
    }

}
