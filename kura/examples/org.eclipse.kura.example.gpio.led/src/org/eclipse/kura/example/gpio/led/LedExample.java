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

    private static final Logger logger = LoggerFactory.getLogger(LedExample.class);
    private static final String APP_ID = "org.eclipse.kura.example.gpio.led.LedExample";

    private GPIOService myservice;
    private LedOptions options;
    private KuraGPIOPin pin;

    protected synchronized void bindGPIOService(final GPIOService gpioService) {
        this.myservice = gpioService;
    }

    protected synchronized void unbindGPIOService(final GPIOService gpioService) {
        this.myservice = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Bundle {} has started with config!", APP_ID);
        updated(properties);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Bundle {} has stopped!", APP_ID);
        close();
    }

    public void updated(Map<String, Object> properties) {

        this.options = new LedOptions(properties);

        close();

        pin = this.myservice.getPinByTerminal(this.options.isConfigPin());

        if (pin == null) {
            return;
        }

        open();

        setValue(this.options.isEnableLed());

    }

    private void open() {
        try {
            pin.open();
        } catch (KuraGPIODeviceException | KuraUnavailableDeviceException | IOException e) {
            logger.error("Open Exception ", e);
        }
    }

    private void close() {

        if (pin != null) {
            try {
                pin.close();
            } catch (IOException e) {
                logger.error("Close Exception ", e);
            }
        }
    }

    private void setValue(boolean bool) {
        try {
            pin.setValue(bool);
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException | KuraUnavailableDeviceException | IOException | KuraClosedDeviceException e) {
            logger.error("Set Value Exception ", e);
        }
    }
}
