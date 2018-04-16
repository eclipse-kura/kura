/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import java.io.IOException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioLedManager implements LedManager {

    private static final Logger logger = LoggerFactory.getLogger(GpioLedManager.class);

    private int ledId;
    private GPIOService gpioService;
    private boolean inverted;

    public GpioLedManager(GPIOService gpioService, int led) {
        this(gpioService, led, false);
    }

    public GpioLedManager(GPIOService gpioService, int led, boolean inverted) {
        this.ledId = led;
        this.gpioService = gpioService;
        this.inverted = inverted;
    }
    
    public void writeLed(boolean enabled) throws KuraException {
        KuraGPIOPin notificationLED = this.gpioService.getPinByTerminal(ledId, KuraGPIODirection.OUTPUT,
                KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

        try {
            if (!notificationLED.isOpen()) {
                notificationLED.open();
                logger.info("CloudConnectionStatus active on LED {}.", ledId);
            }
            notificationLED.setValue(enabled ^ inverted);

        } catch (KuraGPIODeviceException | KuraUnavailableDeviceException | IOException e) {
            logger.error("Error activating CloudConnectionStatus LED!");
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE);
        } catch (KuraClosedDeviceException e) {
            logger.error("Error accessing to the specified LED!");
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE);
        }
    }

}