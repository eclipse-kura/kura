/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.example.gpio.led;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LedExample implements ConfigurableComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(LedExample.class);
    private static final String APP_ID = "org.eclipse.kura.example.gpio.led.LedExample";
   

    private GPIOService myservice;
    private LedOptions options;

    protected synchronized void bindGPIOService(final GPIOService gpioService) {
        this.myservice = gpioService;
    }

    protected synchronized void unbindGPIOService(final GPIOService gpioService) {
        this.myservice = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Bundle " + APP_ID + " has started with config!");
        updated(properties);
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Bundle " + APP_ID + " has stopped!");
    }

    public void updated(Map<String, Object> properties) {

       
        KuraGPIOPin pin = this.myservice.getPinByTerminal(6);

        this.options = new LedOptions(properties);

        try {
            pin.open();
        } catch (KuraGPIODeviceException | KuraUnavailableDeviceException | IOException e) {
            e.printStackTrace();
        }

        if (this.options.isEnableLed()) {

            try {
                pin.setValue(true);
                TimeUnit.SECONDS.sleep(1);
            } catch (KuraUnavailableDeviceException | IOException | KuraClosedDeviceException e) {
                s_logger.error("Exception GPIOService ", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {

            try {
                pin.setValue(false);
                TimeUnit.SECONDS.sleep(1);
                pin.close();
            } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException e) {
                s_logger.error("Exception GPIOService ", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}
