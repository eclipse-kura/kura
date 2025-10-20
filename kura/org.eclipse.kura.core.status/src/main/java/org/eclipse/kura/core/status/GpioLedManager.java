/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import java.io.IOException;
import java.util.Objects;

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

    private final GpioIdentifier identifier;
    private final GPIOService gpioService;
    private final boolean inverted;

    public GpioLedManager(GPIOService gpioService, GpioIdentifier identifier) {
        this(gpioService, identifier, false);
    }

    public GpioLedManager(GPIOService gpioService, GpioIdentifier identifier, boolean inverted) {
        this.identifier = identifier;
        this.gpioService = gpioService;
        this.inverted = inverted;
    }

    public void writeLed(boolean enabled) throws KuraException {
        final KuraGPIOPin notificationLED;

        if (identifier instanceof GpioTerminal) {
            notificationLED = this.gpioService.getPinByTerminal(((GpioTerminal) identifier).getNumber(),
                    KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);
        } else {
            notificationLED = this.gpioService.getPinByName(((GpioName) identifier).getName(), KuraGPIODirection.OUTPUT,
                    KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);
        }

        try {
            if (!notificationLED.isOpen()) {
                notificationLED.open();
                logger.info("CloudConnectionStatus active on LED {}.", identifier);
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

    public interface GpioIdentifier {
    }

    public static class GpioTerminal implements GpioIdentifier {

        private final int number;

        public GpioTerminal(final int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public String toString() {
            return Integer.toString(number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GpioTerminal)) {
                return false;
            }
            GpioTerminal other = (GpioTerminal) obj;
            return number == other.number;
        }

    }

    public static class GpioName implements GpioIdentifier {

        private final String name;

        public GpioName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GpioName)) {
                return false;
            }
            GpioName other = (GpioName) obj;
            return Objects.equals(name, other.name);
        }

    }

}